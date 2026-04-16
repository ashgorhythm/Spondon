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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.SoftRose

/**
 * Animated blood drop loader with pulsing effect.
 * Uses Canvas for smooth custom drawing without requiring Lottie assets.
 */
@Composable
fun BloodDropLoader(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    color: Color = BloodRed,
    accentColor: Color = SoftRose,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blood_drop")

    // Pulsing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    // Subtle glow alpha animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    // Highlight shimmer offset
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // Draw outer glow
            drawCircle(
                color = color.copy(alpha = glowAlpha),
                radius = w * 0.48f * scale,
                center = Offset(cx, cy + h * 0.05f),
            )

            // Draw blood drop shape
            drawBloodDrop(
                cx = cx,
                cy = cy,
                width = w * 0.42f * scale,
                height = h * 0.52f * scale,
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor, color, color.copy(alpha = 0.85f)),
                    startY = cy - h * 0.25f,
                    endY = cy + h * 0.28f,
                ),
            )

            // Draw highlight/shimmer
            val shimmerY = cy - h * 0.15f + shimmerOffset * h * 0.3f
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = w * 0.06f * scale,
                center = Offset(cx - w * 0.06f, shimmerY),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = w * 0.035f * scale,
                center = Offset(cx - w * 0.12f, shimmerY + h * 0.04f),
            )
        }
    }
}

private fun DrawScope.drawBloodDrop(
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    brush: Brush,
) {
    val path = Path().apply {
        // Start at the top point of the drop
        moveTo(cx, cy - height)

        // Left curve down
        cubicTo(
            cx - width * 0.15f, cy - height * 0.65f,   // control 1
            cx - width, cy - height * 0.1f,              // control 2
            cx - width, cy + height * 0.15f,             // end
        )

        // Bottom left curve (round bottom)
        cubicTo(
            cx - width, cy + height * 0.65f,
            cx - width * 0.55f, cy + height,
            cx, cy + height,
        )

        // Bottom right curve (round bottom)
        cubicTo(
            cx + width * 0.55f, cy + height,
            cx + width, cy + height * 0.65f,
            cx + width, cy + height * 0.15f,
        )

        // Right curve back up to top
        cubicTo(
            cx + width, cy - height * 0.1f,
            cx + width * 0.15f, cy - height * 0.65f,
            cx, cy - height,
        )

        close()
    }

    drawPath(path = path, brush = brush, style = Fill)
}

/**
 * Smaller inline blood drop icon for use in cards, list items, etc.
 */
@Composable
fun BloodDropIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = BloodRed,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        drawBloodDrop(
            cx = cx,
            cy = cy,
            width = w * 0.38f,
            height = h * 0.45f,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.8f), color),
            ),
        )
    }
}