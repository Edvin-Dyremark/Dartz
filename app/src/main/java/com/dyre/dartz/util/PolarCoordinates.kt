package com.dyre.dartz.util

import androidx.compose.ui.geometry.Offset
import com.dyre.dartz.model.DartScore
import kotlin.math.atan2
import kotlin.math.sqrt

object PolarCoordinates {

    data class Polar(val radius: Float, val angleDegrees: Float)

    fun toPolar(tap: Offset, center: Offset): Polar {
        val dx = tap.x - center.x
        val dy = tap.y - center.y
        val radius = sqrt(dx * dx + dy * dy)
        // atan2 gives angle from positive X axis; we want angle from top (negative Y)
        // so rotate by -90°. Result in [0, 360)
        var angle = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat()
        if (angle < 0f) angle += 360f
        return Polar(radius, angle)
    }

    fun resolve(tap: Offset, center: Offset, boardRadius: Float): DartScore {
        val polar = toPolar(tap, center)
        val normalizedRadius = polar.radius / boardRadius

        val ring = DartboardGeometry.ringForRadius(normalizedRadius)

        if (ring == DartboardGeometry.Ring.OUTSIDE) {
            return DartScore(0, 0, 0)
        }
        if (ring == DartboardGeometry.Ring.BULLSEYE) {
            return DartScore(25, 2, 50)
        }
        if (ring == DartboardGeometry.Ring.OUTER_BULL) {
            return DartScore(25, 1, 25)
        }

        // Determine which segment the angle falls into
        val segmentIndex = segmentIndexForAngle(polar.angleDegrees)
        val segment = DartboardGeometry.SEGMENT_ORDER[segmentIndex]
        val multiplier = ring.multiplier
        return DartScore(segment, multiplier, segment * multiplier)
    }

    fun segmentIndexForAngle(angleDegrees: Float): Int {
        // Each segment is 18°. Segment 20 is centered at 0° (top).
        // Offset by half a segment so segment boundaries are at 9°, 27°, etc.
        var adjusted = angleDegrees - DartboardGeometry.START_ANGLE_OFFSET
        if (adjusted < 0f) adjusted += 360f
        val index = (adjusted / DartboardGeometry.SEGMENT_ANGLE).toInt() % 20
        return index
    }
}
