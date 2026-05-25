// 경로: com/example/habittracker/ui/components/CategoryScaffold.kt
package com.example.habittracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.habittracker.ui.avatar.AvatarUiState
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitCategoryStyle
import com.example.habittracker.ui.theme.HabitElevation
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary

@Composable
fun CategoryScaffold(
    category: HabitCategoryStyle,
    title: String,
    speech: String,
    avatarUiState: AvatarUiState,
    onSettingsClick: () -> Unit,
    onReportsClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(category.backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            CategoryTopBar(
                title = title,
                onSettingsClick = onSettingsClick,
                onReportsClick = onReportsClick,
            )

            CategoryAvatarSection(
                speech = speech,
                avatarUiState = avatarUiState,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HabitSpacing.base),
                verticalArrangement = Arrangement.spacedBy(HabitSpacing.md),
            ) {
                content()
                Spacer(modifier = Modifier.height(HabitSpacing.xl))
            }
        }
    }
}

@Composable
private fun CategoryTopBar(
    title: String,
    onSettingsClick: () -> Unit,
    onReportsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HabitSpacing.xs, vertical = HabitSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "설정",
                tint = HabitTextSecondary,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = HabitTextPrimary,
        )
        IconButton(onClick = onReportsClick) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "리포트",
                tint = HabitTextSecondary,
            )
        }
    }
}

@Composable
private fun CategoryAvatarSection(
    speech: String,
    avatarUiState: AvatarUiState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = HabitSpacing.base),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (avatarUiState.imageResId != 0) {
            Image(
                painter = painterResource(id = avatarUiState.imageResId),
                contentDescription = "아바타",
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(120.dp, 160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("👤", style = MaterialTheme.typography.displayMedium)
            }
        }

        Spacer(modifier = Modifier.width(HabitSpacing.sm))

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = HabitSpacing.base),
            shape = RoundedCornerShape(
                topStart = HabitRadius.xs,
                topEnd = HabitRadius.card,
                bottomStart = HabitRadius.card,
                bottomEnd = HabitRadius.card,
            ),
            colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
        ) {
            Column(modifier = Modifier.padding(HabitSpacing.base)) {
                Text(
                    text = "${avatarUiState.userName.ifEmpty { "나" }}님,",
                    style = MaterialTheme.typography.labelMedium,
                    color = HabitTextSecondary,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.xxs))
                Text(
                    text = speech,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = HabitTextPrimary,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(HabitSpacing.base))
}
