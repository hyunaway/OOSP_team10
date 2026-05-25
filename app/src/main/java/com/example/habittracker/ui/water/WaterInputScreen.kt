// 경로: com/example/habittracker/ui/water/WaterInputScreen.kt
package com.example.habittracker.ui.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.habittracker.domain.model.WaterTodayStatus
import com.example.habittracker.ui.avatar.SharedAvatarViewModel
import com.example.habittracker.ui.components.CategoryScaffold
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitCategoryStyle
import com.example.habittracker.ui.theme.HabitElevation
import com.example.habittracker.ui.theme.HabitLineGray
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary
import com.example.habittracker.ui.theme.WaterBackground
import com.example.habittracker.ui.theme.WaterPrimary

@Composable
fun WaterInputScreen(
    navController: NavController,
    viewModel: WaterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarVm: SharedAvatarViewModel = hiltViewModel()
    val avatarUiState by avatarVm.uiState.collectAsStateWithLifecycle()
    val status = uiState.todayStatus

    val speech = when {
        status == null -> "물을 마시는 것도 좋은 습관이에요! 💧"
        status.achievementRate >= 1f -> "오늘 목표를 달성했어요!\n정말 잘했어요! 💧"
        status.achievementRate >= 0.5f -> "절반 이상 마셨어요!\n조금만 더 마셔봐요 💧"
        else -> "물이 조금 부족해요.\n한 잔만 마셔도 좋아요! 💧"
    }

    CategoryScaffold(
        category = HabitCategoryStyle.WATER,
        title = "해빗프렌즈",
        speech = speech,
        avatarUiState = avatarUiState,
        onSettingsClick = { navController.navigate("settings") },
        onReportsClick = { navController.navigate("reports") },
    ) {
        WaterStatusCard(status = status)
        WaterQuickAddCard(
            onAdd = { viewModel.onDrinkButtonClick(it) },
            onBack = { navController.popBackStack() },
        )
        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun WaterStatusCard(status: WaterTodayStatus?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Column(modifier = Modifier.padding(HabitSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = WaterBackground,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("💧", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(HabitSpacing.sm))
                    Column {
                        Text(
                            text = "물",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HabitTextPrimary,
                        )
                        Text(
                            text = "매일 ${status?.goalMl ?: 2000}ml 목표",
                            style = MaterialTheme.typography.bodySmall,
                            color = HabitTextSecondary,
                        )
                    }
                }
                val levelLabel = when {
                    (status?.achievementRate ?: 0f) >= 1f -> "충분"
                    (status?.achievementRate ?: 0f) >= 0.5f -> "보통"
                    else -> "낮음"
                }
                Surface(
                    shape = RoundedCornerShape(HabitRadius.full),
                    color = WaterBackground,
                ) {
                    Text(
                        text = "수분 레벨 · $levelLabel",
                        modifier = Modifier.padding(horizontal = HabitSpacing.sm, vertical = HabitSpacing.xxs),
                        style = MaterialTheme.typography.labelSmall,
                        color = WaterPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(HabitSpacing.base))
            HorizontalDivider(color = HabitLineGray)
            Spacer(modifier = Modifier.height(HabitSpacing.base))

            if (status != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${status.totalMl}ml",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = WaterPrimary,
                    )
                    Text(
                        text = "/ ${status.goalMl}ml",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HabitTextSecondary,
                    )
                    Text(
                        text = "${(status.achievementRate * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WaterPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                LinearProgressIndicator(
                    progress = { status.achievementRate.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = WaterPrimary,
                    trackColor = WaterBackground,
                )
            } else {
                Text("데이터 로딩 중...", color = HabitTextSecondary)
            }
        }
    }
}

@Composable
private fun WaterQuickAddCard(
    onAdd: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Column(
            modifier = Modifier.padding(HabitSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
        ) {
            Text(
                text = "빠르게 추가하기",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
            ) {
                WaterAddButton(
                    label = "+250ml",
                    sublabel = "한잔",
                    modifier = Modifier.weight(1f),
                    onClick = { onAdd(250) },
                )
                WaterAddButton(
                    label = "+500ml",
                    sublabel = "큰컵",
                    modifier = Modifier.weight(1f),
                    onClick = { onAdd(500) },
                )
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
            ) {
                Text("나중에", color = HabitTextSecondary)
            }
        }
    }
}

@Composable
private fun WaterAddButton(
    label: String,
    sublabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(HabitRadius.md),
        colors = ButtonDefaults.buttonColors(containerColor = WaterPrimary),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}
