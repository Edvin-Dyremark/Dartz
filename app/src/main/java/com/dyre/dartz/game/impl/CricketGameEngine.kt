package com.dyre.dartz.game.impl

import com.dyre.dartz.game.GameEngine
import com.dyre.dartz.model.DartScore
import com.dyre.dartz.model.GameConfig
import com.dyre.dartz.model.GameState
import com.dyre.dartz.model.PlayerState

class CricketGameEngine : GameEngine {

    companion object {
        val CRICKET_NUMBERS = listOf(15, 16, 17, 18, 19, 20, 25)
        const val MARKS_KEY_PREFIX = "marks_"
    }

    override fun createInitialState(config: GameConfig): GameState {
        val initialExtras = CRICKET_NUMBERS.associate { "${MARKS_KEY_PREFIX}$it" to (0 as Any) }
        return GameState(
            players = config.players.map { PlayerState(it, 0, initialExtras) },
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
        val segment = dart.segment
        val multiplier = dart.multiplier

        // Only cricket numbers count
        if (segment !in CRICKET_NUMBERS) {
            return state.copy(
                currentDartIndex = state.currentDartIndex + 1,
                dartsThisRound = state.dartsThisRound + dart,
                message = "${dart.displayName} — doesn't count in Cricket",
                previousState = state,
            )
        }

        val marksKey = "${MARKS_KEY_PREFIX}$segment"
        val currentMarks = (currentPlayer.extras[marksKey] as? Int) ?: 0
        val newMarksTotal = currentMarks + multiplier

        // Marks needed to close: 3
        val marksToClose = 3
        val alreadyClosed = currentMarks >= marksToClose
        var pointsScored = 0

        if (!alreadyClosed) {
            val marksAfterClose = newMarksTotal - marksToClose
            if (marksAfterClose > 0) {
                // Check if all opponents have closed this number
                val opponentsClosed = state.players.filterIndexed { i, _ -> i != state.currentPlayerIndex }
                    .all { ((it.extras[marksKey] as? Int) ?: 0) >= marksToClose }
                if (!opponentsClosed) {
                    pointsScored = marksAfterClose * segment
                }
            }
        } else {
            // Already closed — can score if opponents haven't closed
            val opponentsClosed = state.players.filterIndexed { i, _ -> i != state.currentPlayerIndex }
                .all { ((it.extras[marksKey] as? Int) ?: 0) >= marksToClose }
            if (!opponentsClosed) {
                pointsScored = multiplier * segment
            }
        }

        val updatedExtras = currentPlayer.extras + (marksKey to newMarksTotal)
        val updatedPlayer = currentPlayer.copy(
            score = currentPlayer.score + pointsScored,
            extras = updatedExtras,
        )

        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[state.currentPlayerIndex] = updatedPlayer

        // Check win: all numbers closed and highest (or tied highest) score
        val allClosed = CRICKET_NUMBERS.all { num ->
            ((updatedPlayer.extras["${MARKS_KEY_PREFIX}$num"] as? Int) ?: 0) >= marksToClose
        }
        val hasHighestScore = allClosed && updatedPlayers.filterIndexed { i, _ -> i != state.currentPlayerIndex }
            .all { updatedPlayer.score >= it.score }

        return state.copy(
            players = updatedPlayers,
            currentDartIndex = state.currentDartIndex + 1,
            dartsThisRound = state.dartsThisRound + dart,
            isGameOver = hasHighestScore,
            winnerId = if (hasHighestScore) updatedPlayer.player.id else null,
            message = if (hasHighestScore) "${updatedPlayer.player.name} wins!"
            else "${dart.displayName}${if (pointsScored > 0) " — $pointsScored points!" else ""}",
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
            previousState = state,
        )
    }
}
