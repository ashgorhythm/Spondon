package com.spondon.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.UnavailableGrey

@Composable
fun AvailabilityIndicator(
    isAvailable: Boolean,
    daysRemaining: Int = 0,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isAvailable) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(AvailableGreen))
            Spacer(Modifier.width(6.dp))
            Text("Available", style = MaterialTheme.typography.labelSmall, color = AvailableGreen)
        } else {
            Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(14.dp), tint = UnavailableGrey)
            Spacer(Modifier.width(6.dp))
            Text("$daysRemaining days", style = MaterialTheme.typography.labelSmall, color = UnavailableGrey)
        }
    }
}