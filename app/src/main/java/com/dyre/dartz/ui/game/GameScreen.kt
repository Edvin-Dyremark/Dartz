package com.dyre.dartz.ui.game

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dyre.dartz.game.impl.CricketGameEngine
import com.dyre.dartz.game.impl.KillerGameEngine
import com.dyre.dartz.ui.game.components.CricketScoreboard
import com.dyre.dartz.ui.game.components.Dartboard
import com.dyre.dartz.ui.game.components.KillerScoreboard
import com.dyre.dartz.ui.game.components.PlayerTurnBanner
import com.dyre.dartz.ui.game.components.Scoreboard

@Composable
fun GameScreen(
    modeArg: String,
    playersArg: String,
    onGameOver: (winnerId: Int, winnerName: String, finalScores: String) -> Unit,
    viewModel: GameViewModel = viewModel(),
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val landingMarkers by viewModel.landingMarkers.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(Unit) {
        viewModel.initialize(modeArg, playersArg)
    }

    val state = gameState ?: return

    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver && state.winnerId != null) {
            val winner = state.players.find { it.player.id == state.winnerId }
            if (winner != null) {
                val finalScores = state.players.joinToString(";") {
                    "${it.player.name}:${it.score}"
                }
                onGameOver(winner.player.id, winner.player.name, finalScores)
            }
        }
    }

    if (state.isMiddling) {
        MiddlingScreen(
            players = state.players,
            onPlayerSelected = { playerId -> viewModel.selectFirstPlayer(playerId) },
        )
        return
    }

    val currentPlayer = state.players[state.currentPlayerIndex]
    val threwThreeDarts = state.currentDartIndex >= 3

    // Compute cricket-specific sets
    val deadNumbers = if (viewModel.isCricket) {
        CricketGameEngine.CRICKET_NUMBERS.filter { number ->
            val key = "${CricketGameEngine.MARKS_KEY_PREFIX}$number"
            state.players.all { ((it.extras[key] as? Int) ?: 0) >= 3 }
        }.toSet()
    } else emptySet()

    // Compute killer-specific sets
    val allClaimed = viewModel.isKiller && state.players.all {
        (it.extras[KillerGameEngine.CLAIMED_NUMBER_KEY] as? Int ?: 0) > 0
    }

    val currentPhase = currentPlayer.extras[KillerGameEngine.PHASE_KEY] as? String
    val isClaimingPhase = viewModel.isKiller && currentPhase == "claiming"

    // Numbers already claimed by other players (for dimming during setup)
    val claimedNumbers = if (viewModel.isKiller) {
        state.players.mapNotNull { it.extras[KillerGameEngine.CLAIMED_NUMBER_KEY] as? Int }
            .filter { it > 0 }
            .toSet()
    } else emptySet()

    val killerLitNumbers = when {
        // During claiming: all 1-20 are lit except already-claimed ones
        isClaimingPhase -> (1..20).toSet() - claimedNumbers
        // All claimed, playing phase
        viewModel.isKiller && allClaimed -> {
            val currentIsKiller = currentPlayer.extras[KillerGameEngine.IS_KILLER_KEY] as? Boolean ?: false
            val currentClaimed = currentPlayer.extras[KillerGameEngine.CLAIMED_NUMBER_KEY] as? Int ?: 0
            if (currentIsKiller) {
                // Killer sees all alive claimed numbers (to attack)
                state.players.filter {
                    val claimed = it.extras[KillerGameEngine.CLAIMED_NUMBER_KEY] as? Int ?: 0
                    val lives = it.extras[KillerGameEngine.LIVES_KEY] as? Int ?: 0
                    claimed > 0 && lives > 0
                }.map { it.extras[KillerGameEngine.CLAIMED_NUMBER_KEY] as Int }.toSet()
            } else {
                // Not yet killer — only own number lit
                if (currentClaimed > 0) setOf(currentClaimed) else emptySet()
            }
        }
        else -> emptySet()
    }

    // Enable dimming during claiming phase too (not just after all claimed)
    val killerDimmingActive = isClaimingPhase || allClaimed

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. All player scores at top
            Spacer(modifier = Modifier.height(8.dp))
            if (viewModel.isCricket) {
                CricketScoreboard(
                    players = state.players,
                    currentPlayerIndex = state.currentPlayerIndex,
                    deadNumbers = deadNumbers,
                )
            } else if (viewModel.isKiller) {
                KillerScoreboard(
                    players = state.players,
                    currentPlayerIndex = state.currentPlayerIndex,
                )
            } else {
                Scoreboard(
                    players = state.players,
                    currentPlayerIndex = state.currentPlayerIndex,
                )
            }

            // 2. Current player name + score + darts
            Spacer(modifier = Modifier.height(8.dp))
            val killerClaiming = viewModel.isKiller &&
                    (currentPlayer.extras[KillerGameEngine.PHASE_KEY] as? String) == "claiming"
            PlayerTurnBanner(
                playerName = currentPlayer.player.name,
                score = currentPlayer.score,
                dartsThisRound = state.dartsThisRound,
                showScore = !viewModel.isKiller,
                showDarts = !killerClaiming,
                useDartNames = viewModel.isCricket || viewModel.isKiller,
            )

            // 3. Dartboard — weight(1f) fills remaining space, touch works in all of it
            Dartboard(
                onDartThrown = { score, position ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.throwDart(score, position)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                landingMarkers = landingMarkers,
                isCricket = viewModel.isCricket,
                deadNumbers = deadNumbers,
                isKiller = viewModel.isKiller,
                killerLitNumbers = killerLitNumbers,
                killerDimmingActive = killerDimmingActive,
            )

            // 4. Action buttons at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.undo() },
                    modifier = Modifier.weight(1f),
                    enabled = viewModel.canUndo,
                ) {
                    Text("Undo")
                }
                Button(
                    onClick = { viewModel.endTurn() },
                    modifier = Modifier.weight(1f),
                    enabled = threwThreeDarts,
                ) {
                    Text("Next Turn")
                }
            }
        }
    }
}

@Composable
private fun MiddlingScreen(
    players: List<com.dyre.dartz.model.PlayerState>,
    onPlayerSelected: (playerId: Int) -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Who won middling?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            players.forEach { playerState ->
                Button(
                    onClick = { onPlayerSelected(playerState.player.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 4.dp),
                ) {
                    Text(playerState.player.name)
                }
            }
        }
    }
}
