// 경로: com/example/habittracker/ui/meal/MealInputScreen.kt
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
import com.example.habittracker.ui.theme.MealBackground
import com.example.habittracker.ui.theme.MealPrimary
import com.example.habittracker.ui.theme.MealSurface

@Composable
fun MealInputScreen(
    navController: NavController,
    viewModel: MealViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarVm: SharedAvatarViewModel = hiltViewModel()
    val avatarUiState by avatarVm.uiState.collectAsStateWithLifecycle()
    val status = uiState.todayStatus

    // 취소 팝업 다이얼로그 타겟 로그 ID 관리
    var cancelLogId by remember { mutableStateOf<Long?>(null) }

    val completedCount = status?.let {
        listOf(it.breakfastLogged, it.lunchLogged, it.dinnerLogged).count { v -> v }
    } ?: 0

    val speech = when {
        completedCount >= 3 -> "오늘 식사를 잘 챙겼어요!\n균형 잡힌 하루예요 🍽️"
        completedCount == 0 -> "아직 식사 기록이 없어요.\n가볍게라도 챙겨볼까요? 🍽️"
        else -> "식사를 ${completedCount}번 드셨군요.\n조금 더 챙겨봐요! 😊"
    }

    // 딥링크 개입 팝업 상태 관리 (배달앱 감지 유입 시)
    val navBackStackEntry = navController.currentBackStackEntry
    val deepLinkType = navBackStackEntry?.arguments?.getString("type")
    val deepLinkSource = navBackStackEntry?.arguments?.getString("source")

    var showInterventionDialog by remember(deepLinkType, deepLinkSource) {
        mutableStateOf(deepLinkType == "LATE_NIGHT" && deepLinkSource == "delivery_app")
    }

    if (showInterventionDialog) {
        AlertDialog(
            onDismissRequest = { showInterventionDialog = false },
            title = { Text("야식의 유혹 ⚠️", fontWeight = FontWeight.Bold) },
            text = { Text("배달앱 실행이 감지되었습니다. 야식 대신 시원한 물 한 잔을 마시거나 가벼운 스트레칭으로 몸을 깨워보는 건 어떨까요?") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showInterventionDialog = false
                            navController.navigate("water?source=late_night_intervention")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MealPrimary)
                    ) {
                        Text("💧 물 마시러 가기")
                    }
                    Button(
                        onClick = {
                            showInterventionDialog = false
                            navController.navigate("stretch?trigger=late_night_intervention")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MealPrimary)
                    ) {
                        Text("🧘 스트레칭 하러 가기")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showInterventionDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("그냥 기록할래요")
                }
            }
        )
    }

    // 취소 확인 팝업
    if (cancelLogId != null) {
        AlertDialog(
            onDismissRequest = { cancelLogId = null },
            title = { Text("식사 기록 취소") },
            text = { Text("이 기록을 취소하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        cancelLogId?.let { viewModel.onCancelMealClick(it) }
                        cancelLogId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MealPrimary)
                ) {
                    Text("예")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { cancelLogId = null }) {
                    Text("아니오")
                }
            }
        )
    }

    CategoryScaffold(
        category = HabitCategoryStyle.MEAL,
        title = "해빗프렌즈",
        speech = speech,
        avatarUiState = avatarUiState,
        onSettingsClick = { navController.navigate("settings") },
        onReportsClick = { navController.navigate("reports") },
    ) {
        MealStatusCard(status = status)
        MealQuickLogCard(
            todayLogs = uiState.todayLogs,
            onMealClick = { type ->
                val log = uiState.todayLogs.firstOrNull { it.type == type }
                if (log != null) {
                    cancelLogId = log.id
                } else {
                    viewModel.onMealButtonClick(type)
                }
            },
            onLateNight = {
                val log = uiState.todayLogs.firstOrNull { it.type == MealType.LATE_NIGHT || it.isLateNight }
                if (log != null) {
                    cancelLogId = log.id
                } else {
                    viewModel.onLateNightClick("manual", "direct")
                }
            },
            onBack = { navController.popBackStack() },
        )
        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun MealStatusCard(status: MealTodayStatus?) {
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
                        text = "카테고리",
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
                MealBadge(label = "아침", emoji = "🌅", checked = status?.breakfastLogged == true)
                MealBadge(label = "점심", emoji = "☀️", checked = status?.lunchLogged == true)
                MealBadge(label = "저녁", emoji = "🌙", checked = status?.dinnerLogged == true)
                MealBadge(label = "야식", emoji = "🌙", checked = status?.lateNightLogged == true)
            }
        }
    }
}

@Composable
private fun MealBadge(label: String, emoji: String, checked: Boolean) {
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
                    Text(emoji, style = MaterialTheme.typography.bodyMedium)
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
    onMealClick: (MealType) -> Unit,
    onLateNight: () -> Unit,
    onBack: () -> Unit,
) {
    val breakfastLog = todayLogs.any { it.type == MealType.BREAKFAST }
    val lunchLog = todayLogs.any { it.type == MealType.LUNCH }
    val dinnerLog = todayLogs.any { it.type == MealType.DINNER }

    val lateNightLog = todayLogs.any { it.type == MealType.LATE_NIGHT || it.isLateNight }

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
                text = "어떤 끼니를 드셨나요?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
            ) {
                MealTypeButton("아침", breakfastLog, Modifier.weight(1f)) { onMealClick(MealType.BREAKFAST) }
                MealTypeButton("점심", lunchLog, Modifier.weight(1f)) { onMealClick(MealType.LUNCH) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
            ) {
                MealTypeButton("저녁", dinnerLog, Modifier.weight(1f)) { onMealClick(MealType.DINNER) }
                MealTypeButton("야식", lateNightLog, Modifier.weight(1f)) { onLateNight() }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HabitRadius.button),
                    border = BorderStroke(1.dp, HabitLineGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTextSecondary),
                ) {
                    Text("나중에", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun MealTypeButton(
    label: String,
    completed: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (completed) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(HabitRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = MealPrimary),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(HabitRadius.button),
            border = BorderStroke(1.dp, MealPrimary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MealPrimary),
        ) {
            Text(label, fontWeight = FontWeight.Medium)
        }
    }
}
