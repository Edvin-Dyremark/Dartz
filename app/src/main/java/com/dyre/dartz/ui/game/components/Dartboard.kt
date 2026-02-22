package com.dyre.dartz.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.dyre.dartz.model.DartScore
import com.dyre.dartz.util.DartboardGeometry
import com.dyre.dartz.util.PolarCoordinates
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Dartboard(
    onDartThrown: (DartScore) -> Unit,
    landingMarkers: List<Offset> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var magnifierPosition by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val size = constraints.maxWidth.toFloat()
        val center = Offset(size / 2f, size / 2f)
        val boardRadius = size / 2f * 0.95f // leave margin for numbers

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        if (!isDragging) {
                            val score = PolarCoordinates.resolve(tapOffset, center, boardRadius)
                            onDartThrown(score)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            isDragging = true
                            magnifierPosition = offset
                        },
                        onDrag = { change, _ ->
                            magnifierPosition = change.position
                        },
                        onDragEnd = {
                            magnifierPosition?.let { pos ->
                                val score = PolarCoordinates.resolve(pos, center, boardRadius)
                                onDartThrown(score)
                            }
                            magnifierPosition = null
                            isDragging = false
                        },
                        onDragCancel = {
                            magnifierPosition = null
                            isDragging = false
                        },
                    )
                }
        ) {
            drawDartboard(center, boardRadius)
            drawNumberLabels(center, boardRadius)

            // Draw landing markers
            landingMarkers.forEach { markerPos ->
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = markerPos,
                    style = Fill,
                )
                drawCircle(
                    color = Color.Black,
                    radius = 6f,
                    center = markerPos,
                    style = Stroke(width = 2f),
                )
            }

            // Draw magnifier
            magnifierPosition?.let { pos ->
                drawMagnifier(pos, center, boardRadius)
            }
        }
    }
}

private fun DrawScope.drawDartboard(center: Offset, boardRadius: Float) {
    val rings = listOf(
        DartboardGeometry.DOUBLE_OUTER to DartboardGeometry.Ring.DOUBLE,
        DartboardGeometry.OUTER_SINGLE_OUTER to DartboardGeometry.Ring.OUTER_SINGLE,
        DartboardGeometry.TRIPLE_OUTER to DartboardGeometry.Ring.TRIPLE,
        DartboardGeometry.INNER_SINGLE_OUTER to DartboardGeometry.Ring.INNER_SINGLE,
    )

    // Draw from outside in
    for (segIdx in 0 until 20) {
        val startAngle = DartboardGeometry.START_ANGLE_OFFSET + segIdx * DartboardGeometry.SEGMENT_ANGLE
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
    val labelRadius = boardRadius * 0.98f
    val textPaint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = boardRadius * 0.07f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    for (segIdx in 0 until 20) {
        val number = DartboardGeometry.SEGMENT_ORDER[segIdx]
        val angleDeg = segIdx * DartboardGeometry.SEGMENT_ANGLE
        val angleRad = Math.toRadians((angleDeg - 90f).toDouble())
        val x = center.x + labelRadius * cos(angleRad).toFloat()
        val y = center.y + labelRadius * sin(angleRad).toFloat()

        drawContext.canvas.nativeCanvas.drawText(
            number.toString(),
            x,
            y + textPaint.textSize / 3f,
            textPaint,
        )
    }
}

private fun DrawScope.drawMagnifier(
    position: Offset,
    boardCenter: Offset,
    boardRadius: Float,
) {
    val magnifierRadius = boardRadius * 0.3f
    val magnifierCenter = Offset(position.x, position.y - magnifierRadius * 2.5f)
    val zoom = 2.5f

    drawContext.canvas.nativeCanvas.save()
    val clipPath = android.graphics.Path()
    clipPath.addCircle(magnifierCenter.x, magnifierCenter.y, magnifierRadius, android.graphics.Path.Direction.CW)
    drawContext.canvas.nativeCanvas.clipPath(clipPath)

    // Draw zoomed dartboard centered on the touch position
    drawContext.canvas.nativeCanvas.translate(
        magnifierCenter.x - position.x * zoom,
        magnifierCenter.y - position.y * zoom,
    )
    drawContext.canvas.nativeCanvas.scale(zoom, zoom)

    // Redraw the board inside the magnifier
    drawDartboard(boardCenter, boardRadius)

    // Draw crosshair at touch position
    drawContext.canvas.nativeCanvas.restore()

    // Magnifier border
    drawCircle(
        color = Color.White,
        radius = magnifierRadius,
        center = magnifierCenter,
        style = Stroke(width = 3f),
    )

    // Crosshair in magnifier
    val crossSize = 10f
    drawLine(Color.White, Offset(magnifierCenter.x - crossSize, magnifierCenter.y), Offset(magnifierCenter.x + crossSize, magnifierCenter.y), strokeWidth = 2f)
    drawLine(Color.White, Offset(magnifierCenter.x, magnifierCenter.y - crossSize), Offset(magnifierCenter.x, magnifierCenter.y + crossSize), strokeWidth = 2f)
}
