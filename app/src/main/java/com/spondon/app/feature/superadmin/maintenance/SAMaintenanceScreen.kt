package com.spondon.app.feature.superadmin.maintenance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)
private val SAGreen = Color(0xFF4CAF50)
private val SARed = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAMaintenanceScreen(
    navController: NavController,
    viewModel: SAMaintenanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toggleSuccess) {
        state.toggleSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToggleSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Build, null, tint = SAGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Maintenance Mode", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SADark),
            )
        },
        containerColor = SADark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ─── Big Toggle Card ─────────────────────
                item {
                    val toggleColor by animateColorAsState(
                        targetValue = if (state.config.isEnabled) SARed else SAGreen,
                        label = "toggleColor",
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = toggleColor.copy(alpha = 0.08f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                if (state.config.isEnabled) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = toggleColor,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (state.config.isEnabled) "MAINTENANCE ACTIVE" else "SYSTEM OPERATIONAL",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                                color = toggleColor,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (state.config.isEnabled)
                                    "All users see the maintenance gate screen. The app is effectively offline."
                                else
                                    "System is running normally. Toggle maintenance below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // ─── Message Editor ──────────────────────
                item {
                    Text(
                        "MESSAGE EDITOR",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = state.editTitle,
                                onValueChange = viewModel::updateTitle,
                                label = { Text("Title (Bangla + English)", color = Color.White.copy(alpha = 0.4f)) },
                                placeholder = { Text("আমরা আপগ্রেড করছি", color = Color.White.copy(alpha = 0.2f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    cursorColor = SAGold,
                                    focusedBorderColor = SAGold.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.editMessage,
                                onValueChange = viewModel::updateMessage,
                                label = { Text("Message shown to users", color = Color.White.copy(alpha = 0.4f)) },
                                placeholder = { Text("অনুগ্রহ করে কিছুক্ষণ অপেক্ষা করুন", color = Color.White.copy(alpha = 0.2f)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    cursorColor = SAGold,
                                    focusedBorderColor = SAGold.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.editEstimatedMinutes,
                                onValueChange = viewModel::updateEstimatedMinutes,
                                label = { Text("Estimated downtime (minutes)", color = Color.White.copy(alpha = 0.4f)) },
                                placeholder = { Text("30", color = Color.White.copy(alpha = 0.2f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    cursorColor = SAGold,
                                    focusedBorderColor = SAGold.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )
                        }
                    }
                }

                // ─── Preview ─────────────────────────────
                item {
                    Text(
                        "USER PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121228)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Outlined.Engineering,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = Color.White.copy(alpha = 0.4f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                state.editTitle.ifBlank { "আমরা আপগ্রেড করছি" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.editMessage.ifBlank { "অনুগ্রহ করে কিছুক্ষণ অপেক্ষা করুন" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                            if (state.editEstimatedMinutes.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(containerColor = SAGold.copy(alpha = 0.1f)),
                                ) {
                                    Text(
                                        "~${state.editEstimatedMinutes} min",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SAGold,
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Toggle Button ───────────────────────
                item {
                    Button(
                        onClick = viewModel::showConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.config.isEnabled) SAGreen else SARed,
                        ),
                    ) {
                        Icon(
                            if (state.config.isEnabled) Icons.Outlined.PowerSettingsNew else Icons.Outlined.Warning,
                            null,
                            Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (state.config.isEnabled) "Disable Maintenance Mode" else "Enable Maintenance Mode",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // ─── Warning ─────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SARed.copy(alpha = 0.06f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Outlined.Warning, null, Modifier.size(14.dp), tint = SARed.copy(alpha = 0.5f))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Enabling maintenance mode immediately blocks ALL users from using the app. " +
                                        "They will see the maintenance gate screen. This action requires passphrase confirmation.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = Color.White.copy(alpha = 0.4f),
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        // ─── Confirmation Dialog ─────────────────────
        if (state.showConfirmDialog) {
            AlertDialog(
                onDismissRequest = viewModel::hideConfirm,
                containerColor = SADarkCard,
                title = {
                    Text(
                        if (state.config.isEnabled) "Disable Maintenance?" else "Enable Maintenance?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                text = {
                    Column {
                        Text(
                            if (state.config.isEnabled)
                                "This will restore normal app operation for all users."
                            else
                                "This will immediately lock ALL users out of the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = state.confirmPassphrase,
                            onValueChange = viewModel::updateConfirmPassphrase,
                            label = { Text("Enter passphrase to confirm", color = Color.White.copy(alpha = 0.4f)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                cursorColor = SAGold,
                                focusedBorderColor = SAGold.copy(alpha = 0.4f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            ),
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::toggleMaintenance,
                        enabled = !state.isToggling && state.confirmPassphrase.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.config.isEnabled) SAGreen else SARed,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        if (state.isToggling) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (state.config.isEnabled) "Disable" else "Enable",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::hideConfirm) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                    }
                },
            )
        }
    }
}
