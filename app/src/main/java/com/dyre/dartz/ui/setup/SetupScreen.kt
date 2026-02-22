package com.dyre.dartz.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dyre.dartz.game.GameMode

@Composable
fun SetupScreen(
    onStartGame: (mode: String, players: String) -> Unit,
    viewModel: SetupViewModel = viewModel(),
) {
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val playerNames by viewModel.playerNames.collectAsStateWithLifecycle()

    val modes = listOf(GameMode.ThreeOhOne, GameMode.FiveOhOne, GameMode.Cricket, GameMode.Killer)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Dartz",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("Game Mode", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                modes.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        label = { Text(mode.displayName) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Players", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconButton(onClick = { viewModel.setPlayerCount(playerNames.size - 1) }) {
                    Text("-", style = MaterialTheme.typography.headlineMedium)
                }
                Text(
                    "${playerNames.size}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                IconButton(onClick = { viewModel.setPlayerCount(playerNames.size + 1) }) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            playerNames.forEachIndexed { index, name ->
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.setPlayerName(index, it) },
                    label = { Text("Player ${index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val modeStr = selectedMode.displayName
                    val playersStr = playerNames.joinToString(",")
                    onStartGame(modeStr, playersStr)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start Game", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
