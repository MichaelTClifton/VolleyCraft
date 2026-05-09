package com.volleycraft.network;

import com.volleycraft.entity.VolleyballEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: player has hit the volleyball.
 * Includes client-reported ball position for lag compensation:
 * server accepts the hit if reported position is within 5 blocks of
 * its own position (covers ~250ms at max ball speed).
 */
public class PlayerHitPacket {
    private final int ballEntityId;
    private final float power;
    private final double clientBallX, clientBallY, clientBallZ;

    public PlayerHitPacket(int ballEntityId, float power,
                           double clientBallX, double clientBallY, double clientBallZ) {
        this.ballEntityId = ballEntityId;
        this.power = power;
        this.clientBallX = clientBallX;
        this.clientBallY = clientBallY;
        this.clientBallZ = clientBallZ;
    }

    public static void encode(PlayerHitPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.ballEntityId);
        buf.writeFloat(pkt.power);
        buf.writeDouble(pkt.clientBallX);
        buf.writeDouble(pkt.clientBallY);
        buf.writeDouble(pkt.clientBallZ);
    }

    public static PlayerHitPacket decode(FriendlyByteBuf buf) {
        return new PlayerHitPacket(
            buf.readInt(), buf.readFloat(),
            buf.readDouble(), buf.readDouble(), buf.readDouble()
        );
    }

    public static void handle(PlayerHitPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.serverLevel().getEntity(pkt.ballEntityId);
            if (!(entity instanceof VolleyballEntity ball)) return;

            // Lag compensation: allow up to 5 blocks of position disagreement
            double dx = pkt.clientBallX - ball.getX();
            double dy = pkt.clientBallY - ball.getY();
            double dz = pkt.clientBallZ - ball.getZ();
            if (dx*dx + dy*dy + dz*dz > 25.0) return;

            // Player must be within 4 blocks of where they say the ball was
            if (player.distanceToSqr(pkt.clientBallX, pkt.clientBallY, pkt.clientBallZ) > 16.0) return;

            ball.applyPlayerHit(player, Math.min(pkt.power, 1.5f));
        });
        ctx.get().setPacketHandled(true);
    }
}
