package com.volleycraft.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class VolleyballGame {
    private static final int POINTS_TO_WIN = 25;
    private static final int MIN_LEAD = 2;

    private final UUID courtId;
    private final BlockPos pole1Pos;
    private final BlockPos pole2Pos;
    private final Vec3 netCenter;
    private final double netX;
    private final double netZ;
    private final boolean netAlongZ;

    private final Team team0 = new Team(0);
    private final Team team1 = new Team(1);

    private GamePhase phase = GamePhase.WAITING;
    private int servingTeam = 0;

    public VolleyballGame(UUID courtId, BlockPos pole1, BlockPos pole2) {
        this.courtId = courtId;
        this.pole1Pos = pole1;
        this.pole2Pos = pole2;
        this.netCenter = new Vec3(
            (pole1.getX() + pole2.getX()) / 2.0,
            Math.max(pole1.getY(), pole2.getY()) + 2.0,
            (pole1.getZ() + pole2.getZ()) / 2.0
        );
        this.netAlongZ = pole1.getX() != pole2.getX();
        this.netX = netCenter.x;
        this.netZ = netCenter.z;
    }

    public void addPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        if (team0.getPlayers().size() <= team1.getPlayers().size()) {
            team0.addPlayer(id);
            player.sendSystemMessage(Component.literal("[VolleyCraft] You joined Team 1!"));
        } else {
            team1.addPlayer(id);
            player.sendSystemMessage(Component.literal("[VolleyCraft] You joined Team 2!"));
        }
        tryStartGame();
    }

    public void removePlayer(UUID playerId) {
        team0.removePlayer(playerId);
        team1.removePlayer(playerId);
    }

    private void tryStartGame() {
        if (!team0.isEmpty() && !team1.isEmpty() && phase == GamePhase.WAITING) {
            phase = GamePhase.SERVING;
        }
    }

    public int getTeamForPlayer(UUID playerId) {
        if (team0.hasPlayer(playerId)) return 0;
        if (team1.hasPlayer(playerId)) return 1;
        return -1;
    }

    public void onBallHitGround(int lastHitTeam, Vec3 groundPos) {
        if (phase != GamePhase.IN_RALLY) return;
        int groundSide = getSideForPosition(groundPos);
        if (groundSide < 0) {
            // Out of bounds — point goes to opposite team
            awardPoint(lastHitTeam == 0 ? 1 : 0, "Ball out of bounds!");
        } else {
            // Ball lands on groundSide's court — other team scores
            awardPoint(groundSide == 0 ? 1 : 0,
                "Ball landed on " + (groundSide == 0 ? "Team 1" : "Team 2") + "'s side!");
        }
    }

    public void onFault(int faultingTeam) {
        if (phase != GamePhase.IN_RALLY) return;
        awardPoint(faultingTeam == 0 ? 1 : 0, "Team " + (faultingTeam + 1) + " fouled (4 touches)!");
    }

    private void awardPoint(int teamId, String reason) {
        Team winner = teamId == 0 ? team0 : team1;
        winner.addPoint();
        phase = GamePhase.POINT_SCORED;

        if (isGameOver()) {
            phase = GamePhase.GAME_OVER;
        } else {
            servingTeam = teamId;
            phase = GamePhase.SERVING;
        }
    }

    private boolean isGameOver() {
        int s0 = team0.getScore(), s1 = team1.getScore();
        return (s0 >= POINTS_TO_WIN && s0 - s1 >= MIN_LEAD) ||
               (s1 >= POINTS_TO_WIN && s1 - s0 >= MIN_LEAD);
    }

    /** Returns which team's side the given world position falls on (-1 = out of bounds). */
    private int getSideForPosition(Vec3 pos) {
        if (netAlongZ) {
            double minX = Math.min(pole1Pos.getX(), pole2Pos.getX()) - 9;
            double maxX = Math.max(pole1Pos.getX(), pole2Pos.getX()) + 9;
            if (pos.x < minX || pos.x > maxX) return -1;
            return pos.x < netX ? 0 : 1;
        } else {
            double minZ = Math.min(pole1Pos.getZ(), pole2Pos.getZ()) - 9;
            double maxZ = Math.max(pole1Pos.getZ(), pole2Pos.getZ()) + 9;
            if (pos.z < minZ || pos.z > maxZ) return -1;
            return pos.z < netZ ? 0 : 1;
        }
    }

    public void setPhase(GamePhase phase) { this.phase = phase; }
    public GamePhase getPhase() { return phase; }
    public int getServingTeam() { return servingTeam; }
    public Team getTeam(int id) { return id == 0 ? team0 : team1; }
    public UUID getCourtId() { return courtId; }
    public BlockPos getPole1Pos() { return pole1Pos; }
    public BlockPos getPole2Pos() { return pole2Pos; }
    public Vec3 getNetCenter() { return netCenter; }
}
