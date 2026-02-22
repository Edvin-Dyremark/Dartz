package com.dyre.dartz.game

sealed interface GameMode {
    val displayName: String
    val startScore: Int

    data object ThreeOhOne : GameMode {
        override val displayName = "301"
        override val startScore = 301
    }

    data object FiveOhOne : GameMode {
        override val displayName = "501"
        override val startScore = 501
    }

    data object Cricket : GameMode {
        override val displayName = "Cricket"
        override val startScore = 0
    }

    data object Killer : GameMode {
        override val displayName = "Killer"
        override val startScore = 0
    }
}
