package com.volleycraft.game;

public enum GamePhase {
    WAITING,    // Waiting for players to join
    SERVING,    // A team is about to serve
    IN_RALLY,   // Ball is in play
    POINT_SCORED, // A point was just scored
    GAME_OVER   // Match is finished
}
