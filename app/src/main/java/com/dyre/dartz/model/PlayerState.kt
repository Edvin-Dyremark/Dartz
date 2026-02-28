package com.dyre.dartz.model

data class PlayerState(
    val player: Player,
    val score: Int,
    val extras: Map<String, Any> = emptyMap(),
    val lastRoundDarts: List<DartScore> = emptyList(),
)
