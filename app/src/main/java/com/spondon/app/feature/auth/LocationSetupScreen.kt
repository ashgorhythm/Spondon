package com.spondon.app.feature.auth

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.components.StepProgressBar
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.PendingAmber
import com.spondon.app.core.ui.theme.UrgencyCritical
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSetupScreen(
    navController: NavController,
    viewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var districtExpanded by remember { mutableStateOf(false) }
    var upazilaExpanded by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    // Use the ViewModel state directly — no local copies that can drift.
    val selectedDistrict = state.selectedDistrict
    val selectedUpazila = state.selectedUpazila

    val districts = BangladeshData.districtNames

    // Derive upazila list safely from the current district
    val upazilas by remember(selectedDistrict) {
        derivedStateOf {
            if (selectedDistrict.isNotEmpty()) {
                BangladeshData.getUpazilas(selectedDistrict)
            } else {
                emptyList()
            }
        }
    }

    // Determine if the upazila section should be shown.
    // Using a derived boolean prevents flicker/crash from rapid recomposition.
    val showUpazila by remember(selectedDistrict) {
        derivedStateOf { selectedDistrict.isNotEmpty() }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            // Permission granted — fetch location
            isLocating = true
            locationError = null
            fetchCurrentLocation(context, scope, viewModel) { error ->
                isLocating = false
                locationError = error
            }
        } else {
            locationError = "Location permission denied. Please select your district manually."
        }
    }

    // Navigate to home on successful sign-up via one-shot event
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToHome -> {
                    navController.navigate(Routes.Home.route) {
                        popUpTo("auth_flow") { inclusive = true }
                    }
                }
                else -> {}
            }
        }
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
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp),
        ) {
            // Step progress
            StepProgressBar(
                currentStep = 2,
                totalSteps = 3,
                stepLabels = listOf("Basic Info", "Health Profile", "Location"),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Your Location",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Step 3 of 3 — Help us find donors near you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Use current location button
            OutlinedButton(
                onClick = {
                    locationError = null
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isLocating,
            ) {
                if (isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BloodRed, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Detecting location...")
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = BloodRed)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Use my current location")
                }
            }

            // Location detection error
            if (locationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = PendingAmber.copy(alpha = 0.1f)),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = locationError!!,
                        color = PendingAmber,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OR divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    "  or select manually  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                )
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // District Picker
            Text(
                text = "District",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = districtExpanded,
                onExpandedChange = { districtExpanded = !districtExpanded },
            ) {
                OutlinedTextField(
                    value = selectedDistrict,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select District") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = MaterialTheme.shapes.medium,
                )
                ExposedDropdownMenu(
                    expanded = districtExpanded,
                    onDismissRequest = { districtExpanded = false },
                ) {
                    districts.forEach { district ->
                        DropdownMenuItem(
                            text = { Text(district) },
                            onClick = {
                                // Close upazila dropdown first to prevent stale state
                                upazilaExpanded = false
                                viewModel.selectDistrict(district)
                                districtExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Upazila Picker — guarded with stable visibility flag
            AnimatedVisibility(
                visible = showUpazila,
                enter = fadeIn(tween(200, easing = LinearEasing)) + expandVertically(animationSpec = tween(200, easing = LinearEasing)),
                exit = fadeOut(tween(150, easing = LinearEasing)) + shrinkVertically(animationSpec = tween(150, easing = LinearEasing)),
            ) {
                Column {
                    Text(
                        text = "Upazila",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = upazilaExpanded,
                        onExpandedChange = {
                            // Only allow expanding if we have upazilas to show
                            if (upazilas.isNotEmpty()) {
                                upazilaExpanded = !upazilaExpanded
                            }
                        },
                    ) {
                        OutlinedTextField(
                            value = selectedUpazila,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Upazila") },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upazilaExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        ExposedDropdownMenu(
                            expanded = upazilaExpanded,
                            onDismissRequest = { upazilaExpanded = false },
                        ) {
                            upazilas.forEach { upazila ->
                                DropdownMenuItem(
                                    text = { Text(upazila) },
                                    onClick = {
                                        viewModel.selectUpazila(upazila)
                                        upazilaExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Privacy note
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = AvailableGreen,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your exact location is never shared. It is used only for matching you with nearby blood requests.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp,
                    )
                }
            }

            // Selected location summary
            if (selectedDistrict.isNotEmpty() && selectedUpazila.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BloodRed.copy(alpha = 0.08f)),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BloodRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = selectedUpazila,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = selectedDistrict,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            // Error message
            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = UrgencyCritical.copy(alpha = 0.1f)),
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

            // Complete Setup
            SpondonButton(
                text = if (state.isLoading) "Creating account..." else "Complete Setup",
                onClick = { viewModel.completeSignUp() },
                enabled = viewModel.isStep3Valid() && !state.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BloodRed)
            }
        }
    }
}

/**
 * Fetches the current device location using FusedLocationProviderClient
 * and reverse-geocodes it to find the district and upazila in Bangladesh.
 */
@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    viewModel: AuthViewModel,
    onComplete: (error: String?) -> Unit,
) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    handleLocation(context, scope, viewModel, location, onComplete)
                } else {
                    // Fallback: try getLastLocation
                    try {
                        fusedClient.lastLocation
                            .addOnSuccessListener { lastLocation ->
                                if (lastLocation != null) {
                                    handleLocation(context, scope, viewModel, lastLocation, onComplete)
                                } else {
                                    onComplete("Could not determine your location. Please make sure GPS is enabled and try again, or select manually.")
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete("Location detection failed: ${e.message}")
                            }
                    } catch (e: SecurityException) {
                        onComplete("Location permission required. Please select manually.")
                    }
                }
            }
            .addOnFailureListener { e ->
                // Fallback: try getLastLocation
                try {
                    fusedClient.lastLocation
                        .addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) {
                                handleLocation(context, scope, viewModel, lastLocation, onComplete)
                            } else {
                                onComplete("Location detection failed: ${e.message}")
                            }
                        }
                        .addOnFailureListener {
                            onComplete("Location detection failed: ${e.message}")
                        }
                } catch (se: SecurityException) {
                    onComplete("Location permission required. Please select manually.")
                }
            }
    } catch (e: SecurityException) {
        onComplete("Location permission required. Please select manually.")
    } catch (e: Exception) {
        onComplete("Location detection failed: ${e.message}")
    }
}

private fun handleLocation(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    viewModel: AuthViewModel,
    location: android.location.Location,
    onComplete: (error: String?) -> Unit,
) {
    scope.launch {
        try {
            val (district, upazila) = withContext(Dispatchers.IO) {
                reverseGeocode(context, location.latitude, location.longitude)
            }
            if (district != null) {
                // Try to match with our BangladeshData
                val matchedDistrict = BangladeshData.districtNames.find {
                    it.equals(district, ignoreCase = true)
                }
                if (matchedDistrict != null) {
                    viewModel.selectDistrict(matchedDistrict)
                    if (upazila != null) {
                        val matchedUpazila = BangladeshData.getUpazilas(matchedDistrict).find {
                            it.equals(upazila, ignoreCase = true) || upazila.contains(it, ignoreCase = true) || it.contains(upazila, ignoreCase = true)
                        }
                        if (matchedUpazila != null) {
                            viewModel.selectUpazila(matchedUpazila)
                        }
                    }
                    onComplete(null)
                } else {
                    onComplete("Detected: $district. Could not match — please select manually.")
                }
            } else {
                onComplete("Could not determine your district. Please select manually.")
            }
        } catch (e: Exception) {
            onComplete("Location detection failed: ${e.message}")
        }
    }
}

@Suppress("DEPRECATION")
private fun reverseGeocode(
    context: android.content.Context,
    lat: Double,
    lon: Double,
): Pair<String?, String?> {
    // Check if Geocoder is available on this device
    if (!Geocoder.isPresent()) {
        return null to null
    }
    return try {
        val geocoder = Geocoder(context, Locale("en", "BD"))
        val addresses = geocoder.getFromLocation(lat, lon, 5) ?: return null to null
        if (addresses.isEmpty()) return null to null

        // Try to find district and upazila from the address components
        var district: String? = null
        var upazila: String? = null

        for (address in addresses) {
            // The "adminArea" is typically the division, "subAdminArea" is the district
            if (district == null) {
                district = address.subAdminArea ?: address.locality ?: address.adminArea
            }
            if (upazila == null) {
                upazila = address.locality ?: address.subLocality
                // If upazila matches district, try subLocality instead
                if (upazila != null && upazila.equals(district, ignoreCase = true)) {
                    upazila = address.subLocality
                }
            }
            // Clean up district name
            district = district
                ?.replace(" District", "")
                ?.replace(" Zila", "")
                ?.replace(" district", "")
                ?.replace(" zila", "")
                ?.trim()
            if (district != null && upazila != null) break
        }

        district to upazila
    } catch (e: Exception) {
        null to null
    }
}