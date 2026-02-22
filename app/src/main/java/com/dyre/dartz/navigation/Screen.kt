package com.dyre.dartz.navigation

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Game : Screen("game/{mode}/{players}") {
        fun createRoute(mode: String, players: String) = "game/$mode/$players"
    }
    data object GameOver : Screen("gameover/{winnerId}/{winnerName}") {
        fun createRoute(winnerId: Int, winnerName: String) = "gameover/$winnerId/$winnerName"
    }
}
