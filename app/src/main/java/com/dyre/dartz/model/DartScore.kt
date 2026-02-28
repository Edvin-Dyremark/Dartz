package com.dyre.dartz.model

data class DartScore(
    val segment: Int,
    val multiplier: Int,
    val points: Int,
) {
    val displayName: String
        get() = when {
            segment == 0 -> "Miss"
            segment == 25 && multiplier == 2 -> "Bull's Eye"
            segment == 25 && multiplier == 1 -> "Outer Bull"
            multiplier == 3 -> "Triple $segment"
            multiplier == 2 -> "Double $segment"
            else -> "Single $segment"
        }

    val isDouble: Boolean get() = multiplier == 2

    val shortName: String
        get() = when {
            segment == 0 -> "Miss"
            segment == 25 && multiplier == 2 -> "D-Bull"
            segment == 25 && multiplier == 1 -> "Bull"
            multiplier == 3 -> "T$segment"
            multiplier == 2 -> "D$segment"
            else -> "$segment"
        }

    companion object {
        val MISS = DartScore(0, 0, 0)
    }
}
