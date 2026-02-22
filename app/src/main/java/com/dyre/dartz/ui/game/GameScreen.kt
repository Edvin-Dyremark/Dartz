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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val snackbarHostState = remember { SnackbarHostState() }
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

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val currentPlayer = state.players[state.currentPlayerIndex]

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            PlayerTurnBanner(
                playerName = currentPlayer.player.name,
                score = currentPlayer.score,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Dartboard(
                onDartThrown = { score ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.throwDart(score)
                },
                landingMarkers = landingMarkers,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // This turn's darts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (i in 0 until 3) {
                    val dart = state.dartsThisRound.getOrNull(i)
                    Text(
                        text = dart?.displayName ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dart != null) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Scoreboard(
                players = state.players,
                currentPlayerIndex = state.currentPlayerIndex,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.undo() },
                    modifier = Modifier.weight(1f),
                    enabled = state.dartsThisRound.isNotEmpty(),
                ) {
                    Text("Undo")
                }
                OutlinedButton(
                    onClick = { viewModel.throwMiss() },
                    modifier = Modifier.weight(1f),
                    enabled = state.currentDartIndex < 3,
                ) {
                    Text("Miss")
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
