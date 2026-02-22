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

    fun initialize(modeArg: String, playersArg: String) {
        if (_gameState.value != null) return

        val mode = when (modeArg) {
            "301" -> GameMode.ThreeOhOne
            "501" -> GameMode.FiveOhOne
            "Cricket" -> GameMode.Cricket
            "Killer" -> GameMode.Killer
            else -> GameMode.FiveOhOne
        }

        engine = when (mode) {
            is GameMode.ThreeOhOne, is GameMode.FiveOhOne -> StandardGameEngine(mode)
            is GameMode.Cricket -> CricketGameEngine()
            is GameMode.Killer -> KillerGameEngine()
        }

        val players = playersArg.split(",").mapIndexed { index, name ->
            Player(id = index, name = name.trim())
        }

        _gameState.value = engine.createInitialState(GameConfig(mode, players))
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
