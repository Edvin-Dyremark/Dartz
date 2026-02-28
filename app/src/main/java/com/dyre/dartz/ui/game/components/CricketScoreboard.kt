package com.dyre.dartz.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dyre.dartz.game.impl.CricketGameEngine
import com.dyre.dartz.model.PlayerState

@Composable
fun CricketScoreboard(
    players: List<PlayerState>,
    currentPlayerIndex: Int,
    deadNumbers: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val cricketNumbers = listOf(20, 19, 18, 17, 16, 15, 25)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        // Header row with player names
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Number label column
            Box(
                modifier = Modifier.size(width = 40.dp, height = 24.dp),
                contentAlignment = Alignment.Center,
            ) {}
            players.forEachIndexed { index, playerState ->
                val isCurrent = index == currentPlayerIndex
                val bgColor = if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = if (isCurrent) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(bgColor)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = playerState.player.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Cricket number rows
        cricketNumbers.forEach { number ->
            val isDead = number in deadNumbers
            val rowAlpha = if (isDead) 0.35f else 1f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(rowAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Number label
                Box(
                    modifier = Modifier.size(width = 40.dp, height = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (number == 25) "Bull" else number.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                // Player mark cells
                players.forEachIndexed { index, playerState ->
                    val isCurrent = index == currentPlayerIndex
                    val bgColor = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                    val marksKey = "${CricketGameEngine.MARKS_KEY_PREFIX}$number"
                    val marks = (playerState.extras[marksKey] as? Int) ?: 0

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .background(bgColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        MarkIndicator(marks = marks)
                    }
                }
            }
        }

        // Score row at bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(width = 40.dp, height = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Pts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            players.forEachIndexed { index, playerState ->
                val isCurrent = index == currentPlayerIndex
                val bgColor = if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = if (isCurrent) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(bgColor)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = playerState.score.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkIndicator(marks: Int) {
    val isClosed = marks >= 3
    val color = if (isClosed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    when {
        marks == 0 -> {
            Text(
                text = "",
                style = MaterialTheme.typography.labelMedium,
            )
        }
        marks == 1 -> {
            Text(
                text = "/",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        marks == 2 -> {
            Text(
                text = "X",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        else -> {
            // 3+ marks: X inside a circle
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(width = 2.dp, color = color, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        }
    }
}
