package com.dyre.dartz.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dyre.dartz.ui.game.GameScreen
import com.dyre.dartz.ui.gameover.GameOverScreen
import com.dyre.dartz.ui.setup.SetupScreen

@Composable
fun DartsNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Setup.route) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onStartGame = { mode, players ->
                    navController.navigate(Screen.Game.createRoute(mode, players))
                }
            )
        }
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("players") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "501"
            val players = backStackEntry.arguments?.getString("players") ?: "Player 1,Player 2"
            GameScreen(
                modeArg = mode,
                playersArg = players,
                onGameOver = { winnerId, winnerName, finalScores ->
                    navController.navigate(Screen.GameOver.createRoute(winnerId, winnerName, finalScores, mode)) {
                        popUpTo(Screen.Setup.route) { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = Screen.GameOver.route,
            arguments = listOf(
                navArgument("winnerId") { type = NavType.IntType },
                navArgument("winnerName") { type = NavType.StringType },
                navArgument("finalScores") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val winnerName = backStackEntry.arguments?.getString("winnerName") ?: ""
            val finalScores = backStackEntry.arguments?.getString("finalScores") ?: ""
            val gameMode = backStackEntry.arguments?.getString("mode") ?: ""
            GameOverScreen(
                winnerName = winnerName,
                finalScores = finalScores,
                isKiller = gameMode == "Killer",
                onPlayAgain = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
