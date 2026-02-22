package com.dyre.dartz.game

import com.dyre.dartz.model.DartScore
import com.dyre.dartz.model.GameConfig
import com.dyre.dartz.model.GameState

interface GameEngine {
    fun createInitialState(config: GameConfig): GameState
    fun processDart(state: GameState, dart: DartScore): GameState
    fun undoLastDart(state: GameState): GameState
    fun endTurn(state: GameState): GameState
}
