package com.spondon.app.feature.superadmin.forceupdate

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
private val SAOrange = Color(0xFFFFA726)
private val SABlue = Color(0xFF42A5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAForceUpdateScreen(
    navController: NavController,
    viewModel: SAForceUpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveSuccess) {
        state.saveSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveSuccess()
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
                        Icon(Icons.Outlined.SystemUpdate, null, tint = SAGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Force Update Control", fontWeight = FontWeight.Bold, color = Color.White)
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
                // ─── Current Status Card ─────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.config.isForceUpdate) SAOrange.copy(alpha = 0.08f)
                            else SAGreen.copy(alpha = 0.08f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                if (state.config.isForceUpdate) Icons.Outlined.NewReleases else Icons.Outlined.VerifiedUser,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = if (state.config.isForceUpdate) SAOrange else SAGreen,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (state.config.isForceUpdate) "FORCE UPDATE ACTIVE" else "No Force Update",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (state.config.isForceUpdate) SAOrange else SAGreen,
                            )
                            if (state.config.latestVersionName.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Latest: v${state.config.latestVersionName} (${state.config.latestVersionCode}) · Min: ${state.config.minimumVersionCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }

                // ─── Version Config ──────────────────────
                item {
                    Text(
                        "VERSION CONFIGURATION",
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedTextField(
                                    value = state.editMinVersionCode,
                                    onValueChange = viewModel::updateMinVersionCode,
                                    label = { Text("Min Version Code", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = saTextFieldColors(),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                OutlinedTextField(
                                    value = state.editLatestVersionCode,
                                    onValueChange = viewModel::updateLatestVersionCode,
                                    label = { Text("Latest Code", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = saTextFieldColors(),
                                    shape = RoundedCornerShape(10.dp),
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.editLatestVersionName,
                                onValueChange = viewModel::updateLatestVersionName,
                                label = { Text("Latest Version Name", color = Color.White.copy(alpha = 0.4f)) },
                                placeholder = { Text("1.2.0", color = Color.White.copy(alpha = 0.2f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = saTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.editPlayStoreUrl,
                                onValueChange = viewModel::updatePlayStoreUrl,
                                label = { Text("Play Store URL", color = Color.White.copy(alpha = 0.4f)) },
                                placeholder = { Text("https://play.google.com/store/apps/details?id=...", color = Color.White.copy(alpha = 0.2f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = saTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.editReleaseNotes,
                                onValueChange = viewModel::updateReleaseNotes,
                                label = { Text("Release Notes (shown to users)", color = Color.White.copy(alpha = 0.4f)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                colors = saTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                            )

                            Spacer(Modifier.height(16.dp))

                            // Force update toggle
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (state.editIsForceUpdate) SARed.copy(alpha = 0.08f)
                                    else Color.White.copy(alpha = 0.03f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Force Update",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (state.editIsForceUpdate) SARed else Color.White.copy(alpha = 0.7f),
                                        )
                                        Text(
                                            "Users below min version MUST update",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.4f),
                                        )
                                    }
                                    Switch(
                                        checked = state.editIsForceUpdate,
                                        onCheckedChange = { viewModel.toggleForceUpdate() },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = SARed,
                                            checkedThumbColor = Color.White,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Save Button ─────────────────────────
                item {
                    Button(
                        onClick = viewModel::saveConfig,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SAGold,
                            contentColor = SADark,
                        ),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = SADark, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Save, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save Configuration", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ─── Info ────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SABlue.copy(alpha = 0.06f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Outlined.Info, null, Modifier.size(14.dp), tint = SABlue.copy(alpha = 0.5f))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "When force update is enabled, users with version code below the minimum will " +
                                        "see a non-dismissible update screen on app launch. They can only proceed after " +
                                        "updating to the latest version from the Play Store.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = Color.White.copy(alpha = 0.4f),
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun saTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
    cursorColor = Color(0xFFFFD700),
    focusedBorderColor = Color(0xFFFFD700).copy(alpha = 0.4f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
)
