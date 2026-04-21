package com.spondon.app.feature.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.BloodDropLoader
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.components.SpondonTextField
import com.spondon.app.core.ui.components.StepProgressBar
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Reset state when leaving
    DisposableEffect(Unit) {
        onDispose { viewModel.resetForgotPasswordState() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Step indicator
            StepProgressBar(
                currentStep = if (state.isPasswordResetSuccess) 2 else 0,
                totalSteps = 3,
                stepLabels = listOf("Email", "Verification", "Done"),
            )

            Spacer(modifier = Modifier.height(40.dp))

            AnimatedContent(
                targetState = state.isPasswordResetSuccess,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "forgot_content",
            ) { isSuccess ->
                if (!isSuccess) {
                    // Step 1: Enter email
                    ForgotPasswordEmailStep(
                        email = state.resetEmail,
                        onEmailChange = { viewModel.updateResetEmail(it) },
                        isLoading = state.isLoading,
                        error = state.error,
                        onSendReset = { viewModel.sendPasswordResetEmail() },
                    )
                } else {
                    // Success screen
                    ForgotPasswordSuccess(
                        email = state.resetEmail,
                        onBackToLogin = {
                            navController.navigate(Routes.Login.route) {
                                popUpTo(Routes.ForgotPassword.route) { inclusive = true }
                            }
                        },
                        onResendEmail = {
                            viewModel.sendPasswordResetEmail()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordEmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onSendReset: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Illustration icon
        Icon(
            Icons.Default.LockReset,
            contentDescription = null,
            tint = BloodRed,
            modifier = Modifier.size(80.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Forgot your password?",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Enter your email address and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email field
        SpondonTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email Address",
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            isError = error != null,
            errorMessage = error,
        )

        Spacer(modifier = Modifier.height(32.dp))

        SpondonButton(
            text = if (isLoading) "Sending..." else "Send Reset Link",
            onClick = onSendReset,
            enabled = email.contains("@") && !isLoading,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tips card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💡 Tips",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Use the email you registered with\n• Check your spam folder if you don't see it\n• Link expires in 1 hour",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun ForgotPasswordSuccess(
    email: String,
    onBackToLogin: () -> Unit,
    onResendEmail: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Success checkmark
        Card(
            modifier = Modifier.size(100.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = AvailableGreen.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AvailableGreen,
                    modifier = Modifier.size(56.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Check your email",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We've sent a password reset link to:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email highlight
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = BloodRed.copy(alpha = 0.08f),
            ),
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.Email,
                    contentDescription = null,
                    tint = BloodRed,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = BloodRed,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Please check your inbox and follow the instructions to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(modifier = Modifier.height(40.dp))

        SpondonButton(
            text = "Back to Login",
            onClick = onBackToLogin,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Resend button
        TextButton(onClick = onResendEmail) {
            Text(
                text = "Didn't receive it? Resend",
                color = SoftRose,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}