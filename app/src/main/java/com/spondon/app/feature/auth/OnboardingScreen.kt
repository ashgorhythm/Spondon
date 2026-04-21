package com.spondon.app.feature.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class OnboardingPage(
    val title: String,
    val titleBn: String,
    val description: String,
    val accentColor: Color,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: AuthViewModel,
) {
    val pages = listOf(
        OnboardingPage(
            title = "Give Blood, Save Lives",
            titleBn = "রক্ত দিন, জীবন বাঁচান",
            description = "Join a community of heroes. Your single donation can save up to three lives. Every drop counts.",
            accentColor = BloodRed,
        ),
        OnboardingPage(
            title = "Find Your Community",
            titleBn = "আপনার কমিউনিটি খুঁজুন",
            description = "Connect with local blood donor communities. Get matched with nearby donors and requests in your area.",
            accentColor = SoftRose,
        ),
        OnboardingPage(
            title = "Donate with Trust",
            titleBn = "বিশ্বাসের সাথে দান করুন",
            description = "Verified communities, transparent processes, and safe donation practices. Your safety and privacy are our priority.",
            accentColor = AvailableGreen,
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Skip button
        TextButton(
            onClick = {
                viewModel.completeOnboarding()
                navController.navigate(Routes.Login.route) {
                    popUpTo(Routes.Onboarding.route) { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
        ) {
            Text(
                text = "Skip",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                OnboardingPageContent(page = pages[page], pageIndex = page)
            }

            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp),
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) pages[index].accentColor
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action button
            val isLastPage = pagerState.currentPage == pages.size - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        viewModel.completeOnboarding()
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accentColor,
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AnimatedContent(
                        targetState = isLastPage,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        },
                        label = "button_text",
                    ) { last ->
                        Text(
                            text = if (last) "Get Started" else "Next",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pageIndex: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Unique animated icon per page
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            page.accentColor.copy(alpha = 0.15f),
                            page.accentColor.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (pageIndex) {
                0 -> HeartbeatDonationAnimation(color = page.accentColor)
                1 -> CommunityNetworkAnimation(color = page.accentColor)
                2 -> ShieldTrustAnimation(color = page.accentColor)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Bangla title
        Text(
            text = page.titleBn,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = page.accentColor,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // English title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Page 1: Heartbeat blood drop with ECG pulse line
// ──────────────────────────────────────────────────────────────

@Composable
private fun HeartbeatDonationAnimation(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat_onboard")

    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1.0f at 0 using LinearEasing
                1.18f at 120 using EaseOutCubic
                0.92f at 240 using EaseInCubic
                1.14f at 380 using EaseOutCubic
                1.0f at 550 using EaseInOutSine
                1.0f at 1400 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ), label = "scale",
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "ecg",
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "glow",
    )

    Canvas(modifier = modifier.size(140.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Outer glow pulse
        drawCircle(
            color = color.copy(alpha = glowAlpha),
            radius = w * 0.48f * heartScale,
            center = Offset(cx, cy),
        )

        // Blood drop
        val dropPath = Path().apply {
            val dw = w * 0.3f * heartScale
            val dh = h * 0.38f * heartScale
            moveTo(cx, cy - dh)
            cubicTo(cx - dw * 0.15f, cy - dh * 0.65f, cx - dw, cy - dh * 0.1f, cx - dw, cy + dh * 0.15f)
            cubicTo(cx - dw, cy + dh * 0.65f, cx - dw * 0.55f, cy + dh, cx, cy + dh)
            cubicTo(cx + dw * 0.55f, cy + dh, cx + dw, cy + dh * 0.65f, cx + dw, cy + dh * 0.15f)
            cubicTo(cx + dw, cy - dh * 0.1f, cx + dw * 0.15f, cy - dh * 0.65f, cx, cy - dh)
            close()
        }
        drawPath(
            dropPath,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.7f), color, color.copy(alpha = 0.9f)),
            ),
        )

        // ECG pulse line across the middle of the drop
        val ecgPath = Path().apply {
            val lineY = cy + h * 0.04f
            val startX = cx - w * 0.18f
            val endX = cx + w * 0.18f
            val total = endX - startX
            moveTo(startX, lineY)
            // Flat start
            lineTo(startX + total * 0.25f, lineY)
            // Spike up
            lineTo(startX + total * 0.35f, lineY - h * 0.12f)
            // Spike down
            lineTo(startX + total * 0.45f, lineY + h * 0.08f)
            // Spike up (main peak)
            lineTo(startX + total * 0.55f, lineY - h * 0.18f * heartScale)
            // Return
            lineTo(startX + total * 0.65f, lineY + h * 0.04f)
            // Flat end
            lineTo(endX, lineY)
        }
        drawPath(ecgPath, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        // Shimmer
        drawCircle(Color.White.copy(alpha = 0.25f), w * 0.035f, Offset(cx - w * 0.08f, cy - h * 0.12f))
    }
}

// ──────────────────────────────────────────────────────────────
// Page 2: Connected nodes representing community network
// ──────────────────────────────────────────────────────────────

@Composable
private fun CommunityNetworkAnimation(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "community_net")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "rotate",
    )

    val connectionPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "conn_pulse",
    )

    val centralScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "central",
    )

    val orbitRadius by infiniteTransition.animateFloat(
        initialValue = 0.28f, targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "orbit",
    )

    Canvas(modifier = modifier.size(140.dp).rotate(0f)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Central node glow
        drawCircle(
            color = color.copy(alpha = 0.12f),
            radius = w * 0.22f * centralScale,
            center = Offset(cx, cy),
        )

        // Orbiting nodes
        val nodeCount = 5
        val nodePositions = (0 until nodeCount).map { i ->
            val angle = Math.toRadians((rotationAngle + i * 360.0 / nodeCount).toDouble())
            val r = w * orbitRadius
            Offset(
                cx + (r * cos(angle)).toFloat(),
                cy + (r * sin(angle)).toFloat(),
            )
        }

        // Draw connection lines
        nodePositions.forEach { pos ->
            drawLine(
                color = color.copy(alpha = connectionPulse),
                start = Offset(cx, cy),
                end = pos,
                strokeWidth = 1.5f,
                cap = StrokeCap.Round,
            )
        }

        // Draw connections between adjacent nodes
        for (i in nodePositions.indices) {
            val next = (i + 1) % nodePositions.size
            drawLine(
                color = color.copy(alpha = connectionPulse * 0.5f),
                start = nodePositions[i],
                end = nodePositions[next],
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
        }

        // Central blood-drop-shaped node
        val centralDrop = Path().apply {
            val dw = w * 0.07f * centralScale
            val dh = h * 0.09f * centralScale
            moveTo(cx, cy - dh)
            cubicTo(cx - dw * 0.15f, cy - dh * 0.65f, cx - dw, cy - dh * 0.1f, cx - dw, cy + dh * 0.15f)
            cubicTo(cx - dw, cy + dh * 0.65f, cx - dw * 0.55f, cy + dh, cx, cy + dh)
            cubicTo(cx + dw * 0.55f, cy + dh, cx + dw, cy + dh * 0.65f, cx + dw, cy + dh * 0.15f)
            cubicTo(cx + dw, cy - dh * 0.1f, cx + dw * 0.15f, cy - dh * 0.65f, cx, cy - dh)
            close()
        }
        drawPath(centralDrop, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.8f), color)), style = Fill)

        // Orbiting dots
        nodePositions.forEachIndexed { index, pos ->
            val nodeSize = w * (0.035f + (index % 2) * 0.015f)
            drawCircle(color = color.copy(alpha = 0.2f), radius = nodeSize * 1.6f, center = pos)
            drawCircle(color = color, radius = nodeSize, center = pos)
            // Person silhouette icon (simple)
            drawCircle(Color.White.copy(alpha = 0.7f), radius = nodeSize * 0.4f, center = Offset(pos.x, pos.y - nodeSize * 0.15f))
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Page 3: Shield with checkmark for trust & safety
// ──────────────────────────────────────────────────────────────

@Composable
private fun ShieldTrustAnimation(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_trust")

    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "shield_scale",
    )

    val checkProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0 using LinearEasing
                1f at 800 using EaseOutCubic
                1f at 3000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ), label = "check",
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ), label = "ring_alpha",
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
        ), label = "ring_scale",
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "glow",
    )

    Canvas(modifier = modifier.size(140.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Expanding ring
        drawCircle(
            color = color.copy(alpha = ringAlpha),
            radius = w * 0.35f * ringScale,
            center = Offset(cx, cy),
            style = Stroke(width = 2f),
        )

        // Background glow
        drawCircle(
            color = color.copy(alpha = glowPulse),
            radius = w * 0.4f * shieldScale,
            center = Offset(cx, cy),
        )

        // Shield shape
        val shieldPath = Path().apply {
            val sw = w * 0.28f * shieldScale
            val sh = h * 0.35f * shieldScale
            moveTo(cx, cy - sh) // Top center
            lineTo(cx + sw, cy - sh * 0.6f) // Top right
            lineTo(cx + sw * 0.9f, cy + sh * 0.15f) // Right side
            cubicTo(cx + sw * 0.7f, cy + sh * 0.7f, cx + sw * 0.2f, cy + sh, cx, cy + sh) // Bottom right curve
            cubicTo(cx - sw * 0.2f, cy + sh, cx - sw * 0.7f, cy + sh * 0.7f, cx - sw * 0.9f, cy + sh * 0.15f) // Bottom left curve
            lineTo(cx - sw, cy - sh * 0.6f) // Left side
            close()
        }
        drawPath(
            shieldPath,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.6f), color, color.copy(alpha = 0.85f)),
                startY = cy - h * 0.35f,
                endY = cy + h * 0.35f,
            ),
        )

        // Shield outline (subtle)
        drawPath(
            shieldPath,
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = 1.5f),
        )

        // Animated checkmark inside shield
        if (checkProgress > 0f) {
            val s = w * 0.08f * shieldScale
            val checkCy = cy + h * 0.02f
            val checkPath = Path().apply {
                moveTo(cx - s, checkCy)
                if (checkProgress < 0.5f) {
                    // First leg of checkmark
                    val p = checkProgress / 0.5f
                    lineTo(
                        cx - s + (cx - s * 0.3f - (cx - s)) * p,
                        checkCy + (checkCy + s * 0.8f - checkCy) * p,
                    )
                } else {
                    // Complete first leg
                    lineTo(cx - s * 0.3f, checkCy + s * 0.8f)
                    // Second leg
                    val p = (checkProgress - 0.5f) / 0.5f
                    lineTo(
                        cx - s * 0.3f + (cx + s - (cx - s * 0.3f)) * p,
                        checkCy + s * 0.8f + (checkCy - s * 0.5f - (checkCy + s * 0.8f)) * p,
                    )
                }
            }
            drawPath(
                checkPath,
                color = Color.White,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round),
            )
        }

        // Small sparkle highlights
        drawCircle(Color.White.copy(alpha = 0.2f), w * 0.025f, Offset(cx - w * 0.1f, cy - h * 0.15f))
        drawCircle(Color.White.copy(alpha = 0.12f), w * 0.018f, Offset(cx - w * 0.14f, cy - h * 0.1f))
    }
}