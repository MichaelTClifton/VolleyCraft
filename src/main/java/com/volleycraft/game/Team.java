package com.volleycraft.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Team {
    private final int id;
    private final List<UUID> players = new ArrayList<>();
    private int score = 0;

    public Team(int id) {
        this.id = id;
    }

    public void addPlayer(UUID playerId) {
        if (!players.contains(playerId)) players.add(playerId);
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    public boolean hasPlayer(UUID playerId) {
        return players.contains(playerId);
    }

    public void addPoint() { score++; }
    public int getScore() { return score; }
    public int getId() { return id; }
    public List<UUID> getPlayers() { return players; }
    public boolean isEmpty() { return players.isEmpty(); }
    public void reset() { score = 0; }
}
