package com.dyre.dartz.model

data class GameState(
    val players: List<PlayerState>,
    val currentPlayerIndex: Int,
    val currentDartIndex: Int,
    val dartsThisRound: List<DartScore>,
    val isGameOver: Boolean,
    val winnerId: Int?,
    val message: String?,
    val previousState: GameState? = null,
    val isMiddling: Boolean = false,
    val middlingResults: Map<Int, Float> = emptyMap(),
    val middlingPlayerIndex: Int = 0,
)
