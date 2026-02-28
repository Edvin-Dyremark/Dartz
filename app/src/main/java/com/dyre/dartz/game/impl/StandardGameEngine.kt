package com.dyre.dartz.game.impl

import com.dyre.dartz.game.GameEngine
import com.dyre.dartz.game.GameMode
import com.dyre.dartz.model.DartScore
import com.dyre.dartz.model.GameConfig
import com.dyre.dartz.model.GameState
import com.dyre.dartz.model.PlayerState

class StandardGameEngine(private val mode: GameMode) : GameEngine {

    private val startScore: Int
        get() = when (mode) {
            is GameMode.ThreeOhOne -> 301
            is GameMode.FiveOhOne -> 501
            else -> 501
        }

    override fun createInitialState(config: GameConfig): GameState {
        return GameState(
            players = config.players.map { PlayerState(it, startScore) },
            currentPlayerIndex = 0,
            currentDartIndex = 0,
            dartsThisRound = emptyList(),
            isGameOver = false,
            winnerId = null,
            message = null,
        )
    }

    override fun processDart(state: GameState, dart: DartScore): GameState {
        if (state.isGameOver || state.currentDartIndex >= 3) return state

        val currentPlayer = state.players[state.currentPlayerIndex]
        val newScore = currentPlayer.score - dart.points

        // Bust: score goes below 0, to exactly 1, or to 0 without a double
        val isBust = newScore < 0 || newScore == 1 || (newScore == 0 && !dart.isDouble)

        if (isBust) {
            // Revert all darts this round — score goes back to what it was before this turn
            val originalScore = dartsThisRound(state).fold(currentPlayer.score) { acc, d -> acc }
            val revertedScore = currentPlayer.score
                .let { scoreBeforeThisDart ->
                    // We need the score at the start of the turn
                    state.dartsThisRound.fold(scoreBeforeThisDart) { acc, d -> acc + d.points }
                }
            val updatedPlayers = state.players.toMutableList()
            updatedPlayers[state.currentPlayerIndex] = currentPlayer.copy(score = revertedScore)
            return state.copy(
                players = updatedPlayers,
                dartsThisRound = state.dartsThisRound + dart,
                currentDartIndex = 3, // Force end of turn
                message = "Bust! Score reverted to $revertedScore",
                previousState = state,
            )
        }

        // Successful dart
        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[state.currentPlayerIndex] = currentPlayer.copy(score = newScore)

        val isWin = newScore == 0 && dart.isDouble
        val newDartIndex = state.currentDartIndex + 1

        return state.copy(
            players = updatedPlayers,
            currentDartIndex = newDartIndex,
            dartsThisRound = state.dartsThisRound + dart,
            isGameOver = isWin,
            winnerId = if (isWin) currentPlayer.player.id else null,
            message = if (isWin) "${currentPlayer.player.name} wins!" else "${dart.displayName} — ${dart.points} points!",
            previousState = state,
        )
    }

    override fun undoLastDart(state: GameState): GameState {
        return state.previousState ?: state
    }

    override fun endTurn(state: GameState): GameState {
        if (state.isGameOver) return state
        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[state.currentPlayerIndex] = updatedPlayers[state.currentPlayerIndex].copy(
            lastRoundDarts = state.dartsThisRound,
        )
        val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size
        return state.copy(
            players = updatedPlayers,
            currentPlayerIndex = nextPlayerIndex,
            currentDartIndex = 0,
            dartsThisRound = emptyList(),
            message = null,
            previousState = null,
        )
    }

    private fun dartsThisRound(state: GameState): List<DartScore> = state.dartsThisRound
}
