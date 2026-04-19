package com.spondon.app.feature.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.AuthOverlayState
import com.spondon.app.core.ui.components.AuthStateOverlay
import com.spondon.app.core.ui.components.BloodDropLoader
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.components.SpondonTextField
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    navController: NavController,
    onGoogleSignIn: () -> Unit = {},
    viewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // ── Entrance animations ──
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.5f) }
    val formAlpha = remember { Animatable(0f) }
    val formOffset = remember { Animatable(40f) }
    val socialAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(500))
        logoScale.animateTo(1f, tween(400, easing = EaseOutBack))
        formAlpha.animateTo(1f, tween(400))
        formOffset.animateTo(0f, tween(400, easing = EaseOutCubic))
        socialAlpha.animateTo(1f, tween(350))
    }

    // ── Auth overlay state ──
    var overlayState by remember { mutableStateOf(AuthOverlayState.HIDDEN) }
    var overlayMessage by remember { mutableStateOf("") }

    // Track loading → show filling blood drop
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            overlayState = AuthOverlayState.LOADING
        }
    }

    // Track error → show shaking blood drop
    LaunchedEffect(state.error) {
        if (state.error != null && !state.isLoading) {
            overlayMessage = state.error ?: "Sign in failed"
            overlayState = AuthOverlayState.ERROR
        }
    }

    // Track login success → show heartbeat blood drop → navigate
    LaunchedEffect(state.isLoginComplete) {
        if (state.isLoginComplete) {
            overlayState = AuthOverlayState.SUCCESS
            delay(1200) // Show success animation briefly
            navController.navigate(Routes.Home.route) {
                popUpTo(Routes.Login.route) { inclusive = true }
            }
        }
    }

    // Track profile setup needed
    LaunchedEffect(state.needsProfileSetup) {
        if (state.needsProfileSetup) {
            overlayState = AuthOverlayState.SUCCESS
            overlayMessage = "Almost there..."
            delay(800)
            navController.navigate(Routes.DonorProfileSetup.route) {
                popUpTo(Routes.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        BloodRed.copy(alpha = 0.03f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Logo with entrance animation ──
            BloodDropLoader(
                size = 80.dp,
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App name
            Text(
                text = "স্পন্দন",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                ),
                color = BloodRed,
                modifier = Modifier.alpha(logoAlpha.value),
            )
            Text(
                text = "Spondon",
                style = MaterialTheme.typography.bodyLarge.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Light,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.alpha(logoAlpha.value),
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Form with slide-up animation ──
            Column(
                modifier = Modifier
                    .alpha(formAlpha.value)
                    .offset(y = formOffset.value.dp),
            ) {
                // Welcome text
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Sign in to continue saving lives",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Email/Phone input
                SpondonTextField(
                    value = state.loginEmail,
                    onValueChange = { viewModel.updateLoginEmail(it) },
                    label = "Email or Phone",
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password
                SpondonTextField(
                    value = state.loginPassword,
                    onValueChange = { viewModel.updateLoginPassword(it) },
                    label = "Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleLoginPasswordVisibility() }) {
                            Icon(
                                if (state.loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    visualTransformation = if (state.loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Remember me & Forgot password row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.rememberMe,
                            onCheckedChange = { viewModel.toggleRememberMe() },
                            colors = CheckboxDefaults.colors(checkedColor = BloodRed),
                        )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                    }
                    TextButton(onClick = {
                        navController.navigate(Routes.ForgotPassword.route)
                    }) {
                        Text(
                            text = "Forgot Password?",
                            color = SoftRose,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }

                // Inline error (shown below the form, NOT the overlay)
                AnimatedVisibility(
                    visible = state.error != null && overlayState == AuthOverlayState.HIDDEN,
                    enter = slideInVertically(initialOffsetY = { -20 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -20 }) + fadeOut(),
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = UrgencyCritical.copy(alpha = 0.1f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = UrgencyCritical,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.error ?: "",
                                color = UrgencyCritical,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login button with scale press effect
                SpondonButton(
                    text = "Sign In",
                    onClick = {
                        viewModel.clearError()
                        viewModel.login()
                    },
                    enabled = state.loginEmail.isNotBlank() && state.loginPassword.isNotBlank() && !state.isLoading,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Social auth with fade-in ──
            Column(
                modifier = Modifier.alpha(socialAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = "  or continue with  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Social buttons in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Google Sign-In
                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ),
                    ) {
                        Text(
                            "G",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = BloodRed,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Google",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    // Biometric
                    OutlinedButton(
                        onClick = { /* BiometricPrompt integration */ },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = BloodRed,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Biometric",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sign up link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                    TextButton(onClick = {
                        navController.navigate(Routes.SignUp.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    }) {
                        Text("Sign Up", color = BloodRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Blood-themed auth overlay ──
        AuthStateOverlay(
            state = overlayState,
            loadingText = "Signing in...",
            successText = "Welcome back! 🩸",
            errorText = overlayMessage,
            onDismiss = {
                overlayState = AuthOverlayState.HIDDEN
                viewModel.clearError()
            },
        )

        // Dismiss error overlay on tap
        if (overlayState == AuthOverlayState.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        overlayState = AuthOverlayState.HIDDEN
                        viewModel.clearError()
                    },
            )
        }
    }
}