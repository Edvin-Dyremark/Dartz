# Dartz

A dart scoring app for Android built with Kotlin and Jetpack Compose.

## Features

- **Game Modes**: 301, 501, Cricket, and Killer
- **Interactive Dartboard**: Tap to throw or press-and-drag for a magnified zoom view with score label
- **Middling Phase**: Players throw at the bull before the game to determine turn order
- **Multi-player**: Supports 2-8 players with live scoreboard showing each player's last round
- **Undo Support**: Undo individual dart throws within a turn

## Game Modes

| Mode | Description |
|------|-------------|
| **301 / 501** | Count down from starting score. Must finish on a double. Bust reverts the round. |
| **Cricket** | Close numbers 15-20 and bull by hitting them 3 times. Score points on closed numbers. |
| **Killer** | Claim a number, become a killer by hitting your own double, then eliminate opponents by hitting their doubles. |

## Building

```bash
./gradlew assembleDebug
```

Requires Android SDK and JDK 17+.

## Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Compose Navigation
- ViewModel + StateFlow
