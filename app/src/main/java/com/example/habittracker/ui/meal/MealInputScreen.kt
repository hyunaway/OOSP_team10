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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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
import androidx.compose.ui.platform.LocalContext
import com.example.habittracker.widget.WidgetUpdateHelper

@Composable
fun MealInputScreen(
    navController: NavController,
    viewModel: MealViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarVm: SharedAvatarViewModel = hiltViewModel()
    val avatarUiState by avatarVm.uiState.collectAsStateWithLifecycle()
    val status = uiState.todayStatus
    val context = LocalContext.current

    val completedCount = status?.let {
        listOf(it.breakfastLogged, it.lunchLogged, it.dinnerLogged).count { v -> v }
    } ?: 0

    val speech = when {
        completedCount >= 3 -> "오늘 식사를 잘 챙겼어요!\n균형 잡힌 하루예요 🍽️"
        completedCount == 0 -> "아직 식사 기록이 없어요.\n가볍게라도 챙겨볼까요? 🍽️"
        else -> "식사를 ${completedCount}번 드셨군요.\n조금 더 챙겨봐요! 😊"
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
            onMealClick = {
                viewModel.onMealButtonClick(it)
                // TODO: 식사 부족 판정 로직 병합 후 MealStatus.LACK 연결
                WidgetUpdateHelper.updateAllWidgetsSync(context)
            },
            onLateNight = {
                viewModel.onLateNightButtonClick(true)
                WidgetUpdateHelper.updateAllWidgetsSync(context)
            },
            onBack = { navController.popBackStack() },
        )
        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
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
    onMealClick: (MealType) -> Unit,
    onLateNight: () -> Unit,
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
                text = "어떤 끼니를 드셨나요?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
            ) {
                MealTypeButton("아침", Modifier.weight(1f)) { onMealClick(MealType.BREAKFAST) }
                MealTypeButton("점심", Modifier.weight(1f)) { onMealClick(MealType.LUNCH) }
                MealTypeButton("저녁", Modifier.weight(1f)) { onMealClick(MealType.DINNER) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
            ) {
                MealTypeButton("간식", Modifier.weight(1f)) { onMealClick(MealType.SNACK) }
                MealTypeButton("나중에", Modifier.weight(1f), onClick = onBack)
            }
            Spacer(modifier = Modifier.height(HabitSpacing.xs))
            Button(
                onClick = onLateNight,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = MealSurface),
            ) {
                Text(
                    text = "🌙 야식 먹었어요",
                    color = HabitTextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MealTypeButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
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
