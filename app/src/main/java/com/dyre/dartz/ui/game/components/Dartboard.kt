package com.dyre.dartz.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.dyre.dartz.model.DartScore
import com.dyre.dartz.util.DartboardGeometry
import com.dyre.dartz.util.PolarCoordinates
import kotlinx.coroutines.coroutineScope
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val CRICKET_NUMBERS = setOf(15, 16, 17, 18, 19, 20, 25)

@Composable
fun Dartboard(
    onDartThrown: (DartScore, Offset) -> Unit,
    modifier: Modifier = Modifier,
    landingMarkers: List<Offset> = emptyList(),
    isCricket: Boolean = false,
) {
    var magnifierPosition by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for finger down
                            val down = awaitPointerEvent()
                            if (down.type != PointerEventType.Press) continue
                            val downPos = down.changes.first().position

                            // Show magnifier immediately
                            magnifierPosition = downPos
                            down.changes.forEach { it.consume() }

                            var lastPos = downPos
                            var moved = false

                            // Track movement until release
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Move -> {
                                        lastPos = event.changes.first().position
                                        magnifierPosition = lastPos
                                        moved = true
                                        event.changes.forEach { it.consume() }
                                    }
                                    PointerEventType.Release -> {
                                        event.changes.forEach { it.consume() }
                                        break
                                    }
                                    else -> break
                                }
                            }

                            // Commit the dart at the final position
                            magnifierPosition = null
                            val s = min(size.width, size.height).toFloat()
                            val center = Offset(s / 2f, s / 2f)
                            val boardRadius = s / 2f
                            val score = PolarCoordinates.resolve(lastPos, center, boardRadius)
                            onDartThrown(score, lastPos)
                        }
                    }
                }
            }
    ) {
        val s = min(size.width, size.height)
        val center = Offset(s / 2f, s / 2f)
        val boardRadius = s / 2f

        drawDartboard(center, boardRadius, isCricket)
        drawNumberLabels(center, boardRadius)

        // Draw landing markers
        landingMarkers.forEach { markerPos ->
            drawLandingDot(markerPos)
        }

        // Draw magnifier
        magnifierPosition?.let { pos ->
            drawMagnifier(pos, center, boardRadius, isCricket)
        }
    }
}

private fun DrawScope.drawLandingDot(position: Offset) {
    drawCircle(
        color = Color.White,
        radius = 6f,
        center = position,
        style = Fill,
    )
    drawCircle(
        color = Color.Black,
        radius = 6f,
        center = position,
        style = Stroke(width = 2f),
    )
}

private val DARKEN_OVERLAY = Color(0xAA000000)

private fun DrawScope.drawDartboard(center: Offset, boardRadius: Float, isCricket: Boolean) {
    val rings = listOf(
        DartboardGeometry.DOUBLE_OUTER to DartboardGeometry.Ring.DOUBLE,
        DartboardGeometry.OUTER_SINGLE_OUTER to DartboardGeometry.Ring.OUTER_SINGLE,
        DartboardGeometry.TRIPLE_OUTER to DartboardGeometry.Ring.TRIPLE,
        DartboardGeometry.INNER_SINGLE_OUTER to DartboardGeometry.Ring.INNER_SINGLE,
    )

    // Draw from outside in
    for (segIdx in 0 until 20) {
        val segment = DartboardGeometry.SEGMENT_ORDER[segIdx]
        val startAngle = DartboardGeometry.START_ANGLE_OFFSET + segIdx * DartboardGeometry.SEGMENT_ANGLE
        val isDimmed = isCricket && segment !in CRICKET_NUMBERS
        for ((outerFrac, ring) in rings) {
            val innerFrac = when (ring) {
                DartboardGeometry.Ring.DOUBLE -> DartboardGeometry.OUTER_SINGLE_OUTER
                DartboardGeometry.Ring.OUTER_SINGLE -> DartboardGeometry.TRIPLE_OUTER
                DartboardGeometry.Ring.TRIPLE -> DartboardGeometry.INNER_SINGLE_OUTER
                DartboardGeometry.Ring.INNER_SINGLE -> DartboardGeometry.OUTER_BULL_OUTER
                else -> 0f
            }
            drawAnnularSector(
                center = center,
                innerRadius = innerFrac * boardRadius,
                outerRadius = outerFrac * boardRadius,
                startAngleDeg = startAngle,
                sweepAngleDeg = DartboardGeometry.SEGMENT_ANGLE,
                color = DartboardGeometry.segmentColor(segIdx, ring),
            )
            if (isDimmed) {
                drawAnnularSector(
                    center = center,
                    innerRadius = innerFrac * boardRadius,
                    outerRadius = outerFrac * boardRadius,
                    startAngleDeg = startAngle,
                    sweepAngleDeg = DartboardGeometry.SEGMENT_ANGLE,
                    color = DARKEN_OVERLAY,
                )
            }
        }
    }

    // Outer bull
    drawCircle(
        color = DartboardGeometry.segmentColor(0, DartboardGeometry.Ring.OUTER_BULL),
        radius = DartboardGeometry.OUTER_BULL_OUTER * boardRadius,
        center = center,
    )
    // Bull's eye
    drawCircle(
        color = DartboardGeometry.segmentColor(0, DartboardGeometry.Ring.BULLSEYE),
        radius = DartboardGeometry.BULLSEYE_OUTER * boardRadius,
        center = center,
    )

    // Wire rings
    val wireColor = Color(0xFF888888)
    val wireWidth = 1.5f
    listOf(
        DartboardGeometry.BULLSEYE_OUTER,
        DartboardGeometry.OUTER_BULL_OUTER,
        DartboardGeometry.INNER_SINGLE_OUTER,
        DartboardGeometry.TRIPLE_OUTER,
        DartboardGeometry.OUTER_SINGLE_OUTER,
        DartboardGeometry.DOUBLE_OUTER,
    ).forEach { frac ->
        drawCircle(
            color = wireColor,
            radius = frac * boardRadius,
            center = center,
            style = Stroke(width = wireWidth),
        )
    }

    // Wire segment lines
    for (segIdx in 0 until 20) {
        val angleDeg = DartboardGeometry.START_ANGLE_OFFSET + segIdx * DartboardGeometry.SEGMENT_ANGLE
        val angleRad = Math.toRadians(angleDeg.toDouble() - 90.0)
        val innerR = DartboardGeometry.OUTER_BULL_OUTER * boardRadius
        val outerR = DartboardGeometry.DOUBLE_OUTER * boardRadius
        drawLine(
            color = wireColor,
            start = Offset(
                center.x + (innerR * cos(angleRad)).toFloat(),
                center.y + (innerR * sin(angleRad)).toFloat(),
            ),
            end = Offset(
                center.x + (outerR * cos(angleRad)).toFloat(),
                center.y + (outerR * sin(angleRad)).toFloat(),
            ),
            strokeWidth = wireWidth,
        )
    }
}

private fun DrawScope.drawAnnularSector(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float,
    color: Color,
) {
    val steps = 32
    val path = Path()

    // Outer arc (clockwise)
    for (i in 0..steps) {
        val angle = Math.toRadians((startAngleDeg + sweepAngleDeg * i / steps - 90f).toDouble())
        val x = center.x + outerRadius * cos(angle).toFloat()
        val y = center.y + outerRadius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    // Inner arc (counter-clockwise)
    for (i in steps downTo 0) {
        val angle = Math.toRadians((startAngleDeg + sweepAngleDeg * i / steps - 90f).toDouble())
        val x = center.x + innerRadius * cos(angle).toFloat()
        val y = center.y + innerRadius * sin(angle).toFloat()
        path.lineTo(x, y)
    }

    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawNumberLabels(center: Offset, boardRadius: Float) {
    val labelRadius = boardRadius * ((DartboardGeometry.OUTER_SINGLE_OUTER + DartboardGeometry.DOUBLE_OUTER) / 2f)
    val textPaint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = boardRadius * 0.055f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    drawIntoCanvas { canvas ->
        for (segIdx in 0 until 20) {
            val number = DartboardGeometry.SEGMENT_ORDER[segIdx]
            val angleDeg = segIdx * DartboardGeometry.SEGMENT_ANGLE
            val angleRad = Math.toRadians((angleDeg - 90f).toDouble())
            val x = center.x + labelRadius * cos(angleRad).toFloat()
            val y = center.y + labelRadius * sin(angleRad).toFloat()

            canvas.nativeCanvas.drawText(
                number.toString(),
                x,
                y + textPaint.textSize / 3f,
                textPaint,
            )
        }
    }
}

private fun DrawScope.drawMagnifier(
    position: Offset,
    boardCenter: Offset,
    boardRadius: Float,
    isCricket: Boolean,
) {
    val magnifierRadius = boardRadius * 0.4f
    val magnifierCenter = Offset(position.x, position.y - magnifierRadius * 2.5f)
    val zoom = 2.0f

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.save()
        val clipPath = android.graphics.Path()
        clipPath.addCircle(magnifierCenter.x, magnifierCenter.y, magnifierRadius, android.graphics.Path.Direction.CW)
        canvas.nativeCanvas.clipPath(clipPath)

        canvas.nativeCanvas.translate(
            magnifierCenter.x - position.x * zoom,
            magnifierCenter.y - position.y * zoom,
        )
        canvas.nativeCanvas.scale(zoom, zoom)

        drawDartboard(boardCenter, boardRadius, isCricket)

        canvas.nativeCanvas.restore()
    }

    // Magnifier border
    drawCircle(
        color = Color.White,
        radius = magnifierRadius,
        center = magnifierCenter,
        style = Stroke(width = 3f),
    )

    // Aiming dot in magnifier center (same style as landing markers)
    drawLandingDot(magnifierCenter)

    // Score label below magnifier
    val score = PolarCoordinates.resolve(position, boardCenter, boardRadius)
    val labelText = score.displayName
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = boardRadius * 0.06f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    val labelY = magnifierCenter.y + magnifierRadius + labelPaint.textSize + 8f
    val textWidth = labelPaint.measureText(labelText)
    val pillPadH = 16f
    val pillPadV = 6f
    val bgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(180, 0, 0, 0)
        isAntiAlias = true
    }

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRoundRect(
            magnifierCenter.x - textWidth / 2f - pillPadH,
            labelY - labelPaint.textSize - pillPadV,
            magnifierCenter.x + textWidth / 2f + pillPadH,
            labelY + pillPadV,
            16f, 16f,
            bgPaint,
        )
        canvas.nativeCanvas.drawText(
            labelText,
            magnifierCenter.x,
            labelY,
            labelPaint,
        )
    }
}
