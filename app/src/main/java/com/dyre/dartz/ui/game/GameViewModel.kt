package com.dyre.dartz.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.dyre.dartz.game.GameEngine
import com.dyre.dartz.game.GameMode
import com.dyre.dartz.game.impl.CricketGameEngine
import com.dyre.dartz.game.impl.KillerGameEngine
import com.dyre.dartz.game.impl.StandardGameEngine
import com.dyre.dartz.model.DartScore
import com.dyre.dartz.model.GameConfig
import com.dyre.dartz.model.GameState
import com.dyre.dartz.model.Player
import com.dyre.dartz.util.PolarCoordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    private lateinit var engine: GameEngine
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _landingMarkers = MutableStateFlow<List<Offset>>(emptyList())
    val landingMarkers: StateFlow<List<Offset>> = _landingMarkers.asStateFlow()

    private lateinit var gameMode: GameMode
    private lateinit var playerList: List<Player>

    val isCricket: Boolean get() = ::gameMode.isInitialized && gameMode is GameMode.Cricket

    fun initialize(modeArg: String, playersArg: String) {
        if (_gameState.value != null) return

        gameMode = when (modeArg) {
            "301" -> GameMode.ThreeOhOne
            "501" -> GameMode.FiveOhOne
            "Cricket" -> GameMode.Cricket
            "Killer" -> GameMode.Killer
            else -> GameMode.FiveOhOne
        }

        engine = when (gameMode) {
            is GameMode.ThreeOhOne, is GameMode.FiveOhOne -> StandardGameEngine(gameMode)
            is GameMode.Cricket -> CricketGameEngine()
            is GameMode.Killer -> KillerGameEngine()
        }

        playerList = playersArg.split(",").mapIndexed { index, name ->
            Player(id = index, name = name.trim())
        }

        // Start middling phase
        val middlingState = GameState(
            players = playerList.map { com.dyre.dartz.model.PlayerState(it, 0) },
            currentPlayerIndex = 0,
            currentDartIndex = 0,
            dartsThisRound = emptyList(),
            isGameOver = false,
            winnerId = null,
            message = "${playerList[0].name}: throw at the bull!",
            isMiddling = true,
            middlingResults = emptyMap(),
            middlingPlayerIndex = 0,
        )
        _gameState.value = middlingState
    }

    fun throwMiddlingDart(boardPosition: Offset, boardCenter: Offset, boardRadius: Float) {
        val current = _gameState.value ?: return
        if (!current.isMiddling) return

        val polar = PolarCoordinates.toPolar(boardPosition, boardCenter)
        val distanceFromBull = polar.radius / boardRadius

        val playerIdx = current.middlingPlayerIndex
        val player = current.players[playerIdx]
        val updatedResults = current.middlingResults + (player.player.id to distanceFromBull)

        _landingMarkers.value = _landingMarkers.value + boardPosition

        val nextIdx = playerIdx + 1
        if (nextIdx >= current.players.size) {
            // All players have thrown - determine order
            val sortedPlayers = current.players.sortedBy { updatedResults[it.player.id] ?: Float.MAX_VALUE }
            val resultMessage = sortedPlayers.joinToString("\n") { p ->
                val dist = updatedResults[p.player.id] ?: 0f
                val pct = "%.1f".format(dist * 100)
                "${p.player.name}: ${pct}% from bull"
            } + "\n${sortedPlayers.first().player.name} goes first!"

            _gameState.value = current.copy(
                middlingResults = updatedResults,
                middlingPlayerIndex = nextIdx,
                message = resultMessage,
            )

            // After a brief display, start the real game with reordered players
            val orderedPlayers = sortedPlayers.map { it.player }
            startGame(orderedPlayers)
        } else {
            _gameState.value = current.copy(
                middlingResults = updatedResults,
                middlingPlayerIndex = nextIdx,
                message = "${current.players[nextIdx].player.name}: throw at the bull!",
            )
        }
    }

    private fun startGame(orderedPlayers: List<Player>) {
        _landingMarkers.value = emptyList()
        _gameState.value = engine.createInitialState(GameConfig(gameMode, orderedPlayers))
    }

    fun throwDart(score: DartScore, boardPosition: Offset? = null) {
        val current = _gameState.value ?: return
        if (current.isGameOver || current.currentDartIndex >= 3) return
        _gameState.value = engine.processDart(current, score)
        boardPosition?.let {
            _landingMarkers.value = _landingMarkers.value + it
        }
    }

    fun throwMiss() {
        throwDart(DartScore.MISS)
    }

    fun undo() {
        val current = _gameState.value ?: return
        _gameState.value = engine.undoLastDart(current)
        if (_landingMarkers.value.isNotEmpty()) {
            _landingMarkers.value = _landingMarkers.value.dropLast(1)
        }
    }

    fun endTurn() {
        val current = _gameState.value ?: return
        _gameState.value = engine.endTurn(current)
        _landingMarkers.value = emptyList()
    }
}
