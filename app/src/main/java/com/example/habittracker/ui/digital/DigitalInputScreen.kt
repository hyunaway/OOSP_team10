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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.habittracker.Routes
import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.ui.avatar.AvatarState
import com.example.habittracker.ui.avatar.SharedAvatarViewModel
import com.example.habittracker.ui.avatar.forCategory
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
import com.example.habittracker.util.formatMinutes
import com.example.habittracker.widget.WidgetUpdateHelper

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
    val context = LocalContext.current

    val totalMinutes = status?.totalUsageMinutes ?: 0
    val isInterventionMode = interventionId >= 0
    val isOverDigitalThreshold = totalMinutes >= uiState.digitalInterventionThresholdMinutes
    val shouldShowActionCard = isInterventionMode || isOverDigitalThreshold

    val speech = when {
        shouldShowActionCard -> "디지털 사용이 길어졌어요.\n잠깐 쉬어가도 괜찮아요."
        uiState.selectedDigitalPackages.isEmpty() -> "관리 앱을 선택하면\n기준을 넘었을 때 도와드릴게요."
        else -> "오늘 디지털 사용이 적당해요!\n이대로 유지해봐요."
    }

    CategoryScaffold(
        category = HabitCategoryStyle.DIGITAL,
        title = "디지털",
        speech = speech,
        avatarUiState = avatarUiState.forCategory(AvatarState.DIGITAL_OVERUSE),
        onSettingsClick = { navController.navigate("settings") },
        onReportsClick = { navController.navigate("reports") },
    ) {
        DigitalStatusCard(
            status = status,
            thresholdMinutes = uiState.digitalInterventionThresholdMinutes,
        )
        DigitalManageAppsCard(
            selectedCount = uiState.selectedDigitalPackages.size,
            onClick = { navController.navigate(Routes.DIGITAL_APP_SELECTION) },
        )
        if (shouldShowActionCard) {
            DigitalActionCard(
                onBreak = {
                    if (interventionId >= 0) viewModel.onInterventionAction(interventionId, "break")
                    WidgetUpdateHelper.updateAllWidgetsSync(context)
                    navController.popBackStack()
                },
                onStretch = {
                    if (interventionId >= 0) viewModel.onInterventionAction(interventionId, "stretch")
                    WidgetUpdateHelper.updateAllWidgetsSync(context)
                    navController.navigate("stretch")
                },
                onContinue = {
                    if (interventionId >= 0) viewModel.onInterventionAction(interventionId, "continue")
                    WidgetUpdateHelper.updateAllWidgetsSync(context)
                    navController.popBackStack()
                },
            )
        } else {
            DigitalNormalStateCard(
                hasSelectedApps = uiState.selectedDigitalPackages.isNotEmpty(),
                thresholdMinutes = uiState.digitalInterventionThresholdMinutes,
            )
        }
        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HabitRadius.button),
        ) {
            Text("뒤로 가기", color = HabitTextSecondary)
        }
    }
}

@Composable
private fun DigitalManageAppsCard(
    selectedCount: Int,
    onClick: () -> Unit,
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
                text = "관리 앱 설정",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Text(
                text = if (selectedCount > 0) {
                    "관리 앱 ${selectedCount}개 선택됨"
                } else {
                    "관리할 앱을 선택하면 사용 시간이 기준을 넘었을 때 개입할 수 있어요."
                },
                style = MaterialTheme.typography.bodySmall,
                color = HabitTextSecondary,
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = DigitalPrimary),
            ) {
                Text("관리 앱 설정하기", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DigitalStatusCard(
    status: DigitalTodayStatus?,
    thresholdMinutes: Int,
) {
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
                        text = "목표 · ${formatMinutes(thresholdMinutes)} 이하",
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
                Text(
                    text = "오늘 디지털 기기 사용 시간",
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.xs))
                Text(
                    text = formatMinutes(status.totalUsageMinutes),
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
                        DigitalStat(label = "개입", value = "${status.interventionCount}회")
                        DigitalStat(
                            label = "반응률",
                            value = "${(status.reactedCount * 100f / status.interventionCount.coerceAtLeast(1)).toInt()}%",
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
private fun DigitalNormalStateCard(
    hasSelectedApps: Boolean,
    thresholdMinutes: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Column(
            modifier = Modifier.padding(HabitSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HabitSpacing.xs),
        ) {
            Text(
                text = "오늘 디지털 사용이 적당해요.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Text(
                text = if (hasSelectedApps) {
                    "기준 시간 ${formatMinutes(thresholdMinutes)}을 넘으면 쉬어갈 수 있게 알려드릴게요."
                } else {
                    "관리 앱을 선택하면 사용 시간이 기준을 넘었을 때 개입할 수 있어요."
                },
                style = MaterialTheme.typography.bodySmall,
                color = HabitTextSecondary,
            )
        }
    }
}

@Composable
private fun DigitalActionCard(
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
                Text("5분 쉬기", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStretch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = DigitalContainer),
            ) {
                Text("스트레칭하기", color = DigitalPrimary, fontWeight = FontWeight.Bold)
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
