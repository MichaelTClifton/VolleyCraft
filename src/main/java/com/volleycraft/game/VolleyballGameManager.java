package com.volleycraft.game;

import com.volleycraft.entity.VolleyballEntity;
import com.volleycraft.network.GameStatePacket;
import com.volleycraft.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class VolleyballGameManager {
    private static VolleyballGameManager INSTANCE;

    private final Map<UUID, VolleyballGame> games = new HashMap<>();
    private final Map<BlockPos, UUID> poleToCourtId = new HashMap<>();

    private VolleyballGameManager() {}

    public static VolleyballGameManager getInstance() {
        if (INSTANCE == null) INSTANCE = new VolleyballGameManager();
        return INSTANCE;
    }

    public UUID createCourt(BlockPos pole1, BlockPos pole2) {
        UUID courtId = UUID.randomUUID();
        games.put(courtId, new VolleyballGame(courtId, pole1, pole2));
        poleToCourtId.put(pole1, courtId);
        poleToCourtId.put(pole2, courtId);
        return courtId;
    }

    public void removeCourt(BlockPos polePos) {
        UUID courtId = poleToCourtId.remove(polePos);
        if (courtId != null) {
            games.remove(courtId);
            poleToCourtId.values().removeIf(id -> id.equals(courtId));
        }
    }

    public UUID getCourtIdForPole(BlockPos polePos) {
        return poleToCourtId.get(polePos);
    }

    public VolleyballGame getGame(UUID courtId) {
        return games.get(courtId);
    }

    public void addPlayerToCourt(UUID courtId, ServerPlayer player) {
        VolleyballGame game = games.get(courtId);
        if (game != null) {
            game.addPlayer(player);
            syncGameState(player.serverLevel(), courtId);
        }
    }

    public int getTeamForPlayer(UUID courtId, UUID playerId) {
        if (courtId == null) return -1;
        VolleyballGame game = games.get(courtId);
        return game == null ? -1 : game.getTeamForPlayer(playerId);
    }

    public void onBallHitGround(UUID courtId, int lastHitTeam, Vec3 pos) {
        VolleyballGame game = games.get(courtId);
        if (game != null) game.onBallHitGround(lastHitTeam, pos);
    }

    public void onFault(UUID courtId, int faultingTeam) {
        VolleyballGame game = games.get(courtId);
        if (game != null) game.onFault(faultingTeam);
    }

    public void spawnBall(ServerLevel level, UUID courtId) {
        VolleyballGame game = games.get(courtId);
        if (game == null) return;
        Vec3 spawn = game.getNetCenter().add(0, 3, 0);
        VolleyballEntity ball = new VolleyballEntity(level, spawn.x, spawn.y, spawn.z);
        ball.setCourtId(courtId);
        ball.setInPlay(true);
        level.addFreshEntity(ball);
        game.setPhase(GamePhase.IN_RALLY);
    }

    public void syncGameState(ServerLevel level, UUID courtId) {
        VolleyballGame game = games.get(courtId);
        if (game == null) return;
        GameStatePacket packet = new GameStatePacket(
            courtId,
            game.getTeam(0).getScore(),
            game.getTeam(1).getScore(),
            game.getPhase().ordinal(),
            game.getServingTeam()
        );
        for (int t = 0; t < 2; t++) {
            for (UUID pid : game.getTeam(t).getPlayers()) {
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(pid);
                if (sp != null) {
                    PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), packet);
                }
            }
        }
    }

    public void broadcastToGame(UUID courtId, ServerLevel level, String message) {
        VolleyballGame game = games.get(courtId);
        if (game == null) return;
        Component msg = Component.literal(message);
        for (int t = 0; t < 2; t++) {
            for (UUID pid : game.getTeam(t).getPlayers()) {
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(pid);
                if (sp != null) sp.sendSystemMessage(msg);
            }
        }
    }

    public void reset() {
        games.clear();
        poleToCourtId.clear();
    }
}
