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
import com.dyre.dartz.ui.game.components.CricketScoreboard
import com.dyre.dartz.ui.game.components.Dartboard
import com.dyre.dartz.ui.game.components.PlayerTurnBanner
import com.dyre.dartz.ui.game.components.Scoreboard

@Composable
fun GameScreen(
    modeArg: String,
    playersArg: String,
    onGameOver: (winnerId: Int, winnerName: String) -> Unit,
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
                onGameOver(winner.player.id, winner.player.name)
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

    val activeNumbers = if (viewModel.isCricket) {
        CricketGameEngine.CRICKET_NUMBERS.filter { number ->
            val key = "${CricketGameEngine.MARKS_KEY_PREFIX}$number"
            ((currentPlayer.extras[key] as? Int) ?: 0) >= 3 && number !in deadNumbers
        }.toSet()
    } else emptySet()

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
            } else {
                Scoreboard(
                    players = state.players,
                    currentPlayerIndex = state.currentPlayerIndex,
                )
            }

            // 2. Current player name + score + darts
            Spacer(modifier = Modifier.height(8.dp))
            if (viewModel.isCricket) {
                PlayerTurnBanner(
                    playerName = currentPlayer.player.name,
                    score = currentPlayer.score,
                )
            } else {
                PlayerTurnBanner(
                    playerName = currentPlayer.player.name,
                    score = currentPlayer.score,
                    dartsThisRound = state.dartsThisRound,
                )
            }

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
                activeNumbers = activeNumbers,
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
