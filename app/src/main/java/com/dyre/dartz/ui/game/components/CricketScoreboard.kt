package com.dyre.dartz.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    // Numbers as columns: 15, 16, 17, 18, 19, 20, Bull
    val cricketNumbers = listOf(15, 16, 17, 18, 19, 20, 25)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        // Header row: player name label + number columns + Pts column
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Empty corner for player name column
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(20.dp),
            )
            cricketNumbers.forEach { number ->
                val isDead = number in deadNumbers
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .alpha(if (isDead) 0.35f else 1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (number == 25) "B" else number.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Pts header
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Pts",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // One row per player
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(bgColor)
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Player name
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = playerState.player.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                // Mark cells for each cricket number
                cricketNumbers.forEach { number ->
                    val isDead = number in deadNumbers
                    val marksKey = "${CricketGameEngine.MARKS_KEY_PREFIX}$number"
                    val marks = (playerState.extras[marksKey] as? Int) ?: 0

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .alpha(if (isDead) 0.35f else 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        MarkIndicator(marks = marks)
                    }
                }

                // Score
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = playerState.score.toString(),
                        style = MaterialTheme.typography.labelMedium,
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
        marks == 0 -> {}
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
