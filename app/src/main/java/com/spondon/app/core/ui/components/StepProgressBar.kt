package com.spondon.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.SoftRose

/**
 * Enhanced step progress bar with connected lines, step labels, and animated transitions.
 */
@Composable
fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    stepLabels: List<String>? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(totalSteps) { step ->
                val isCompleted = step < currentStep
                val isCurrent = step == currentStep

                val dotSize by animateDpAsState(
                    targetValue = if (isCurrent) 14.dp else if (isCompleted) 12.dp else 10.dp,
                    animationSpec = tween(300),
                    label = "dot_size_$step",
                )

                val dotColor by animateColorAsState(
                    targetValue = when {
                        isCompleted -> BloodRed
                        isCurrent -> SoftRose
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(300),
                    label = "dot_color_$step",
                )

                // Step dot
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCompleted) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                // Connector line between dots
                if (step < totalSteps - 1) {
                    val lineColor by animateColorAsState(
                        targetValue = if (step < currentStep) BloodRed else MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(300),
                        label = "line_color_$step",
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(lineColor)
                    )
                }
            }
        }

        // Step labels
        if (stepLabels != null && stepLabels.size == totalSteps) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                stepLabels.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == currentStep) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            index < currentStep -> BloodRed
                            index == currentStep -> SoftRose
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}