// 경로: com/example/habittracker/ui/components/AvatarHealthView.kt
package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AvatarHealthView(
    score: Float,
    modifier: Modifier = Modifier,
) {
    val color = when {
        score >= 0.8f -> Color(0xFF4CAF50)
        score >= 0.5f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
    val emoji = when {
        score >= 0.8f -> "😄"
        score >= 0.5f -> "😐"
        else -> "😔"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.2f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
            }
        }
        Text(
            text = "건강도 ${(score * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
