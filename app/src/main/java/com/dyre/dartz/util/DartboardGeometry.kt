package com.dyre.dartz.util

import androidx.compose.ui.graphics.Color
import com.dyre.dartz.ui.theme.BullseyeRed
import com.dyre.dartz.ui.theme.DartboardBlack
import com.dyre.dartz.ui.theme.DartboardCream
import com.dyre.dartz.ui.theme.DartboardGreen
import com.dyre.dartz.ui.theme.DartboardRed
import com.dyre.dartz.ui.theme.OuterBullGreen

object DartboardGeometry {

    // Segment order clockwise from top (12 o'clock position)
    val SEGMENT_ORDER = intArrayOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)

    // Ring radius fractions (of total board radius)
    const val BULLSEYE_OUTER = 0.07f
    const val OUTER_BULL_OUTER = 0.15f
    const val INNER_SINGLE_OUTER = 0.46f
    const val TRIPLE_OUTER = 0.58f
    const val OUTER_SINGLE_OUTER = 0.82f
    const val DOUBLE_OUTER = 0.95f

    // Each segment spans 18 degrees (360 / 20)
    const val SEGMENT_ANGLE = 18f

    // The first segment (20) is centered at 0° (top), so it starts at -9°
    const val START_ANGLE_OFFSET = -9f

    enum class Ring(val multiplier: Int) {
        BULLSEYE(2),       // Double bull = 50
        OUTER_BULL(1),     // Single bull = 25
        INNER_SINGLE(1),
        TRIPLE(3),
        OUTER_SINGLE(1),
        DOUBLE(2),
        OUTSIDE(0)
    }

    fun ringForRadius(normalizedRadius: Float): Ring = when {
        normalizedRadius <= BULLSEYE_OUTER -> Ring.BULLSEYE
        normalizedRadius <= OUTER_BULL_OUTER -> Ring.OUTER_BULL
        normalizedRadius <= INNER_SINGLE_OUTER -> Ring.INNER_SINGLE
        normalizedRadius <= TRIPLE_OUTER -> Ring.TRIPLE
        normalizedRadius <= OUTER_SINGLE_OUTER -> Ring.OUTER_SINGLE
        normalizedRadius <= DOUBLE_OUTER -> Ring.DOUBLE
        else -> Ring.OUTSIDE
    }

    fun segmentColor(segmentIndex: Int, ring: Ring): Color {
        val even = segmentIndex % 2 == 0
        return when (ring) {
            Ring.BULLSEYE -> BullseyeRed
            Ring.OUTER_BULL -> OuterBullGreen
            Ring.DOUBLE, Ring.TRIPLE -> if (even) DartboardRed else DartboardGreen
            Ring.INNER_SINGLE, Ring.OUTER_SINGLE -> if (even) DartboardBlack else DartboardCream
            Ring.OUTSIDE -> Color.Transparent
        }
    }

    fun numberTextColor(segmentIndex: Int): Color {
        val even = segmentIndex % 2 == 0
        return if (even) DartboardRed else DartboardGreen
    }
}
