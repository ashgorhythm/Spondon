package com.spondon.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spondon.app.core.domain.model.Urgency
import com.spondon.app.core.ui.theme.*

@Composable
fun UrgencyTag(urgency: Urgency, modifier: Modifier = Modifier) {
    val (bg, label) = when (urgency) {
        Urgency.CRITICAL -> UrgencyCritical to "CRITICAL"
        Urgency.MODERATE -> UrgencyModerate to "MODERATE"
        Urgency.NORMAL -> UrgencyNormal to "NORMAL"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}