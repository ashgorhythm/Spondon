package com.spondon.app.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.UrgencyCritical

/**
 * Auth state enum used by the blood overlay.
 */
enum class AuthOverlayState {
    HIDDEN, LOADING, SUCCESS, ERROR
}

/**
 * Full-screen overlay with blood-themed animations for auth states.
 *
 * - **LOADING**: Filling blood drop + "Signing in..." text
 * - **SUCCESS**: Heartbeat blood drop + "Welcome!" text → auto-dismisses
 * - **ERROR**: Shaking blood drop with X + error message → tap to dismiss
 */
@Composable
fun AuthStateOverlay(
    state: AuthOverlayState,
    loadingText: String = "Signing in...",
    successText: String = "Welcome!",
    errorText: String = "Something went wrong",
    onDismiss: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = state != AuthOverlayState.HIDDEN,
        enter = fadeIn(animationSpec = tween(250, easing = LinearEasing)),
        exit = fadeOut(animationSpec = tween(300, easing = LinearEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (fadeIn(tween(300, easing = LinearEasing)) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(300, easing = LinearEasing),
                        )) togetherWith (fadeOut(tween(200, easing = LinearEasing)) + scaleOut(
                            targetScale = 0.8f,
                            animationSpec = tween(200, easing = LinearEasing),
                        ))
                    },
                    label = "auth_overlay_content",
                ) { currentState ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (currentState) {
                            AuthOverlayState.LOADING -> {
                                BloodDropFillingLoader(size = 100.dp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = loadingText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }

                            AuthOverlayState.SUCCESS -> {
                                BloodDropHeartbeat(
                                    size = 100.dp,
                                    color = AvailableGreen,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = successText,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                    ),
                                    color = AvailableGreen,
                                )
                            }

                            AuthOverlayState.ERROR -> {
                                BloodDropError(
                                    size = 100.dp,
                                    color = UrgencyCritical,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = errorText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = UrgencyCritical,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 40.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tap anywhere to dismiss",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                )
                            }

                            AuthOverlayState.HIDDEN -> {}
                        }
                    }
                }
            }
        }
    }
}
