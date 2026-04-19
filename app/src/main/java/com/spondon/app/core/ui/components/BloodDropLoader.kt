package com.spondon.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.SoftRose
import com.spondon.app.core.ui.theme.UrgencyCritical
import kotlin.math.sin

// ──────────────────────────────────────────────────────────────
// Blood drop path helper (shared by all variants)
// ──────────────────────────────────────────────────────────────

private fun buildBloodDropPath(cx: Float, cy: Float, width: Float, height: Float): Path {
    return Path().apply {
        moveTo(cx, cy - height)
        cubicTo(
            cx - width * 0.15f, cy - height * 0.65f,
            cx - width, cy - height * 0.1f,
            cx - width, cy + height * 0.15f,
        )
        cubicTo(
            cx - width, cy + height * 0.65f,
            cx - width * 0.55f, cy + height,
            cx, cy + height,
        )
        cubicTo(
            cx + width * 0.55f, cy + height,
            cx + width, cy + height * 0.65f,
            cx + width, cy + height * 0.15f,
        )
        cubicTo(
            cx + width, cy - height * 0.1f,
            cx + width * 0.15f, cy - height * 0.65f,
            cx, cy - height,
        )
        close()
    }
}

private fun DrawScope.drawBloodDrop(
    cx: Float, cy: Float, width: Float, height: Float, brush: Brush,
) {
    drawPath(path = buildBloodDropPath(cx, cy, width, height), brush = brush, style = Fill)
}

private fun DrawScope.drawBloodDropOutline(
    cx: Float, cy: Float, width: Float, height: Float,
    color: Color, strokeWidth: Float = 3f,
) {
    drawPath(
        path = buildBloodDropPath(cx, cy, width, height),
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

// ──────────────────────────────────────────────────────────────
// 1) Standard pulsing blood drop (logo / idle state)
// ──────────────────────────────────────────────────────────────

@Composable
fun BloodDropLoader(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    color: Color = BloodRed,
    accentColor: Color = SoftRose,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blood_drop")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ), label = "scale",
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "glow",
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "shimmer",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width; val h = this.size.height
            val cx = w / 2f; val cy = h / 2f

            drawCircle(
                color = color.copy(alpha = glowAlpha),
                radius = w * 0.48f * scale,
                center = Offset(cx, cy + h * 0.05f),
            )

            drawBloodDrop(
                cx = cx, cy = cy,
                width = w * 0.42f * scale, height = h * 0.52f * scale,
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor, color, color.copy(alpha = 0.85f)),
                    startY = cy - h * 0.25f, endY = cy + h * 0.28f,
                ),
            )

            val shimmerY = cy - h * 0.15f + shimmerOffset * h * 0.3f
            drawCircle(Color.White.copy(alpha = 0.3f), w * 0.06f * scale, Offset(cx - w * 0.06f, shimmerY))
            drawCircle(Color.White.copy(alpha = 0.15f), w * 0.035f * scale, Offset(cx - w * 0.12f, shimmerY + h * 0.04f))
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 2) Blood-filling loader (for auth loading state)
//    The drop outline is drawn, then filled from bottom to top
//    with a wave surface that oscillates.
// ──────────────────────────────────────────────────────────────

@Composable
fun BloodDropFillingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    color: Color = BloodRed,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fill_loader")

    val fillLevel by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "fill",
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "wave",
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.98f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "pulse",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width; val h = this.size.height
            val cx = w / 2f; val cy = h / 2f
            val dropW = w * 0.42f * pulse
            val dropH = h * 0.52f * pulse

            // Outer glow
            drawCircle(
                color = color.copy(alpha = 0.08f),
                radius = w * 0.48f,
                center = Offset(cx, cy + h * 0.03f),
            )

            // Draw the outline
            drawBloodDropOutline(cx, cy, dropW, dropH, color.copy(alpha = 0.35f), 2.5f)

            // Clip to the drop shape and draw fill + wave
            val dropPath = buildBloodDropPath(cx, cy, dropW, dropH)
            clipPath(dropPath) {
                // The fill starts from the bottom of the drop
                val dropTop = cy - dropH
                val dropBottom = cy + dropH
                val totalHeight = dropBottom - dropTop
                val fillTop = dropBottom - (totalHeight * fillLevel)
                val waveAmp = h * 0.012f

                // Draw wave-topped fill rectangle
                val fillPath = Path().apply {
                    // Start from bottom-left
                    moveTo(0f, dropBottom + 10f)
                    lineTo(0f, fillTop)
                    // Wave surface
                    val steps = 50
                    for (i in 0..steps) {
                        val frac = i.toFloat() / steps
                        val x = frac * w
                        val y = fillTop + sin((wavePhase + frac * 4 * Math.PI).toFloat()) * waveAmp
                        lineTo(x, y)
                    }
                    lineTo(w, dropBottom + 10f)
                    close()
                }

                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.7f),
                            color,
                            color.copy(alpha = 0.95f),
                        ),
                        startY = fillTop,
                        endY = dropBottom,
                    ),
                )
            }

            // Highlight
            drawCircle(Color.White.copy(alpha = 0.2f), w * 0.04f, Offset(cx - w * 0.08f, cy - h * 0.1f))
            drawCircle(Color.White.copy(alpha = 0.1f), w * 0.025f, Offset(cx - w * 0.13f, cy - h * 0.06f))
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 3) Heartbeat blood drop (for success state)
// ──────────────────────────────────────────────────────────────

@Composable
fun BloodDropHeartbeat(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    color: Color = AvailableGreen,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")

    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.0f at 0 using LinearEasing
                1.15f at 100 using EaseOutCubic
                0.95f at 200 using EaseInCubic
                1.12f at 350 using EaseOutCubic
                1.0f at 500 using EaseInOutSine
                1.0f at 1200 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ), label = "heart_scale",
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ), label = "ring",
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ), label = "ring_scale",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width; val h = this.size.height
            val cx = w / 2f; val cy = h / 2f

            // Expanding ring
            drawCircle(
                color = color.copy(alpha = ringAlpha),
                radius = w * 0.35f * ringScale,
                center = Offset(cx, cy + h * 0.03f),
                style = Stroke(width = 2f),
            )

            // Glow
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = w * 0.45f * heartScale,
                center = Offset(cx, cy + h * 0.03f),
            )

            // Drop
            drawBloodDrop(
                cx, cy,
                w * 0.4f * heartScale, h * 0.5f * heartScale,
                Brush.verticalGradient(listOf(color.copy(alpha = 0.8f), color, color.copy(alpha = 0.9f))),
            )

            // Checkmark
            val s = w * 0.1f * heartScale
            val checkCy = cy + h * 0.06f
            val checkPath = Path().apply {
                moveTo(cx - s, checkCy)
                lineTo(cx - s * 0.3f, checkCy + s * 0.8f)
                lineTo(cx + s, checkCy - s * 0.5f)
            }
            drawPath(checkPath, Color.White, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 4) Error blood drop (shakes with X mark)
// ──────────────────────────────────────────────────────────────

@Composable
fun BloodDropError(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    color: Color = UrgencyCritical,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "error_drop")

    val shake by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0; -6f at 100; 6f at 200; -4f at 300
                4f at 400; -2f at 500; 0f at 600; 0f at 2000
            },
            repeatMode = RepeatMode.Restart,
        ), label = "shake",
    )

    val errorPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "error_pulse",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width; val h = this.size.height
            val cx = w / 2f + shake; val cy = h / 2f

            // Glow
            drawCircle(
                color = color.copy(alpha = 0.12f * errorPulse),
                radius = w * 0.48f,
                center = Offset(cx, cy + h * 0.03f),
            )

            // Drop
            drawBloodDrop(
                cx, cy, w * 0.4f, h * 0.5f,
                Brush.verticalGradient(listOf(color.copy(alpha = 0.7f), color, color.copy(alpha = 0.85f))),
            )

            // X mark
            val xSize = w * 0.08f
            val xCy = cy + h * 0.06f
            drawLine(Color.White, Offset(cx - xSize, xCy - xSize), Offset(cx + xSize, xCy + xSize), 3.5f, StrokeCap.Round)
            drawLine(Color.White, Offset(cx + xSize, xCy - xSize), Offset(cx - xSize, xCy + xSize), 3.5f, StrokeCap.Round)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 5) Smaller inline blood drop icon
// ──────────────────────────────────────────────────────────────

@Composable
fun BloodDropIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = BloodRed,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        drawBloodDrop(
            w / 2f, h / 2f, w * 0.38f, h * 0.45f,
            Brush.verticalGradient(listOf(color.copy(alpha = 0.8f), color)),
        )
    }
}