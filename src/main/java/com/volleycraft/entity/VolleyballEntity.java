package com.volleycraft.entity;

import com.volleycraft.game.VolleyballGameManager;
import com.volleycraft.network.BallSyncPacket;
import com.volleycraft.network.PacketHandler;
import com.volleycraft.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class VolleyballEntity extends Entity {

    // Synced data for client-side interpolation
    private static final EntityDataAccessor<Float> DATA_VEL_X =
        SynchedEntityData.defineId(VolleyballEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_VEL_Y =
        SynchedEntityData.defineId(VolleyballEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_VEL_Z =
        SynchedEntityData.defineId(VolleyballEntity.class, EntityDataSerializers.FLOAT);

    // Physics constants
    private static final double GRAVITY = 0.035;
    private static final double AIR_DRAG = 0.99;
    private static final double BOUNCE_FACTOR = 0.35;
    private static final double HIT_RANGE = 1.8;
    private static final double MIN_SPEED_TO_BOUNCE = 0.05;

    // Game state
    @Nullable
    private UUID courtId = null;
    private int lastHitTeam = -1;
    private int touchesThisSide = 0;
    private boolean inPlay = false;
    private int syncTimer = 0;

    // Net-crossing tracking
    private boolean netInfoInitialized = false;
    private double netMidpoint = Double.NaN;
    private boolean netRunsAlongZ = false;
    private double lastBallNetCoord = Double.NaN;

    // Client interpolation state
    private double lerpX, lerpY, lerpZ;
    private double lerpVX, lerpVY, lerpVZ;
    private int lerpSteps = 0;

    public VolleyballEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public VolleyballEntity(Level level, double x, double y, double z) {
        this(ModEntityTypes.VOLLEYBALL.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_VEL_X, 0.0f);
        this.entityData.define(DATA_VEL_Y, 0.0f);
        this.entityData.define(DATA_VEL_Z, 0.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            serverTick();
        } else {
            clientTick();
        }
    }

    private void serverTick() {
        Vec3 vel = getDeltaMovement();
        double vx = vel.x * AIR_DRAG;
        double vy = (vel.y - GRAVITY) * AIR_DRAG;
        double vz = vel.z * AIR_DRAG;

        double nx = getX() + vx;
        double ny = getY() + vy;
        double nz = getZ() + vz;

        // Block collision — check midpoint along trajectory to catch fast-moving balls
        boolean bounced = false;
        double[] checkYs = { getY() + vy * 0.5, ny };
        double[] checkXs = { getX() + vx * 0.5, nx };
        double[] checkZs = { getZ() + vz * 0.5, nz };
        for (int ci = 0; ci < 2 && !bounced; ci++) {
            BlockPos below = BlockPos.containing(checkXs[ci], checkYs[ci] - 0.21, checkZs[ci]);
            BlockState blockBelow = level().getBlockState(below);
            if (!blockBelow.isAir() && checkYs[ci] < below.getY() + 1.0) {
                ny = below.getY() + 1.0;
                bounced = true;
                if (Math.abs(vy) > MIN_SPEED_TO_BOUNCE) {
                    vy = -vy * BOUNCE_FACTOR;
                    handleBallHitGround(nx, ny, nz);
                } else {
                    vy = 0;
                    vx *= 0.7;
                    vz *= 0.7;
                }
            }
        }

        // Net-crossing: reset touch counter when ball passes from one side to the other
        if (inPlay && courtId != null) {
            if (!netInfoInitialized) initNetInfo();
            if (netInfoInitialized && !Double.isNaN(netMidpoint) && !Double.isNaN(lastBallNetCoord)) {
                double newCoord = netRunsAlongZ ? nx : nz;
                if ((lastBallNetCoord - netMidpoint) * (newCoord - netMidpoint) < 0) {
                    // Ball just crossed the net midpoint — reset touches for new side
                    touchesThisSide = 0;
                }
                lastBallNetCoord = newCoord;
            }
        }

        setDeltaMovement(vx, vy, vz);
        setPos(nx, ny, nz);

        // Sync velocity to clients
        this.entityData.set(DATA_VEL_X, (float) vx);
        this.entityData.set(DATA_VEL_Y, (float) vy);
        this.entityData.set(DATA_VEL_Z, (float) vz);

        // Periodic full sync packet (every 3 ticks)
        if (++syncTimer >= 3) {
            syncTimer = 0;
            if (level() instanceof ServerLevel sl) {
                PacketHandler.CHANNEL.send(
                    PacketDistributor.NEAR.with(() ->
                        new PacketDistributor.TargetPoint(getX(), getY(), getZ(), 64, level().dimension())),
                    new BallSyncPacket(getId(), getX(), getY(), getZ(), vx, vy, vz)
                );
            }
        }

        // Player proximity hit detection (for in-game serves/spikes only handled via PlayerHitPacket)
        checkAutoHitPlayers();

        // Despawn if below world or idle too long
        if (getY() < level().getMinBuildHeight() - 10) {
            discard();
        }
    }

    private void clientTick() {
        if (lerpSteps > 0) {
            double tx = getX() + (lerpX - getX()) / lerpSteps;
            double ty = getY() + (lerpY - getY()) / lerpSteps;
            double tz = getZ() + (lerpZ - getZ()) / lerpSteps;
            setPos(tx, ty, tz);
            setDeltaMovement(
                getDeltaMovement().x + (lerpVX - getDeltaMovement().x) / lerpSteps,
                getDeltaMovement().y + (lerpVY - getDeltaMovement().y) / lerpSteps,
                getDeltaMovement().z + (lerpVZ - getDeltaMovement().z) / lerpSteps
            );
            lerpSteps--;
        } else {
            // Apply same physics locally between server syncs
            Vec3 vel = getDeltaMovement();
            double vx = vel.x * AIR_DRAG;
            double vy = (vel.y - GRAVITY) * AIR_DRAG;
            double vz = vel.z * AIR_DRAG;
            setDeltaMovement(vx, vy, vz);
            setPos(getX() + vx, getY() + vy, getZ() + vz);
        }
    }

    /** Called from BallSyncPacket on the client to smoothly reconcile position. */
    public void lerpTo(double x, double y, double z, double vx, double vy, double vz) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpVX = vx;
        this.lerpVY = vy;
        this.lerpVZ = vz;
        this.lerpSteps = 6; // spread correction over 6 ticks (~300ms)
    }

    private void initNetInfo() {
        if (courtId == null) return;
        com.volleycraft.game.VolleyballGame game =
            com.volleycraft.game.VolleyballGameManager.getInstance().getGame(courtId);
        if (game == null) return;
        net.minecraft.world.phys.Vec3 center = game.getNetCenter();
        net.minecraft.core.BlockPos p1 = game.getPole1Pos();
        net.minecraft.core.BlockPos p2 = game.getPole2Pos();
        netRunsAlongZ = p1.getX() != p2.getX();
        netMidpoint = netRunsAlongZ ? center.x : center.z;
        netInfoInitialized = true;
        lastBallNetCoord = netRunsAlongZ ? getX() : getZ();
    }

    /** Called server-side when the volleyball hits the ground. */
    private void handleBallHitGround(double x, double y, double z) {
        if (courtId == null || !inPlay) return;
        VolleyballGameManager.getInstance().onBallHitGround(courtId, lastHitTeam, new Vec3(x, y, z));
    }

    /** Auto-hit players that are very close (casual mode: ball bounces off players automatically). */
    private void checkAutoHitPlayers() {
        if (!inPlay) return;
        AABB hitBox = getBoundingBox().inflate(0.3);
        List<Player> nearby = level().getEntitiesOfClass(Player.class, hitBox);
        for (Player player : nearby) {
            applyPlayerHit(player, 0.6f);
            break;
        }
    }

    /** Apply a hit from a player. Called from PlayerHitPacket handler on server. */
    public void applyPlayerHit(Player player, float power) {
        Vec3 lookDir = player.getLookAngle();
        double upBoost = 0.3;
        Vec3 hitVel = new Vec3(
            lookDir.x * power,
            Math.max(lookDir.y * power + upBoost, 0.3),
            lookDir.z * power
        );
        setDeltaMovement(hitVel);
        lastHitTeam = VolleyballGameManager.getInstance().getTeamForPlayer(courtId, player.getUUID());
        touchesThisSide++;

        if (touchesThisSide > 3) {
            // Fault: too many touches
            VolleyballGameManager.getInstance().onFault(courtId, lastHitTeam);
        }

        level().playSound(null, getX(), getY(), getZ(),
            net.minecraft.sounds.SoundEvents.SLIME_SQUISH, net.minecraft.sounds.SoundSource.NEUTRAL,
            1.0f, 1.2f + level().getRandom().nextFloat() * 0.4f);
    }

    public void setCourtId(UUID id) {
        this.courtId = id;
    }

    public void setInPlay(boolean inPlay) {
        this.inPlay = inPlay;
    }

    public void resetTouches() {
        this.touchesThisSide = 0;
    }

    public void setLastHitTeam(int team) {
        this.lastHitTeam = team;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("CourtId")) courtId = tag.getUUID("CourtId");
        lastHitTeam = tag.getInt("LastHitTeam");
        inPlay = tag.getBoolean("InPlay");
        netMidpoint = tag.contains("NetMidpoint") ? tag.getDouble("NetMidpoint") : Double.NaN;
        netRunsAlongZ = tag.getBoolean("NetRunsAlongZ");
        netInfoInitialized = tag.getBoolean("NetInfoInit");
        lastBallNetCoord = netMidpoint; // will be corrected on first tick
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (courtId != null) tag.putUUID("CourtId", courtId);
        tag.putInt("LastHitTeam", lastHitTeam);
        tag.putBoolean("InPlay", inPlay);
        tag.putDouble("NetMidpoint", netMidpoint);
        tag.putBoolean("NetRunsAlongZ", netRunsAlongZ);
        tag.putBoolean("NetInfoInit", netInfoInitialized);
    }
}
