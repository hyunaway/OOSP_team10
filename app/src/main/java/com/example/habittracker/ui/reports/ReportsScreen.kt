// 경로: com/example/habittracker/ui/reports/ReportsScreen.kt
package com.example.habittracker.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.domain.model.DigitalPatternResult
import com.example.habittracker.domain.model.MealPatternResult
import com.example.habittracker.domain.model.MonthlyReportState
import com.example.habittracker.domain.model.StretchPatternResult
import com.example.habittracker.domain.model.WaterPatternResult
import com.example.habittracker.domain.model.WeeklyReportState
import com.example.habittracker.ui.theme.HabitBackground
import com.example.habittracker.ui.theme.HabitCardWhite
import com.example.habittracker.ui.theme.HabitCategoryStyle
import com.example.habittracker.ui.theme.HabitDeepMint
import com.example.habittracker.ui.theme.HabitElevation
import com.example.habittracker.ui.theme.HabitLineGray
import com.example.habittracker.ui.theme.HabitRadius
import com.example.habittracker.ui.theme.HabitSpacing
import com.example.habittracker.ui.theme.HabitTextPrimary
import com.example.habittracker.ui.theme.HabitTextSecondary

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val weeklyState by viewModel.weeklyState.collectAsStateWithLifecycle()
    val monthlyState by viewModel.monthlyState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) viewModel.loadWeekly()
        else viewModel.loadMonthly()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HabitSpacing.base),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "리포트",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
        }

        Card(
            modifier = Modifier
                .padding(horizontal = HabitSpacing.base)
                .fillMaxWidth(),
            shape = RoundedCornerShape(HabitRadius.button),
            colors = CardDefaults.cardColors(containerColor = HabitLineGray),
            elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.none),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(HabitSpacing.xxs),
            ) {
                SegmentTab(
                    label = "주간",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f),
                )
                SegmentTab(
                    label = "월간",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(HabitSpacing.base))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HabitSpacing.base),
            verticalArrangement = Arrangement.spacedBy(HabitSpacing.md),
        ) {
            if (selectedTab == 0) {
                WeeklyContent(weeklyState)
            } else {
                MonthlyContent(monthlyState)
            }
            Spacer(modifier = Modifier.height(HabitSpacing.xl))
        }
    }
}

@Composable
private fun SegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(HabitRadius.button),
        color = if (selected) HabitCardWhite else Color.Transparent,
        shadowElevation = if (selected) HabitElevation.xs else HabitElevation.none,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = HabitSpacing.sm),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) HabitTextPrimary else HabitTextSecondary,
            )
        }
    }
}

@Composable
private fun ReportLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HabitSpacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            Text("데이터를 불러오는 중...", color = HabitTextSecondary)
        }
    }
}

@Composable
private fun ReportSummaryCard(
    periodLabel: String,
    overallRate: Float,
    isWeekly: Boolean,
) {
    val period = if (isWeekly) "주" else "달"
    val message = when {
        overallRate >= 0.8f -> "이번 ${period}도 훌륭해요!\n꾸준함이 최고의 습관이에요 🌟"
        overallRate >= 0.5f -> "절반 이상 달성했어요!\n조금만 더 힘내봐요 💪"
        overallRate > 0f -> "이번 ${period} 리듬을 확인해봤어요.\n작은 습관부터 다시 시작해봐요 🌱"
        else -> "기록을 시작해봐요!\n하루 한 번씩이면 충분해요 😊"
    }

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
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HabitTextPrimary,
                )
                Surface(
                    shape = RoundedCornerShape(HabitRadius.full),
                    color = HabitLineGray,
                ) {
                    Text(
                        text = "전체 ${(overallRate * 100).toInt()}%",
                        modifier = Modifier.padding(
                            horizontal = HabitSpacing.sm,
                            vertical = HabitSpacing.xxs,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HabitDeepMint,
                    )
                }
            }
            Spacer(modifier = Modifier.height(HabitSpacing.sm))
            LinearProgressIndicator(
                progress = { overallRate.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = HabitDeepMint,
                trackColor = HabitLineGray,
            )
            Spacer(modifier = Modifier.height(HabitSpacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = HabitTextSecondary,
            )
        }
    }
}

@Composable
private fun CategoryAchievementCard(
    category: HabitCategoryStyle,
    achievementRate: Float,
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
                        shape = RoundedCornerShape(HabitRadius.sm),
                        color = category.backgroundColor,
                    ) {
                        Text(
                            text = category.emoji,
                            modifier = Modifier.padding(HabitSpacing.xs),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Spacer(modifier = Modifier.width(HabitSpacing.sm))
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = HabitTextPrimary,
                    )
                }
                Text(
                    text = "${(achievementRate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = category.primaryColor,
                )
            }
            Spacer(modifier = Modifier.height(HabitSpacing.sm))
            LinearProgressIndicator(
                progress = { achievementRate.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = category.primaryColor,
                trackColor = category.backgroundColor,
            )
        }
    }
}

@Composable
private fun InsightsCard(
    waterPattern: WaterPatternResult?,
    mealPattern: MealPatternResult?,
    digitalPattern: DigitalPatternResult?,
    stretchPattern: StretchPatternResult?,
) {
    val insights = mutableListOf<String>()

    waterPattern?.let { p ->
        if (p.peakHours.isNotEmpty()) {
            insights.add("💧 물은 ${p.peakHours.first()}시 전후로 가장 많이 마셨어요.")
        }
    }
    mealPattern?.let { p ->
        p.lateNightRiskHour?.let { h ->
            insights.add("🌙 ${h}시 이후 야식 패턴이 감지됐어요. 주의해봐요!")
        }
        val mostSkipped = p.skippedMealPattern.maxByOrNull { it.value }
        if (mostSkipped != null && mostSkipped.value > 0) {
            insights.add("🍽️ ${mostSkipped.key} 식사를 자주 건너뛰고 있어요.")
        }
    }
    digitalPattern?.let { p ->
        if (p.bedtimeUsageScore > 0.5f) {
            insights.add("📱 취침 전 디지털 사용이 많아요. 수면에 영향을 줄 수 있어요.")
        } else if (p.peakUsageHours.isNotEmpty()) {
            insights.add("📱 ${p.peakUsageHours.first()}시 전후로 디지털 사용이 가장 많아요.")
        }
    }
    stretchPattern?.let { p ->
        p.preferredBodyPart?.let { part ->
            insights.add("🧘 스트레칭은 주로 $part 부위를 많이 했어요.")
        }
        if (p.digitalTriggerConversionRate > 0.3f) {
            insights.add("🧘 디지털 알림 후 스트레칭 전환율이 ${(p.digitalTriggerConversionRate * 100).toInt()}%예요!")
        }
    }

    if (insights.isEmpty()) {
        insights.add("💡 꾸준히 기록하면 더 정확한 패턴 분석이 가능해요.")
    }

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
                text = "해빗프렌즈의 작은 인사이트",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            HorizontalDivider(color = HabitLineGray)
            insights.forEach { insight ->
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodySmall,
                    color = HabitTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun WeeklyContent(state: WeeklyReportState?) {
    if (state == null) {
        ReportLoadingCard()
        return
    }

    ReportSummaryCard(
        periodLabel = state.weekLabel,
        overallRate = state.overallAchievementRate,
        isWeekly = true,
    )

    val waterRate = if (state.dailyWaterSummaries.isNotEmpty())
        state.dailyWaterSummaries.map { it.achievementRate }.average().toFloat()
    else 0f

    val mealRate = if (state.dailyMealSummaries.isNotEmpty()) {
        state.dailyMealSummaries.map { summary ->
            val logged = summary.mealMap.values.count { it }
            (logged.toFloat() / 3f).coerceIn(0f, 1f)
        }.average().toFloat()
    } else 0f

    val digitalRate = if (state.dailyDigitalSummaries.isNotEmpty()) {
        state.dailyDigitalSummaries.map { summary ->
            val total = summary.interventionCount.coerceAtLeast(1).toFloat()
            (summary.reactedCount.toFloat() / total).coerceIn(0f, 1f)
        }.average().toFloat()
    } else 0f

    val stretchRate = if (state.dailyStretchSummaries.isNotEmpty()) {
        state.dailyStretchSummaries.map { summary ->
            (summary.count.toFloat() / 3f).coerceIn(0f, 1f)
        }.average().toFloat()
    } else 0f

    CategoryAchievementCard(category = HabitCategoryStyle.MEAL, achievementRate = mealRate)
    CategoryAchievementCard(category = HabitCategoryStyle.WATER, achievementRate = waterRate)
    CategoryAchievementCard(category = HabitCategoryStyle.DIGITAL, achievementRate = digitalRate)
    CategoryAchievementCard(category = HabitCategoryStyle.STRETCH, achievementRate = stretchRate)

    InsightsCard(
        waterPattern = state.waterPattern,
        mealPattern = state.mealPattern,
        digitalPattern = state.digitalPattern,
        stretchPattern = state.stretchPattern,
    )
}

@Composable
private fun MonthlyContent(state: MonthlyReportState?) {
    if (state == null) {
        ReportLoadingCard()
        return
    }

    ReportSummaryCard(
        periodLabel = state.monthLabel,
        overallRate = state.overallAchievementRate,
        isWeekly = false,
    )

    if (state.weeklySnapshots.isNotEmpty()) {
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
                    text = "주차별 달성률",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HabitTextPrimary,
                )
                HorizontalDivider(color = HabitLineGray)
                state.weeklySnapshots.forEach { snapshot ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HabitSpacing.sm),
                    ) {
                        Text(
                            text = snapshot.weekLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = HabitTextSecondary,
                            modifier = Modifier.width(60.dp),
                        )
                        LinearProgressIndicator(
                            progress = { snapshot.achievementRate.coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f),
                            color = HabitDeepMint,
                            trackColor = HabitLineGray,
                        )
                        Text(
                            text = "${(snapshot.achievementRate * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HabitTextPrimary,
                        )
                    }
                }
            }
        }
    }

    InsightsCard(
        waterPattern = state.waterPattern,
        mealPattern = state.mealPattern,
        digitalPattern = state.digitalPattern,
        stretchPattern = state.stretchPattern,
    )
}
