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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.domain.model.StretchTodayStatus
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
import com.example.habittracker.ui.theme.StretchContainer
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
        StretchStatusCard(status = status)
        StretchBodyPartCard(
            onBodyPart = {
                viewModel.onStretchButtonClick(it)
                // TODO: 스트레칭 부족 판정 로직 병합 후 StretchStatus.LACK 연결
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
private fun StretchStatusCard(status: StretchTodayStatus?) {
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
                            text = "오늘 ${status.totalCount}회 완료",
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
                            text = "연속 ${status.totalCount}일째",
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
                LinearProgressIndicator(
                    progress = { status.avatarHealthScore.coerceIn(0f, 1f) },
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
private fun StretchBodyPartCard(
    onBodyPart: (BodyPartType) -> Unit,
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
                text = "어느 부위를 스트레칭할까요?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
            ) {
                BodyPartButton("목", Modifier.weight(1f)) { onBodyPart(BodyPartType.NECK) }
                BodyPartButton("어깨", Modifier.weight(1f)) { onBodyPart(BodyPartType.SHOULDER) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
            ) {
                BodyPartButton("허리", Modifier.weight(1f)) { onBodyPart(BodyPartType.BACK) }
                BodyPartButton("전신", Modifier.weight(1f)) { onBodyPart(BodyPartType.FULL) }
            }
            Spacer(modifier = Modifier.height(HabitSpacing.xs))
            Button(
                onClick = { onBodyPart(BodyPartType.FULL) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HabitRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = StretchPrimary),
            ) {
                Text("지금 바로 스트레칭", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun BodyPartButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(HabitRadius.button),
        border = BorderStroke(1.dp, StretchPrimary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StretchPrimary),
    ) {
        Text(label, fontWeight = FontWeight.Medium)
    }
}
