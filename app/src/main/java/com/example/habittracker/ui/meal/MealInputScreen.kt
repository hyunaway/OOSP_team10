package com.example.habittracker.ui.meal

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.usecase.meal.MealInterventionIntensity
import com.example.habittracker.ui.avatar.AvatarState
import com.example.habittracker.ui.avatar.SharedAvatarViewModel
import com.example.habittracker.ui.avatar.forCategory
import com.example.habittracker.ui.components.CategoryScaffold
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitCategoryStyle
import com.example.habittracker.ui.theme.HabitElevation
import com.example.habittracker.ui.theme.HabitLineGray
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary
import com.example.habittracker.ui.theme.MealBackground
import com.example.habittracker.ui.theme.MealPrimary
import java.time.LocalDate

@Composable
fun MealInputScreen(
    navController: NavController,
    viewModel: MealViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarVm: SharedAvatarViewModel = hiltViewModel()
    val avatarUiState by avatarVm.uiState.collectAsStateWithLifecycle()
    val status = uiState.todayStatus

    var showCancelDialog by remember { mutableStateOf(false) }
    var showMissedMealDialog by remember { mutableStateOf(false) }
    val latestLog = uiState.displayLogs.firstOrNull()
    val visibleMessage = uiState.transientMessage
        ?: uiState.classificationMessage
        ?: currentMealSpeech(uiState)
        ?: uiState.dailyStatusMessage

    val navBackStackEntry = navController.currentBackStackEntry
    val deepLinkType = navBackStackEntry?.arguments?.getString("type")
    val deepLinkSource = navBackStackEntry?.arguments?.getString("source")
    var showInterventionDialog by remember(deepLinkType, deepLinkSource) {
        mutableStateOf(deepLinkType == "LATE_NIGHT" && deepLinkSource == "delivery_app")
    }

    if (showInterventionDialog) {
        AlertDialog(
            onDismissRequest = { showInterventionDialog = false },
            title = { Text("야식 감지", fontWeight = FontWeight.Bold) },
            text = { Text("배달앱 실행이 감지되었어요. 물을 마시거나 가벼운 스트레칭으로 몸을 깨워볼까요?") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            showInterventionDialog = false
                            navController.navigate("water?source=late_night_intervention")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MealPrimary),
                    ) {
                        Text("물 마시러 가기")
                    }
                    Button(
                        onClick = {
                            showInterventionDialog = false
                            navController.navigate("stretch?trigger=late_night_intervention")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MealPrimary),
                    ) {
                        Text("스트레칭 하러 가기")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showInterventionDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("그냥 기록할래요")
                }
            },
        )
    }

    if (showCancelDialog && latestLog != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("최근 식사 기록 취소") },
            text = {
                Text("${viewModel.formatMealTime(latestLog)} ${viewModel.mealLabel(latestLog)} 기록을 취소할까요?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelLatestMealLog()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MealPrimary),
                ) {
                    Text("취소하기")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("닫기")
                }
            },
        )
    }

    if (showMissedMealDialog) {
        MissedMealDialog(
            onDismiss = { showMissedMealDialog = false },
            onConfirm = { time ->
                viewModel.onMissedMealTimeSelected(time)
                showMissedMealDialog = false
            },
        )
    }

    CategoryScaffold(
        category = HabitCategoryStyle.MEAL,
        title = "식사",
        speech = visibleMessage ?: "현재 시간과 생활 패턴에 맞춰 식사 리듬을 챙겨봐요.",
        avatarUiState = avatarUiState.forCategory(AvatarState.MEAL_LACK),
        onSettingsClick = { navController.navigate("settings") },
        onReportsClick = { navController.navigate("reports") },
    ) {
        MealStatusCard(
            status = status,
            dailyStatusMessage = uiState.dailyStatusMessage,
            mealPlanMessage = uiState.mealPlanMessage,
            expectedMealCount = uiState.expectedMealCount,
            completedExpectedMealCount = uiState.completedExpectedMealCount,
            additionalIntakeCount = uiState.additionalIntakeCount,
            hasIrregularIntake = uiState.hasIrregularIntake,
        )
        MealQuickLogCard(
            todayLogs = uiState.displayLogs,
            logsExpanded = uiState.mealLogsExpanded,
            latestLog = latestLog,
            onRecordMeal = { viewModel.onAutoMealRecordClick() },
            onToggleLogs = { viewModel.toggleMealLogsExpanded() },
            onAddMissedMeal = { showMissedMealDialog = true },
            onCancelLatest = { showCancelDialog = true },
            mealLabel = viewModel::mealLabel,
            mealTime = viewModel::formatMealTime,
        )
        visibleMessage?.let { msg ->
            Text(text = msg, color = MealPrimary, modifier = Modifier.padding(16.dp))
        }
        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
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

private fun currentMealSpeech(uiState: MealUiState): String? {
    val mealName = uiState.currentMealWindowType?.let { mealTypeLabel(it) }
    return when {
        uiState.currentActionableMealType != null &&
            uiState.currentMealInterventionIntensity == MealInterventionIntensity.SOFT -> {
            "오늘은 식사 리듬을 조금 여유 있게 볼게요. 가볍게 챙길 수 있을 때만 챙겨요."
        }
        uiState.currentActionableMealType != null -> {
            "아직 ${mealTypeLabel(uiState.currentActionableMealType)}을 챙기지 않았어요. 가볍게 챙겨볼까요? 🍽️"
        }
        uiState.currentMealInterventionReason == "already_completed" && mealName != null -> {
            "$mealName 잘 챙겼어요. 다음 식사 리듬도 천천히 이어가요."
        }
        uiState.currentMealInterventionReason == "not_in_meal_window" -> {
            "오늘 식사 기록은 잘 저장되고 있어요. 다음 식사 때 다시 챙겨볼게요."
        }
        else -> null
    }
}

private fun mealTypeLabel(type: MealType): String =
    when (type) {
        MealType.BREAKFAST -> "아침"
        MealType.LUNCH -> "점심"
        MealType.DINNER -> "저녁"
        MealType.LATE_NIGHT -> "야식"
    }

@Composable
private fun MealStatusCard(
    status: MealTodayStatus?,
    dailyStatusMessage: String?,
    mealPlanMessage: String?,
    expectedMealCount: Int,
    completedExpectedMealCount: Int,
    additionalIntakeCount: Int,
    hasIrregularIntake: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Column(modifier = Modifier.padding(HabitSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MealBackground,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🍽️", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(modifier = Modifier.width(HabitSpacing.sm))
                Column {
                    Text(
                        text = "식사",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HabitTextPrimary,
                    )
                    Text(
                        text = "오늘 식사 상태",
                        style = MaterialTheme.typography.bodySmall,
                        color = HabitTextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(HabitSpacing.base))
            HorizontalDivider(color = HabitLineGray)
            Spacer(modifier = Modifier.height(HabitSpacing.base))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MealBadge(label = "아침", checked = status?.breakfastLogged == true)
                MealBadge(label = "점심", checked = status?.lunchLogged == true)
                MealBadge(label = "저녁", checked = status?.dinnerLogged == true)
                MealBadge(label = "야식", checked = status?.lateNightLogged == true)
            }
            mealPlanMessage?.let {
                Spacer(modifier = Modifier.height(HabitSpacing.base))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
            }
            if (expectedMealCount > 0) {
                Spacer(modifier = Modifier.height(HabitSpacing.xxs))
                Text(
                    text = "오늘 계획: $completedExpectedMealCount/$expectedMealCount 완료",
                    style = MaterialTheme.typography.labelSmall,
                    color = HabitTextSecondary,
                )
            }
            if (additionalIntakeCount > 0) {
                Spacer(modifier = Modifier.height(HabitSpacing.xxs))
                Text(
                    text = if (hasIrregularIntake) {
                        "오늘은 식사가 여러 번 나뉘어 기록됐어요. 리듬만 가볍게 맞춰요."
                    } else {
                        "추가로 챙긴 식사는 완료 횟수와 분리해서 볼게요."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = HabitTextSecondary,
                )
            }
            dailyStatusMessage?.let {
                Spacer(modifier = Modifier.height(HabitSpacing.base))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun MealBadge(label: String, checked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (checked) MealPrimary else HabitLineGray,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "$label 완료",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(label.take(1), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(HabitSpacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) MealPrimary else HabitTextSecondary,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun MealQuickLogCard(
    todayLogs: List<MealLogEntity>,
    logsExpanded: Boolean,
    latestLog: MealLogEntity?,
    onRecordMeal: () -> Unit,
    onToggleLogs: () -> Unit,
    onAddMissedMeal: () -> Unit,
    onCancelLatest: () -> Unit,
    mealLabel: (MealLogEntity) -> String,
    mealTime: (MealLogEntity) -> String,
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
                text = "식사를 기록할까요?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Text(
                text = "현재 시간과 생활 패턴을 기준으로 자동으로 판단해요.",
                style = MaterialTheme.typography.bodySmall,
                color = HabitTextSecondary,
            )
            Button(
                onClick = onRecordMeal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = MealPrimary),
            ) {
                Text("식사 기록하기", fontWeight = FontWeight.Bold, color = Color.White)
            }
            OutlinedButton(
                onClick = onAddMissedMeal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                border = BorderStroke(1.dp, MealPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MealPrimary),
            ) {
                Text("놓친 기록 추가", fontWeight = FontWeight.Medium)
            }
            latestLog?.let { log ->
                Text(
                    text = "최근 기록: ${mealLabel(log)} · ${mealTime(log)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
                OutlinedButton(
                    onClick = onCancelLatest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HabitRadius.button),
                    border = BorderStroke(1.dp, HabitLineGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTextSecondary),
                ) {
                    Text("최근 기록 취소하기", fontWeight = FontWeight.Medium)
                }
            }
            OutlinedButton(
                onClick = onToggleLogs,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                border = BorderStroke(1.dp, HabitLineGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTextSecondary),
            ) {
                Text(
                    text = if (logsExpanded) "식사 기록 접기" else "식사 기록 펼치기",
                    fontWeight = FontWeight.Medium,
                )
            }
            if (logsExpanded) {
                MealLogList(
                    logs = todayLogs,
                    mealLabel = mealLabel,
                    mealTime = mealTime,
                )
            }
        }
    }
}

@Composable
private fun MealLogList(
    logs: List<MealLogEntity>,
    mealLabel: (MealLogEntity) -> String,
    mealTime: (MealLogEntity) -> String,
) {
    if (logs.isEmpty()) {
        Text(
            text = "아직 오늘 식사 기록이 없어요.",
            style = MaterialTheme.typography.bodySmall,
            color = HabitTextSecondary,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(HabitSpacing.xs)) {
        logs.forEach { log ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${mealLabel(log)} · ${mealTime(log)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = HabitTextPrimary,
                    )
                    if (log.mealDate != LocalDate.now().toString()) {
                        Text(
                            text = "전날 야식으로 기록됨",
                            style = MaterialTheme.typography.labelSmall,
                            color = HabitTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissedMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var timeText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("놓친 기록 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HabitSpacing.sm)) {
                Text(
                    text = "오늘 먹은 시간을 HH:mm 형식으로 입력해주세요. 분류는 앱이 자동으로 판단해요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("예: 14:30") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timeText) },
                colors = ButtonDefaults.buttonColors(containerColor = MealPrimary),
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
    )
}
