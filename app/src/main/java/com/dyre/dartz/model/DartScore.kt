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

    companion object {
        val MISS = DartScore(0, 0, 0)
    }
}
