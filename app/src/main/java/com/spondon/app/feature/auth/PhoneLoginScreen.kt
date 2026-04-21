package com.spondon.app.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
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
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import java.net.URLEncoder

@Composable
fun PhoneLoginScreen(
    navController: NavController,
    onSendOtp: (String) -> Unit = {},
    viewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsState()

    // ── Entrance animations ──
    val headerAlpha = remember { Animatable(0f) }
    val formAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(400))
        formAlpha.animateTo(1f, tween(450))
    }

    // When OTP is sent, navigate to OTP screen
    LaunchedEffect(state.otpSent) {
        if (state.otpSent && state.verificationId.isNotEmpty()) {
            val phone = state.otpPhone.ifEmpty { state.phoneLoginNumber }
            val encodedPhone = URLEncoder.encode(phone, "UTF-8")
            navController.navigate("otp/$encodedPhone")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        BloodRed.copy(alpha = 0.02f),
                    ),
                ),
            ),
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
                text = "Phone Login",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ── Header ──
            Column(
                modifier = Modifier.alpha(headerAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BloodDropLoader(size = 80.dp)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Sign in with Phone",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enter your phone number and we'll send you a verification code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Phone number field ──
            Column(modifier = Modifier.alpha(formAlpha.value)) {
                // Country code hint
                Text(
                    text = "Bangladesh (+880)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                SpondonTextField(
                    value = state.phoneLoginNumber,
                    onValueChange = { viewModel.updatePhoneLoginNumber(it) },
                    label = "Phone Number (e.g. +8801XXXXXXXXX)",
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                    isError = state.phoneLoginNumber.isNotEmpty() && state.phoneLoginNumber.length < 11,
                    errorMessage = if (state.phoneLoginNumber.isNotEmpty() && state.phoneLoginNumber.length < 11)
                        "Enter a valid phone number" else null,
                )

                // Error display
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = UrgencyCritical.copy(alpha = 0.1f),
                        ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = state.error!!,
                            color = UrgencyCritical,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Send OTP button
                SpondonButton(
                    text = if (state.isLoading) "Sending OTP..." else "Send Verification Code",
                    onClick = {
                        viewModel.clearError()
                        var phone = state.phoneLoginNumber.trim()
                        // Auto-add +880 for Bangladeshi numbers
                        if (phone.startsWith("01") && phone.length == 11) {
                            phone = "+880$phone"
                        } else if (!phone.startsWith("+")) {
                            phone = "+$phone"
                        }
                        viewModel.setOtpPhone(phone)
                        onSendOtp(phone)
                    },
                    enabled = state.phoneLoginNumber.length >= 11 && !state.isLoading,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = "📱 Standard SMS rates may apply. We'll send a 6-digit code to your phone number for verification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp),
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}
