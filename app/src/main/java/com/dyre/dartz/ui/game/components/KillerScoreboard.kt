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
import com.dyre.dartz.game.impl.KillerGameEngine
import com.dyre.dartz.model.PlayerState

@Composable
fun KillerScoreboard(
    players: List<PlayerState>,
    currentPlayerIndex: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Marks",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Lives",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "K",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // One row per player
        players.forEachIndexed { index, playerState ->
            val isCurrent = index == currentPlayerIndex
            val lives = playerState.extras[KillerGameEngine.LIVES_KEY] as? Int ?: KillerGameEngine.MAX_LIVES
            val isEliminated = lives <= 0 && (playerState.extras[KillerGameEngine.CLAIMED_NUMBER_KEY] as? Int ?: 0) > 0
            val claimedNumber = playerState.extras[KillerGameEngine.CLAIMED_NUMBER_KEY] as? Int ?: 0
            val marks = (playerState.extras[KillerGameEngine.MARKS_KEY] as? Int) ?: 0
            val isKiller = playerState.extras[KillerGameEngine.IS_KILLER_KEY] as? Boolean ?: false

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
                    .alpha(if (isEliminated) 0.35f else 1f)
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

                // Claimed number
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (claimedNumber > 0) claimedNumber.toString() else "—",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                    )
                }

                // Marks toward killer (/, X, circled X)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (claimedNumber > 0) {
                        KillerMarkIndicator(marks = marks)
                    }
                }

                // Lives
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = lives.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                    )
                }

                // Killer status
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isKiller) {
                        Text(
                            text = "K",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KillerMarkIndicator(marks: Int) {
    val color = if (marks >= 3) {
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
