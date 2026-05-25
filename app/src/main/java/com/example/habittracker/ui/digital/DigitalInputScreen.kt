// 경로: com/example/habittracker/ui/digital/DigitalInputScreen.kt
package com.example.habittracker.ui.digital

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
import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.ui.avatar.SharedAvatarViewModel
import com.example.habittracker.ui.components.CategoryScaffold
import com.example.habittracker.ui.theme.DigitalBackground
import com.example.habittracker.ui.theme.DigitalContainer
import com.example.habittracker.ui.theme.DigitalPrimary
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitCategoryStyle
import com.example.habittracker.ui.theme.HabitElevation
import com.example.habittracker.ui.theme.HabitLineGray
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary

@Composable
fun DigitalInputScreen(
    navController: NavController,
    interventionId: Long = -1L,
    viewModel: DigitalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarVm: SharedAvatarViewModel = hiltViewModel()
    val avatarUiState by avatarVm.uiState.collectAsStateWithLifecycle()
    val status = uiState.todayStatus

    val totalMin = status?.totalUsageMinutes ?: 0
    val speech = when {
        totalMin >= 180 -> "눈이 피곤해 보여요.\n5분만 쉬어갈까요? 📱"
        totalMin >= 60 -> "디지털 사용이 꽤 많아요.\n잠깐 스트레칭은 어떤가요? 📱"
        else -> "오늘 디지털 사용이 적당해요!\n이대로 유지해봐요 😊"
    }

    CategoryScaffold(
        category = HabitCategoryStyle.DIGITAL,
        title = "해빗프렌즈",
        speech = speech,
        avatarUiState = avatarUiState,
        onSettingsClick = { navController.navigate("settings") },
        onReportsClick = { navController.navigate("reports") },
    ) {
        DigitalStatusCard(status = status)
        DigitalActionCard(
            interventionId = interventionId,
            onBreak = {
                if (interventionId >= 0) viewModel.onInterventionAction(interventionId, "break")
                navController.popBackStack()
            },
            onStretch = {
                if (interventionId >= 0) viewModel.onInterventionAction(interventionId, "stretch")
                navController.navigate("stretch")
            },
            onContinue = {
                if (interventionId >= 0) viewModel.onInterventionAction(interventionId, "continue")
                navController.popBackStack()
            },
        )
        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DigitalStatusCard(status: DigitalTodayStatus?) {
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
                        color = DigitalBackground,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📱", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(HabitSpacing.sm))
                    Column {
                        Text(
                            text = "디지털",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HabitTextPrimary,
                        )
                        Text(
                            text = "스마트폰 · PC · 태블릿 사용 습관",
                            style = MaterialTheme.typography.bodySmall,
                            color = HabitTextSecondary,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(HabitRadius.full),
                    color = DigitalBackground,
                ) {
                    Text(
                        text = "습관 목표 · 하루 2시간 이하",
                        modifier = Modifier.padding(
                            horizontal = HabitSpacing.sm,
                            vertical = HabitSpacing.xxs,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = DigitalPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(HabitSpacing.base))
            HorizontalDivider(color = HabitLineGray)
            Spacer(modifier = Modifier.height(HabitSpacing.base))

            if (status != null) {
                val hours = status.totalUsageMinutes / 60
                val mins = status.totalUsageMinutes % 60
                val timeText = if (hours > 0) "${hours}시간 ${mins}분" else "${mins}분"

                Text(
                    text = "오늘 디지털 기기 사용 시간",
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.xs))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DigitalPrimary,
                )

                status.topApp?.let { app ->
                    Spacer(modifier = Modifier.height(HabitSpacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HabitSpacing.xl),
                    ) {
                        DigitalStat(label = "최다 앱", value = app)
                        DigitalStat(label = "세션 수", value = "${status.interventionCount}회")
                        DigitalStat(
                            label = "목표 달성",
                            value = "${(status.reactedCount * 100f / (status.interventionCount.coerceAtLeast(1))).toInt()}%",
                        )
                    }
                }
            } else {
                Text("데이터 로딩 중...", color = HabitTextSecondary)
            }
        }
    }
}

@Composable
private fun DigitalStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = HabitTextPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = HabitTextSecondary,
        )
    }
}

@Composable
private fun DigitalActionCard(
    interventionId: Long,
    onBreak: () -> Unit,
    onStretch: () -> Unit,
    onContinue: () -> Unit,
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
                text = "지금 무엇을 할까요?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Button(
                onClick = onBreak,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = DigitalPrimary),
            ) {
                Text("⏸ 5분 쉬기", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStretch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = DigitalContainer),
            ) {
                Text("🧘 스트레칭하기", color = DigitalPrimary, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
            ) {
                Text("계속 보기", color = HabitTextSecondary)
            }
        }
    }
}
