package com.dyre.dartz.ui.setup

import androidx.lifecycle.ViewModel
import com.dyre.dartz.game.GameMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SetupViewModel : ViewModel() {

    private val _selectedMode = MutableStateFlow<GameMode>(GameMode.FiveOhOne)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _playerNames = MutableStateFlow(listOf("Player 1", "Player 2"))
    val playerNames: StateFlow<List<String>> = _playerNames.asStateFlow()

    fun selectMode(mode: GameMode) {
        _selectedMode.value = mode
    }

    fun setPlayerCount(count: Int) {
        val clamped = count.coerceIn(2, 8)
        val current = _playerNames.value
        _playerNames.value = List(clamped) { i ->
            current.getOrElse(i) { "Player ${i + 1}" }
        }
    }

    fun setPlayerName(index: Int, name: String) {
        _playerNames.value = _playerNames.value.toMutableList().apply {
            if (index in indices) this[index] = name
        }
    }
}
