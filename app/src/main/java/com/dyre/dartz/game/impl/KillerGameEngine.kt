package com.dyre.dartz.game.impl

import com.dyre.dartz.game.GameEngine
import com.dyre.dartz.model.DartScore
import com.dyre.dartz.model.GameConfig
import com.dyre.dartz.model.GameState
import com.dyre.dartz.model.PlayerState

class KillerGameEngine : GameEngine {

    companion object {
        const val LIVES_KEY = "lives"
        const val CLAIMED_NUMBER_KEY = "claimedNumber"
        const val PHASE_KEY = "phase" // "claiming" or "killing"
        const val IS_KILLER_KEY = "isKiller"
        const val MARKS_KEY = "marks"
        const val ELIMINATION_ORDER_KEY = "eliminationOrder"
        const val MAX_LIVES = 3
        const val MARKS_TO_KILLER = 3
    }

    override fun createInitialState(config: GameConfig): GameState {
        val initialExtras = mapOf<String, Any>(
            LIVES_KEY to MAX_LIVES,
            CLAIMED_NUMBER_KEY to 0,
            PHASE_KEY to "claiming",
            IS_KILLER_KEY to false,
            MARKS_KEY to 0,
        )
        return GameState(
            players = config.players.map { PlayerState(it, MAX_LIVES, initialExtras) },
            currentPlayerIndex = 0,
            currentDartIndex = 0,
            dartsThisRound = emptyList(),
            isGameOver = false,
            winnerId = null,
            message = "Each player throws one dart to select their number",
        )
    }

    override fun processDart(state: GameState, dart: DartScore): GameState {
        if (state.isGameOver || state.currentDartIndex >= 3) return state

        val currentPlayer = state.players[state.currentPlayerIndex]
        val phase = currentPlayer.extras[PHASE_KEY] as? String ?: "claiming"

        return if (phase == "claiming") {
            processClaimDart(state, dart, currentPlayer)
        } else {
            processKillDart(state, dart, currentPlayer)
        }
    }

    private fun processClaimDart(state: GameState, dart: DartScore, currentPlayer: PlayerState): GameState {
        if (dart.segment == 0 || dart.segment == 25) {
            return state.copy(
                currentDartIndex = state.currentDartIndex + 1,
                dartsThisRound = state.dartsThisRound + dart,
                message = "Throw at a number 1-20 to claim it",
                previousState = state,
            )
        }

        // Check if number is already claimed
        val alreadyClaimed = state.players.any { (it.extras[CLAIMED_NUMBER_KEY] as? Int) == dart.segment }
        if (alreadyClaimed) {
            return state.copy(
                currentDartIndex = state.currentDartIndex + 1,
                dartsThisRound = state.dartsThisRound + dart,
                message = "${dart.segment} is already claimed! Try another number",
                previousState = state,
            )
        }

        val updatedPlayer = currentPlayer.copy(
            extras = currentPlayer.extras + (CLAIMED_NUMBER_KEY to dart.segment) + (PHASE_KEY to "killing"),
        )
        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[state.currentPlayerIndex] = updatedPlayer

        // Check if all players have claimed
        val allClaimed = updatedPlayers.all { (it.extras[CLAIMED_NUMBER_KEY] as? Int ?: 0) > 0 }

        return state.copy(
            players = updatedPlayers,
            currentDartIndex = 3, // End turn after claiming
            dartsThisRound = state.dartsThisRound + dart,
            message = "${currentPlayer.player.name} claimed ${dart.segment}!" +
                    if (!allClaimed) "" else " All numbers claimed — game on!",
            previousState = state,
        )
    }

    private fun processKillDart(state: GameState, dart: DartScore, currentPlayer: PlayerState): GameState {
        val marks = (currentPlayer.extras[MARKS_KEY] as? Int) ?: 0
        val lives = currentPlayer.extras[LIVES_KEY] as? Int ?: MAX_LIVES
        val isKiller = marks >= MARKS_TO_KILLER && lives >= MAX_LIVES
        val claimedNumber = currentPlayer.extras[CLAIMED_NUMBER_KEY] as? Int ?: 0

        // Hit own number — accumulate marks or regain lives
        if (dart.segment == claimedNumber) {
            if (marks < MARKS_TO_KILLER) {
                // Accumulate marks toward killer
                val newMarks = (marks + dart.multiplier).coerceAtMost(MARKS_TO_KILLER)
                val becomesKiller = newMarks >= MARKS_TO_KILLER && lives >= MAX_LIVES

                val updatedPlayer = currentPlayer.copy(
                    extras = currentPlayer.extras +
                            (MARKS_KEY to newMarks) +
                            (IS_KILLER_KEY to becomesKiller),
                )
                val updatedPlayers = state.players.toMutableList()
                updatedPlayers[state.currentPlayerIndex] = updatedPlayer

                val message = if (becomesKiller) {
                    "${currentPlayer.player.name} is now a KILLER!"
                } else {
                    "${currentPlayer.player.name} marks $newMarks/$MARKS_TO_KILLER"
                }

                return state.copy(
                    players = updatedPlayers,
                    currentDartIndex = state.currentDartIndex + 1,
                    dartsThisRound = state.dartsThisRound + dart,
                    message = message,
                    previousState = state,
                )
            } else if (lives < MAX_LIVES) {
                // Already has 3 marks but lost lives — regain lives
                val newLives = (lives + dart.multiplier).coerceAtMost(MAX_LIVES)
                val becomesKiller = newLives >= MAX_LIVES

                val updatedPlayer = currentPlayer.copy(
                    score = newLives,
                    extras = currentPlayer.extras +
                            (LIVES_KEY to newLives) +
                            (IS_KILLER_KEY to becomesKiller),
                )
                val updatedPlayers = state.players.toMutableList()
                updatedPlayers[state.currentPlayerIndex] = updatedPlayer

                val message = if (becomesKiller) {
                    "${currentPlayer.player.name} is now a KILLER!"
                } else {
                    "${currentPlayer.player.name} regains ${dart.multiplier} ${if (dart.multiplier == 1) "life" else "lives"}! ($newLives/$MAX_LIVES)"
                }

                return state.copy(
                    players = updatedPlayers,
                    currentDartIndex = state.currentDartIndex + 1,
                    dartsThisRound = state.dartsThisRound + dart,
                    message = message,
                    previousState = state,
                )
            }
            // Already killer with max lives — no effect
            return state.copy(
                currentDartIndex = state.currentDartIndex + 1,
                dartsThisRound = state.dartsThisRound + dart,
                message = dart.displayName,
                previousState = state,
            )
        }

        // As killer, hit opponent's claimed number — remove lives and killer status
        if (isKiller) {
            val targetIdx = state.players.indexOfFirst {
                (it.extras[CLAIMED_NUMBER_KEY] as? Int) == dart.segment &&
                        it.player.id != currentPlayer.player.id &&
                        (it.extras[LIVES_KEY] as? Int ?: 0) > 0
            }
            if (targetIdx >= 0) {
                val target = state.players[targetIdx]
                val targetLives = ((target.extras[LIVES_KEY] as? Int ?: MAX_LIVES) - dart.multiplier).coerceAtLeast(0)
                val updatedPlayers = state.players.toMutableList()

                val targetExtras = target.extras +
                        (LIVES_KEY to targetLives) +
                        (IS_KILLER_KEY to false) +
                        if (targetLives <= 0) {
                            val alreadyEliminated = state.players.count { it.extras[ELIMINATION_ORDER_KEY] as? Int ?: 0 > 0 }
                            mapOf(ELIMINATION_ORDER_KEY to alreadyEliminated + 1)
                        } else emptyMap()
                updatedPlayers[targetIdx] = target.copy(
                    score = targetLives,
                    extras = targetExtras,
                )

                // Check win: only one player with lives > 0
                val alive = updatedPlayers.filter { (it.extras[LIVES_KEY] as? Int ?: 0) > 0 }
                val isWin = alive.size == 1

                return state.copy(
                    players = updatedPlayers,
                    currentDartIndex = state.currentDartIndex + 1,
                    dartsThisRound = state.dartsThisRound + dart,
                    isGameOver = isWin,
                    winnerId = if (isWin) alive.first().player.id else null,
                    message = if (isWin) "${alive.first().player.name} wins!"
                    else "${target.player.name} loses ${dart.multiplier} ${if (dart.multiplier == 1) "life" else "lives"}! ($targetLives remaining)",
                    previousState = state,
                )
            }
        }

        // Normal non-scoring dart
        return state.copy(
            currentDartIndex = state.currentDartIndex + 1,
            dartsThisRound = state.dartsThisRound + dart,
            message = dart.displayName,
            previousState = state,
        )
    }

    override fun undoLastDart(state: GameState): GameState {
        return state.previousState ?: state
    }

    override fun endTurn(state: GameState): GameState {
        if (state.isGameOver) return state

        // Skip eliminated players
        var nextIndex = (state.currentPlayerIndex + 1) % state.players.size
        var attempts = 0
        while (attempts < state.players.size) {
            val nextPlayer = state.players[nextIndex]
            val lives = nextPlayer.extras[LIVES_KEY] as? Int ?: 0
            val hasClaimed = (nextPlayer.extras[CLAIMED_NUMBER_KEY] as? Int ?: 0) > 0
            if (lives > 0 || !hasClaimed) break // alive or still claiming
            nextIndex = (nextIndex + 1) % state.players.size
            attempts++
        }

        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[state.currentPlayerIndex] = updatedPlayers[state.currentPlayerIndex].copy(
            lastRoundDarts = state.dartsThisRound,
        )
        return state.copy(
            players = updatedPlayers,
            currentPlayerIndex = nextIndex,
            currentDartIndex = 0,
            dartsThisRound = emptyList(),
            message = null,
            previousState = state,
        )
    }
}
