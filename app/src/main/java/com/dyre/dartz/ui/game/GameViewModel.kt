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

    fun selectFirstPlayer(playerId: Int) {
        val current = _gameState.value ?: return
        if (!current.isMiddling) return

        val winner = playerList.first { it.id == playerId }
        val others = playerList.filter { it.id != playerId }
        startGame(listOf(winner) + others)
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
