package com.spondon.app.feature.auth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes

private data class PermissionItem(
    val icon: ImageVector,
    val title: String,
    val titleBn: String,
    val description: String,
    val descriptionBn: String,
    val permission: String,
    val color: Color,
    val minSdk: Int = 0,
)

@Composable
fun PermissionsScreen(
    navController: NavController,
) {
    val permissions = remember {
        listOf(
            PermissionItem(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                titleBn = "পুশ নোটিফিকেশন",
                description = "Receive urgent blood request alerts, community updates, and donor responses instantly.",
                descriptionBn = "জরুরি রক্তের অনুরোধ, কমিউনিটি আপডেট এবং দাতার সাড়া তাৎক্ষণিকভাবে পান।",
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.POST_NOTIFICATIONS else "",
                color = BloodRed,
                minSdk = Build.VERSION_CODES.TIRAMISU,
            ),
            PermissionItem(
                icon = Icons.Default.MyLocation,
                title = "Location Access",
                titleBn = "অবস্থান অ্যাক্সেস",
                description = "Find nearby donors and match you with blood requests in your area. Your exact location is never shared.",
                descriptionBn = "কাছের দাতা খুঁজুন এবং আপনার এলাকার রক্তের অনুরোধের সাথে মিলান। আপনার সঠিক অবস্থান কখনো শেয়ার হয় না।",
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                color = AvailableGreen,
            ),
            PermissionItem(
                icon = Icons.Default.Call,
                title = "Phone Calls",
                titleBn = "ফোন কল",
                description = "Quickly call donors directly from the app when you find a match.",
                descriptionBn = "ম্যাচ পেলে সরাসরি অ্যাপ থেকে দাতাকে দ্রুত কল করুন।",
                permission = Manifest.permission.CALL_PHONE,
                color = SoftRose,
            ),
            PermissionItem(
                icon = Icons.Default.CameraAlt,
                title = "Camera",
                titleBn = "ক্যামেরা",
                description = "Upload profile photos and community cover images to personalize your experience.",
                descriptionBn = "প্রোফাইল ফটো এবং কমিউনিটি কভার ইমেজ আপলোড করুন।",
                permission = Manifest.permission.CAMERA,
                color = PendingAmber,
            ),
        )
    }

    // Build the list of permissions that need requesting
    val permissionsToRequest = remember {
        permissions
            .filter { it.permission.isNotEmpty() }
            .filter { Build.VERSION.SDK_INT >= it.minSdk }
            .map { it.permission }
            .toTypedArray()
    }

    var allRequested by remember { mutableStateOf(false) }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        allRequested = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ─── Header ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BloodRed, DarkRose, BloodRed.copy(alpha = 0.85f)),
                        ),
                    )
                    .padding(top = 56.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "App Permissions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "We need a few permissions to serve you better",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ─── Permission Cards ────────────────────
            permissions.forEach { item ->
                PermissionCard(item = item)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))

            // ─── Grant All Button ────────────────────
            if (!allRequested) {
                Button(
                    onClick = {
                        if (permissionsToRequest.isNotEmpty()) {
                            multiplePermissionLauncher.launch(permissionsToRequest)
                        } else {
                            allRequested = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Grant All Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Continue Button ─────────────────────
            OutlinedButton(
                onClick = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Permissions.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (allRequested) "Continue" else "Skip for Now",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BloodRed,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "You can change these later in your device settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = item.color.copy(alpha = 0.06f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
