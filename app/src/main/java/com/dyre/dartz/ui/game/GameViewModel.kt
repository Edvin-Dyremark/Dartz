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

    // Stack of marker snapshots for undo across turns
    private val markerHistory = mutableListOf<List<Offset>>()

    private lateinit var gameMode: GameMode
    private lateinit var playerList: List<Player>

    val isCricket: Boolean get() = ::gameMode.isInitialized && gameMode is GameMode.Cricket

    val canUndo: Boolean get() = _gameState.value?.previousState != null

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
            val sortedPlayers = current.players.sortedBy { updatedResults[it.player.id] ?: Float.MAX_VALUE }
            val resultMessage = sortedPlayers.joinToString("\n") { p ->
                val dist = updatedResults[p.player.id] ?: 0f
                val pct = "%.1f".format(dist * 100)
                "${p.player.name}: ${pct}% from bull"
            } + "\n\n${sortedPlayers.first().player.name} goes first!"

            _gameState.value = current.copy(
                middlingResults = updatedResults,
                middlingPlayerIndex = nextIdx,
                message = resultMessage,
            )
        } else {
            _gameState.value = current.copy(
                middlingResults = updatedResults,
                middlingPlayerIndex = nextIdx,
                message = "${current.players[nextIdx].player.name}: throw at the bull!",
            )
        }
    }

    fun confirmMiddling() {
        val current = _gameState.value ?: return
        if (!current.isMiddling) return
        val sortedPlayers = current.players.sortedBy { current.middlingResults[it.player.id] ?: Float.MAX_VALUE }
        startGame(sortedPlayers.map { it.player })
    }

    private fun startGame(orderedPlayers: List<Player>) {
        _landingMarkers.value = emptyList()
        markerHistory.clear()
        _gameState.value = engine.createInitialState(GameConfig(gameMode, orderedPlayers))
    }

    fun throwDart(score: DartScore, boardPosition: Offset? = null) {
        val current = _gameState.value ?: return
        if (current.isGameOver || current.currentDartIndex >= 3) return

        // Save marker state before this dart for undo
        markerHistory.add(_landingMarkers.value)

        val newState = engine.processDart(current, score)
        _gameState.value = newState
        boardPosition?.let {
            _landingMarkers.value = _landingMarkers.value + it
        }
        // Auto-advance after 3 darts
        if (!newState.isGameOver && newState.currentDartIndex >= 3) {
            endTurn()
        }
    }

    fun throwMiss() {
        throwDart(DartScore.MISS)
    }

    fun undo() {
        val current = _gameState.value ?: return
        val prev = current.previousState ?: return
        _gameState.value = prev
        // Restore markers from history
        if (markerHistory.isNotEmpty()) {
            _landingMarkers.value = markerHistory.removeAt(markerHistory.lastIndex)
        }
    }

    fun endTurn() {
        val current = _gameState.value ?: return
        // Save marker state before end turn for undo
        markerHistory.add(_landingMarkers.value)
        _gameState.value = engine.endTurn(current)
        _landingMarkers.value = emptyList()
    }
}
