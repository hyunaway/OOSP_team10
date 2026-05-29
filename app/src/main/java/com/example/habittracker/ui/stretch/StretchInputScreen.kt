// 경로: com/example/habittracker/ui/stretch/StretchInputScreen.kt
package com.example.habittracker.ui.stretch

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AlertDialog
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.habittracker.data.local.UserPreferenceManager
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
import com.example.habittracker.ui.theme.StretchBackground
import com.example.habittracker.ui.theme.StretchPrimary
import androidx.compose.ui.platform.LocalContext
import com.example.habittracker.widget.WidgetUpdateHelper

@Composable
fun StretchInputScreen(
    navController: NavController,
    viewModel: StretchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarVm: SharedAvatarViewModel = hiltViewModel()
    val avatarUiState by avatarVm.uiState.collectAsStateWithLifecycle()
    val status = uiState.todayStatus
    val context = LocalContext.current

    // 토스트 및 팝업 상태 관찰
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val cancelConfirmRecord by viewModel.showCancelConfirmPopup.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (toastMsg != null) {
        android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
        viewModel.clearToastMessage()
    }

    // 자정 날짜 변경 감지 시 자동 갱신 리시버 등록 (수정 4 요건)
    androidx.compose.runtime.DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.content.Intent.ACTION_DATE_CHANGED) {
                    viewModel.refreshData()
                }
            }
        }
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_DATE_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // 단일 기록 취소 다이얼로그 (수정 1 요건)
    if (cancelConfirmRecord != null) {
        val record = cancelConfirmRecord!!
        AlertDialog(
            onDismissRequest = { viewModel.setShowCancelConfirmPopup(null) },
            title = { Text("기록 취소", fontWeight = FontWeight.Bold) },
            text = { Text("이 기록을 취소하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStretchRecordBySlot(record.timeSlot)
                        viewModel.setShowCancelConfirmPopup(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StretchPrimary)
                ) {
                    Text("예")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.setShowCancelConfirmPopup(null) }) {
                    Text("아니오")
                }
            }
        )
    }

    val speech = when {
        (status?.totalCount ?: 0) == 0 -> "어깨가 굳어 있어요.\n잠깐 풀어볼까요? 🧘"
        (status?.totalCount ?: 0) >= 3 -> "오늘 스트레칭을 잘 챙겼어요!\n몸이 가뿐하죠? 🧘"
        else -> "좋아요! 조금 더 해볼까요?\n몸도 마음도 가볍게! 🧘"
    }

    CategoryScaffold(
        category = HabitCategoryStyle.STRETCH,
        title = "해빗프렌즈",
        speech = speech,
        avatarUiState = avatarUiState,
        onSettingsClick = { navController.navigate("settings") },
        onReportsClick = { navController.navigate("reports") },
    ) {
        // 수정 8: 진행률 위젯 (퍼센트% 라벨 포함)
        StretchProgressWidget(
            todayCount = uiState.todayCount,
            isHalfGoalAchieved = uiState.isHalfGoalAchieved
        )
        
        StretchStatusCard(uiState = uiState)
        
        StretchSlotsCard(
            uiState = uiState,
            viewModel = viewModel,
            userPreferenceManager = viewModel.userPreferenceManager
        )
        
        // 부위 선택 그리드를 지우고 "나중에" 버튼만 하단에 배치
        Spacer(modifier = Modifier.height(HabitSpacing.sm))
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HabitRadius.button),
        ) {
            Text("나중에", color = HabitTextSecondary)
        }

        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
    }
}

// 수정 8 & 수정 3 요건: 진행률 퍼센트(%) 위젯 구현
@Composable
private fun StretchProgressWidget(todayCount: Int, isHalfGoalAchieved: Boolean) {
    val progressTarget = (todayCount * 0.25f).coerceIn(0f, 1f)
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500),
        label = "stretchProgressAnimation"
    )
    val percentage = (progressTarget * 100).toInt()
    val isGoalAchieved = todayCount >= 4
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = HabitSpacing.sm),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Column(modifier = Modifier.padding(HabitSpacing.base)) {
            if (isHalfGoalAchieved) {
                Text(
                    text = "오늘 스트레칭 목표를 달성하셨어요! ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = StretchPrimary,
                    modifier = Modifier.padding(bottom = HabitSpacing.xs)
                )
            }
            Text(
                text = if (isGoalAchieved) "오늘 목표 달성! 🎉 ($percentage%)" else "오늘 스트레칭 $todayCount / 4회 ($percentage%)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isGoalAchieved) StretchPrimary else HabitTextPrimary
            )
            Spacer(modifier = Modifier.height(HabitSpacing.xs))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (isGoalAchieved) StretchPrimary else StretchPrimary.copy(alpha = 0.6f),
                trackColor = StretchBackground,
            )
        }
    }
}

@Composable
private fun StretchStatusCard(uiState: StretchUiState) {
    val status = uiState.todayStatus
    val streak = uiState.streak
    val todayCount = uiState.todayCount

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
                        color = StretchBackground,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🧘", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(HabitSpacing.sm))
                    Column {
                        Text(
                            text = "스트레칭",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HabitTextPrimary,
                        )
                        Text(
                            text = "몸도 마음도 가볍게, 오늘도 스트레칭!",
                            style = MaterialTheme.typography.bodySmall,
                            color = HabitTextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(HabitSpacing.base))
            HorizontalDivider(color = HabitLineGray)
            Spacer(modifier = Modifier.height(HabitSpacing.base))

            if (status != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "현재 상태",
                            style = MaterialTheme.typography.bodySmall,
                            color = HabitTextSecondary,
                        )
                        Text(
                            text = "오늘 ${todayCount}회 완료",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StretchPrimary,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(HabitRadius.full),
                        color = StretchBackground,
                    ) {
                        Text(
                            text = "연속 ${streak}일째",
                            modifier = Modifier.padding(
                                horizontal = HabitSpacing.sm,
                                vertical = HabitSpacing.xxs,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = StretchPrimary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                val animatedHealthScore by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = status.avatarHealthScore.coerceIn(0f, 1f),
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 500),
                    label = "stretchHealthScoreAnimation"
                )
                LinearProgressIndicator(
                    progress = { animatedHealthScore },
                    modifier = Modifier.fillMaxWidth(),
                    color = StretchPrimary,
                    trackColor = StretchBackground,
                )
                Spacer(modifier = Modifier.height(HabitSpacing.xs))
                Text(
                    text = "건강 점수 ${(status.avatarHealthScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
            } else {
                Text("데이터 로딩 중...", color = HabitTextSecondary)
            }
        }
    }
}

@Composable
private fun StretchSlotsCard(
    uiState: StretchUiState,
    viewModel: StretchViewModel,
    userPreferenceManager: UserPreferenceManager,
) {
    val amEnabled by userPreferenceManager.stretchSlotAmEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val pmEnabled by userPreferenceManager.stretchSlotPmEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val eveEnabled by userPreferenceManager.stretchSlotEveEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val nightEnabled by userPreferenceManager.stretchSlotNightEnabledFlow.collectAsStateWithLifecycle(initialValue = true)

    val scope = rememberCoroutineScope()

    val slots = listOf(
        Triple("오전 (10:30)", "아침", amEnabled),
        Triple("점심 후 (14:00)", "점심", pmEnabled),
        Triple("저녁 (19:30)", "저녁", eveEnabled),
        Triple("취침 전 (22:00)", "기타", nightEnabled)
    )

    val activeSlotsCount = slots.count { it.third }
    val completedCount = uiState.todayCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Column(modifier = Modifier.padding(HabitSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "시간대별 스트레칭 슬롯",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HabitTextPrimary,
                )
                Text(
                    text = "달성도: $completedCount/$activeSlotsCount",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = StretchPrimary,
                )
            }
            Spacer(modifier = Modifier.height(HabitSpacing.sm))

            slots.forEach { (label, key, enabled) ->
                val buttonState = uiState.buttonStates[key] ?: StretchButtonState.INPUTTABLE
                val isCompleted = buttonState == StretchButtonState.DISABLED_COMPLETED

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = HabitTextPrimary)
                            if (isCompleted) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(HabitRadius.full),
                                    color = StretchBackground,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "완료 🧘",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StretchPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (enabled) "🔔 알림 활성화" else "🔕 알림 비활성화",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) StretchPrimary else HabitTextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    when (key) {
                                        "아침" -> userPreferenceManager.updateStretchSlotAmEnabled(!enabled)
                                        "점심" -> userPreferenceManager.updateStretchSlotPmEnabled(!enabled)
                                        "저녁" -> userPreferenceManager.updateStretchSlotEveEnabled(!enabled)
                                        "기타" -> userPreferenceManager.updateStretchSlotNightEnabled(!enabled)
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp),
                            border = BorderStroke(1.dp, if (enabled) StretchPrimary else HabitLineGray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (enabled) StretchPrimary else HabitTextSecondary)
                        ) {
                            Text(if (enabled) "알림 끄기" else "알림 켜기", style = MaterialTheme.typography.labelSmall)
                        }

                        val isRecorded = (buttonState == StretchButtonState.DISABLED_COMPLETED) || (buttonState == StretchButtonState.EDITABLE)
                        val btnEnabled = isRecorded || (uiState.todayCount < 4)
                        val btnText = if (isRecorded) "완료" else "기록"

                        Button(
                            onClick = { viewModel.handleTimeSlotTap(key) },
                            enabled = btnEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecorded) StretchPrimary else StretchBackground,
                                disabledContainerColor = StretchBackground
                            )
                        ) {
                            Text(
                                text = btnText,
                                color = if (isRecorded) Color.White else StretchPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
