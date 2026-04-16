package com.spondon.app.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Countdown timer
    var timerSeconds by remember { mutableIntStateOf(60) }
    var timerActive by remember { mutableStateOf(true) }

    LaunchedEffect(timerActive) {
        if (timerActive) {
            while (timerSeconds > 0) {
                delay(1000)
                timerSeconds--
            }
            timerActive = false
        }
    }

    // Navigate on successful verification
    LaunchedEffect(state.isLoginComplete) {
        if (state.isLoginComplete) {
            navController.navigate(Routes.Home.route) {
                popUpTo(Routes.Splash.route) { inclusive = true }
            }
        }
    }

    // Error shake animation
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.error) {
        if (state.error != null) {
            repeat(3) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Verify Your Number",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Phone number hint
        val maskedPhone = if (state.otpPhone.length >= 4) {
            "+880 ×××× ×${state.otpPhone.takeLast(4)}"
        } else {
            "your phone number"
        }
        Text(
            text = "Code sent to $maskedPhone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 6 OTP digit boxes
        Row(
            modifier = Modifier
                .offset(x = shakeOffset.value.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            state.otpDigits.forEachIndexed { index, digit ->
                val isFocused = digit.isEmpty() && (index == 0 || state.otpDigits[index - 1].isNotEmpty())

                BasicTextField(
                    value = digit,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                            viewModel.updateOtpDigit(index, newValue)
                            if (newValue.isNotEmpty() && index < 5) {
                                focusRequesters[index + 1].requestFocus()
                            }
                            // Auto-verify when all 6 digits entered
                            if (index == 5 && newValue.isNotEmpty()) {
                                focusManager.clearFocus()
                                viewModel.verifyOtp()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (state.error != null) {
                                Modifier.border(2.dp, UrgencyCritical, RoundedCornerShape(12.dp))
                            } else if (digit.isNotEmpty()) {
                                Modifier.border(2.dp, BloodRed, RoundedCornerShape(12.dp))
                            } else {
                                Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp),
                                )
                            }
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .focusRequester(focusRequesters[index]),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    cursorBrush = SolidColor(BloodRed),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            innerTextField()
                        }
                    },
                )
            }
        }

        // Error message
        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.error!!,
                color = UrgencyCritical,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Timer & Resend
        if (timerActive && timerSeconds > 0) {
            Text(
                text = "Resend code in ${timerSeconds}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        } else {
            TextButton(
                onClick = {
                    timerSeconds = 60
                    timerActive = true
                    // Trigger resend OTP here
                },
            ) {
                Text("Resend Code", color = BloodRed, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Verify button
        SpondonButton(
            text = if (state.isLoading) "Verifying..." else "Verify",
            onClick = { viewModel.verifyOtp() },
            enabled = state.otpDigits.all { it.isNotEmpty() } && !state.isLoading,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}