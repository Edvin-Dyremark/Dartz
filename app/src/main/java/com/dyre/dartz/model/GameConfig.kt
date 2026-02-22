package com.dyre.dartz.model

import com.dyre.dartz.game.GameMode

data class GameConfig(
    val mode: GameMode,
    val players: List<Player>,
)
