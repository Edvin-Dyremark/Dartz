package com.dyre.dartz.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
    deadNumbers: Set<Int> = emptySet(),
) {
    var magnifierPosition by remember { mutableStateOf<Offset?>(null) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // Box fills all available space — touch works everywhere including above/below board
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            if (down.type != PointerEventType.Press) continue
                            val downPos = down.changes.first().position

                            // Canvas is centered in Box — compute its Y offset
                            val boardS = min(boardSize.width, boardSize.height).toFloat()
                            val canvasTopY = (boxSize.height - boardS) / 2f
                            val toCanvas = { pos: Offset ->
                                Offset(
                                    pos.x.coerceIn(0f, boardS),
                                    (pos.y - canvasTopY).coerceIn(0f, boardS),
                                )
                            }

                            magnifierPosition = toCanvas(downPos)
                            down.changes.forEach { it.consume() }

                            var lastPos = toCanvas(downPos)

                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Move -> {
                                        lastPos = toCanvas(event.changes.first().position)
                                        magnifierPosition = lastPos
                                        event.changes.forEach { it.consume() }
                                    }
                                    PointerEventType.Release -> {
                                        event.changes.forEach { it.consume() }
                                        break
                                    }
                                    else -> break
                                }
                            }

                            magnifierPosition = null
                            val center = Offset(boardS / 2f, boardS / 2f)
                            val boardRadius = boardS / 2f
                            val score = PolarCoordinates.resolve(lastPos, center, boardRadius)
                            onDartThrown(score, lastPos)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onSizeChanged { boardSize = it }
        ) {
            val s = min(size.width, size.height)
            val center = Offset(s / 2f, s / 2f)
            val boardRadius = s / 2f

            drawDartboard(center, boardRadius, isCricket, deadNumbers)
            drawNumberLabels(center, boardRadius)

            landingMarkers.forEach { markerPos ->
                drawLandingDot(markerPos)
            }

            magnifierPosition?.let { pos ->
                drawMagnifier(pos, center, boardRadius, isCricket, deadNumbers)
            }
        }
    }
}

private fun DrawScope.drawLandingDot(position: Offset) {
    drawCircle(
        color = Color.White,
        radius = 18f,
        center = position,
        style = Fill,
    )
    drawCircle(
        color = Color.Black,
        radius = 18f,
        center = position,
        style = Stroke(width = 4f),
    )
}

private val DARKEN_OVERLAY = Color(0xAA000000)

private fun DrawScope.drawDartboard(
    center: Offset,
    boardRadius: Float,
    isCricket: Boolean,
    deadNumbers: Set<Int> = emptySet(),
) {
    val rings = listOf(
        DartboardGeometry.DOUBLE_OUTER to DartboardGeometry.Ring.DOUBLE,
        DartboardGeometry.OUTER_SINGLE_OUTER to DartboardGeometry.Ring.OUTER_SINGLE,
        DartboardGeometry.TRIPLE_OUTER to DartboardGeometry.Ring.TRIPLE,
        DartboardGeometry.INNER_SINGLE_OUTER to DartboardGeometry.Ring.INNER_SINGLE,
    )

    for (segIdx in 0 until 20) {
        val segment = DartboardGeometry.SEGMENT_ORDER[segIdx]
        val startAngle = DartboardGeometry.START_ANGLE_OFFSET + segIdx * DartboardGeometry.SEGMENT_ANGLE
        val isDimmed = isCricket && (segment !in CRICKET_NUMBERS || segment in deadNumbers)
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

    val bullDimmed = isCricket && 25 in deadNumbers

    drawCircle(
        color = DartboardGeometry.segmentColor(0, DartboardGeometry.Ring.OUTER_BULL),
        radius = DartboardGeometry.OUTER_BULL_OUTER * boardRadius,
        center = center,
    )
    drawCircle(
        color = DartboardGeometry.segmentColor(0, DartboardGeometry.Ring.BULLSEYE),
        radius = DartboardGeometry.BULLSEYE_OUTER * boardRadius,
        center = center,
    )

    if (bullDimmed) {
        drawCircle(
            color = DARKEN_OVERLAY,
            radius = DartboardGeometry.OUTER_BULL_OUTER * boardRadius,
            center = center,
        )
    }

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

    for (i in 0..steps) {
        val angle = Math.toRadians((startAngleDeg + sweepAngleDeg * i / steps - 90f).toDouble())
        val x = center.x + outerRadius * cos(angle).toFloat()
        val y = center.y + outerRadius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

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
        textSize = boardRadius * 0.075f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
        setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
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
    deadNumbers: Set<Int> = emptySet(),
) {
    val magnifierRadius = boardRadius * 0.4f
    val magnifierCenter = Offset(position.x, position.y - magnifierRadius * 2.5f)
    val zoom = 1.5f
    val borderWidth = 8f
    val outlineWidth = 6f

    val score = PolarCoordinates.resolve(position, boardCenter, boardRadius)
    val labelText = score.displayName
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = boardRadius * 0.065f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val tabHeight = labelPaint.textSize + 32f
    val tabWidth = magnifierRadius * 1.8f

    // Tab ABOVE magnifier for score label (gap between tab and circle)
    val tabBottom = magnifierCenter.y - magnifierRadius - outlineWidth - 4f
    val tabTop = tabBottom - tabHeight
    val tabPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = magnifierCenter.x - tabWidth / 2f,
                top = tabTop,
                right = magnifierCenter.x + tabWidth / 2f,
                bottom = tabBottom,
                radiusX = 12f,
                radiusY = 12f,
            )
        )
    }
    drawPath(tabPath, Color(0xDD000000))
    drawPath(tabPath, Color.White, style = Stroke(width = borderWidth))

    // Zoomed board clipped to circle
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

        drawDartboard(boardCenter, boardRadius, isCricket, deadNumbers)

        canvas.nativeCanvas.restore()
    }

    // Dark outline behind white border for contrast
    drawCircle(
        color = Color.Black,
        radius = magnifierRadius + borderWidth / 2f + outlineWidth / 2f,
        center = magnifierCenter,
        style = Stroke(width = outlineWidth),
    )

    // White circle border
    drawCircle(
        color = Color.White,
        radius = magnifierRadius,
        center = magnifierCenter,
        style = Stroke(width = borderWidth),
    )

    // Inner dark outline for extra contrast
    drawCircle(
        color = Color.Black,
        radius = magnifierRadius - borderWidth / 2f - outlineWidth / 2f,
        center = magnifierCenter,
        style = Stroke(width = outlineWidth),
    )

    // Larger aiming dot in magnifier center
    drawCircle(
        color = Color.White,
        radius = 14f,
        center = magnifierCenter,
        style = Fill,
    )
    drawCircle(
        color = Color.Black,
        radius = 14f,
        center = magnifierCenter,
        style = Stroke(width = 3f),
    )

    // Score text inside tab above
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            labelText,
            magnifierCenter.x,
            tabTop + tabHeight / 2f + labelPaint.textSize / 3f,
            labelPaint,
        )
    }
}
