package com.spondon.app.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.BloodDropLoader
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    // NOTE: AuthViewModel removed temporarily — Firebase is not configured yet.
    // Uncomment `viewModel: AuthViewModel = hiltViewModel()` and restore auth logic
    // after adding google-services.json.
) {
    // Entrance animation values
    val logoAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        // Animate in sequence
        logoAlpha.animateTo(1f, animationSpec = tween(600))
        logoScale.animateTo(1f, animationSpec = tween(500, easing = EaseOutBack))
        nameAlpha.animateTo(1f, animationSpec = tween(500))
        taglineAlpha.animateTo(1f, animationSpec = tween(500))
        delay(800)

        // TODO: Restore auth-based navigation after adding google-services.json:
        //   val state = viewModel.state.value
        //   val destination = when {
        //       !state.isOnboardingComplete -> Routes.Onboarding.route
        //       state.isLoggedIn && !state.needsProfileSetup -> Routes.Home.route
        //       state.isLoggedIn && state.needsProfileSetup -> Routes.DonorProfileSetup.route
        //       else -> Routes.Login.route
        //   }
        // For now, skip auth and go directly to Home:
        navController.navigate(Routes.Home.route) {
            popUpTo(Routes.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Animated blood drop logo
            BloodDropLoader(
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value),
                size = 120.dp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App name in Bangla
            Text(
                text = "স্পন্দন",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = BloodRed,
                modifier = Modifier.alpha(nameAlpha.value),
            )

            // App name in English
            Text(
                text = "Spondon",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 4.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.alpha(nameAlpha.value),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            Text(
                text = "রক্ত দিন, জীবন বাঁচান",
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value),
            )
        }
    }
}