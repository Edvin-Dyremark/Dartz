package com.dyre.dartz.ui.game

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
            state = state,
            landingMarkers = landingMarkers,
            onDartThrown = { position, center, radius ->
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                viewModel.throwMiddlingDart(position, center, radius)
            },
        )
        return
    }

    val currentPlayer = state.players[state.currentPlayerIndex]

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top margin
            Spacer(modifier = Modifier.height(16.dp))

            // 1. All player scores at top
            Scoreboard(
                players = state.players,
                currentPlayerIndex = state.currentPlayerIndex,
            )

            // Push current player info + board down
            Spacer(modifier = Modifier.weight(1f))

            // 2. Current player turn info with darts
            PlayerTurnBanner(
                playerName = currentPlayer.player.name,
                score = currentPlayer.score,
                dartsThisRound = state.dartsThisRound,
            )

            // 3. Dartboard
            Dartboard(
                onDartThrown = { score, position ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.throwDart(score, position)
                },
                modifier = Modifier.padding(horizontal = 2.dp),
                landingMarkers = landingMarkers,
                isCricket = viewModel.isCricket,
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
                ) {
                    Text("Next Turn")
                }
            }
        }
    }
}

@Composable
private fun MiddlingScreen(
    state: com.dyre.dartz.model.GameState,
    landingMarkers: List<Offset>,
    onDartThrown: (position: Offset, center: Offset, radius: Float) -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Middling",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.message ?: "Throw at the bull to determine order",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.middlingPlayerIndex < state.players.size) {
                val currentMiddler = state.players[state.middlingPlayerIndex]
                Text(
                    text = "${currentMiddler.player.name}'s throw",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val size = constraints.maxWidth.toFloat()
                    val center = Offset(size / 2f, size / 2f)
                    val boardRadius = size / 2f

                    Dartboard(
                        onDartThrown = { _, position ->
                            onDartThrown(position, center, boardRadius)
                        },
                        landingMarkers = landingMarkers,
                    )
                }
            }
        }
    }
}
