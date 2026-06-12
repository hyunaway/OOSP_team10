// 경로: com/example/habittracker/ui/reports/ReportsScreen.kt
package com.example.habittracker.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val weeklyState by viewModel.weeklyState.collectAsStateWithLifecycle()
    val monthlyState by viewModel.monthlyState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    val currentWeekStart by viewModel.currentWeekStart.collectAsStateWithLifecycle()
    val currentMonthStart by viewModel.currentMonthStart.collectAsStateWithLifecycle()

    // 개인화 데이터
    val waterPersonalizationReady by viewModel.waterPersonalizationReady.collectAsStateWithLifecycle()
    val waterGoalMl by viewModel.waterGoalMl.collectAsStateWithLifecycle()
    val waterReminderIntervalMinutes by viewModel.waterReminderIntervalMinutes.collectAsStateWithLifecycle()
    val waterPeakJson by viewModel.waterPeakJson.collectAsStateWithLifecycle()

    val mealPersonalizationReady by viewModel.mealPersonalizationReady.collectAsStateWithLifecycle()
    val mealBreakfastPeak by viewModel.mealBreakfastPeak.collectAsStateWithLifecycle()
    val mealLunchPeak by viewModel.mealLunchPeak.collectAsStateWithLifecycle()
    val mealDinnerPeak by viewModel.mealDinnerPeak.collectAsStateWithLifecycle()
    val mealLateNightPeak by viewModel.mealLateNightPeak.collectAsStateWithLifecycle()
    val deliveryIntervalDays by viewModel.deliveryIntervalDays.collectAsStateWithLifecycle()

    val stretchPersonalizationReady by viewModel.stretchPersonalizationReady.collectAsStateWithLifecycle()
    val stretchGoalCount by viewModel.stretchGoalCount.collectAsStateWithLifecycle()
    val stretchPreferredTimeSlots by viewModel.stretchPreferredTimeSlots.collectAsStateWithLifecycle()

    val digitalPersonalizationReady by viewModel.digitalPersonalizationReady.collectAsStateWithLifecycle()
    val digitalInterventionThresholdMinutes by viewModel.digitalInterventionThresholdMinutes.collectAsStateWithLifecycle()
    val preferredMessageTone by viewModel.preferredMessageTone.collectAsStateWithLifecycle()

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

        // 탭 제어기
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

        Spacer(modifier = Modifier.height(HabitSpacing.sm))

        // 기간 내비게이션 UI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HabitSpacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {
                if (selectedTab == 0) viewModel.navigateWeek(-1)
                else viewModel.navigateMonth(-1)
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "이전 기간",
                    tint = HabitTextPrimary,
                )
            }

            val formatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }
            val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월") }
            val dateLabel = if (selectedTab == 0) {
                val end = currentWeekStart.plusDays(6)
                "${currentWeekStart.format(formatter)} ~ ${end.format(formatter)}"
            } else {
                currentMonthStart.format(monthFormatter)
            }

            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )

            val isNextEnabled = if (selectedTab == 0) {
                currentWeekStart.plusWeeks(1).isBefore(LocalDate.now().plusDays(1))
            } else {
                currentMonthStart.plusMonths(1).isBefore(LocalDate.now().withDayOfMonth(1).plusDays(1))
            }

            IconButton(
                onClick = {
                    if (selectedTab == 0) viewModel.navigateWeek(1)
                    else viewModel.navigateMonth(1)
                },
                enabled = isNextEnabled
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "다음 기간",
                    tint = if (isNextEnabled) HabitTextPrimary else Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(HabitSpacing.sm))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HabitSpacing.base),
            verticalArrangement = Arrangement.spacedBy(HabitSpacing.md),
        ) {
            if (selectedTab == 0) {
                WeeklyContent(
                    state = weeklyState,
                    currentWeekStart = currentWeekStart,
                    waterPersonalizationReady = waterPersonalizationReady,
                    waterGoalMl = waterGoalMl,
                    waterReminderIntervalMinutes = waterReminderIntervalMinutes,
                    waterPeakJson = waterPeakJson,
                    mealPersonalizationReady = mealPersonalizationReady,
                    mealBreakfastPeak = mealBreakfastPeak,
                    mealLunchPeak = mealLunchPeak,
                    mealDinnerPeak = mealDinnerPeak,
                    mealLateNightPeak = mealLateNightPeak,
                    deliveryIntervalDays = deliveryIntervalDays,
                    stretchPersonalizationReady = stretchPersonalizationReady,
                    stretchGoalCount = stretchGoalCount,
                    stretchPreferredTimeSlots = stretchPreferredTimeSlots,
                    digitalPersonalizationReady = digitalPersonalizationReady,
                    digitalInterventionThresholdMinutes = digitalInterventionThresholdMinutes,
                    preferredMessageTone = preferredMessageTone
                )
            } else {
                MonthlyContent(
                    state = monthlyState,
                    waterPersonalizationReady = waterPersonalizationReady,
                    waterGoalMl = waterGoalMl,
                    waterReminderIntervalMinutes = waterReminderIntervalMinutes,
                    waterPeakJson = waterPeakJson,
                    mealPersonalizationReady = mealPersonalizationReady,
                    mealBreakfastPeak = mealBreakfastPeak,
                    mealLunchPeak = mealLunchPeak,
                    mealDinnerPeak = mealDinnerPeak,
                    mealLateNightPeak = mealLateNightPeak,
                    deliveryIntervalDays = deliveryIntervalDays,
                    stretchPersonalizationReady = stretchPersonalizationReady,
                    stretchGoalCount = stretchGoalCount,
                    stretchPreferredTimeSlots = stretchPreferredTimeSlots,
                    digitalPersonalizationReady = digitalPersonalizationReady,
                    digitalInterventionThresholdMinutes = digitalInterventionThresholdMinutes,
                    preferredMessageTone = preferredMessageTone
                )
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

// 아코디언 컴포저블 확장
@Composable
private fun ExpandableCategoryAchievementCard(
    category: HabitCategoryStyle,
    achievementRate: Float,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
        onClick = onToggleExpand
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val rateText = if (category == HabitCategoryStyle.DIGITAL) {
                        "차단 성공률 ${(achievementRate * 100).toInt()}%"
                    } else {
                        "${(achievementRate * 100).toInt()}%"
                    }
                    Text(
                        text = rateText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = category.primaryColor,
                    )
                    Spacer(modifier = Modifier.width(HabitSpacing.xs))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "펼치기",
                        tint = HabitTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(HabitSpacing.sm))
            LinearProgressIndicator(
                progress = { achievementRate.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = category.primaryColor,
                trackColor = category.backgroundColor,
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(HabitSpacing.md))
                    HorizontalDivider(color = HabitLineGray)
                    Spacer(modifier = Modifier.height(HabitSpacing.sm))
                    content()
                }
            }
        }
    }
}

// 요일별/주차별 심플 막대그래프
@Composable
private fun SimpleBarChart(
    data: List<Pair<String, Float>>,
    targetValue: Float,
    unit: String,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(data, targetValue) {
        val maxRecorded = data.maxOfOrNull { it.second } ?: 0f
        maxOf(maxRecorded, targetValue).coerceAtLeast(1f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = HabitSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (value > 0f) {
                        if (value == value.toInt().toFloat()) "${value.toInt()}$unit" else "${"%.1f".format(value)}$unit"
                    } else "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = HabitTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(HabitSpacing.xs))
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .width(16.dp)
                        .background(HabitLineGray, shape = RoundedCornerShape(HabitRadius.sm)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val fillRatio = value / maxVal
                    if (fillRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fillRatio.coerceIn(0f, 1f))
                                .background(barColor, shape = RoundedCornerShape(HabitRadius.sm))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(HabitSpacing.xs))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = HabitTextSecondary
                )
            }
        }
    }
}

// 마크다운 파싱용 헬퍼 함수
@Composable
private fun rememberMarkdownAnnotatedString(text: String): androidx.compose.ui.text.AnnotatedString {
    return remember(text) {
        buildAnnotatedString {
            val parts = text.split("**")
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = HabitTextPrimary)) {
                        append(part)
                    }
                } else {
                    append(part)
                }
            }
        }
    }
}

@Composable
private fun FeedbackText(text: String) {
    val annotated = rememberMarkdownAnnotatedString(text)
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall,
        color = HabitTextSecondary,
        modifier = Modifier.padding(top = HabitSpacing.sm)
    )
}

// ── 카테고리별 피드백 콘텐츠 ──

@Composable
private fun WaterFeedbackContent(
    ready: Boolean,
    goalMl: Int,
    interval: Int,
    peakJson: String
) {
    val peakWindow = remember(peakJson) {
        if (peakJson.isNotBlank()) com.example.habittracker.domain.model.PeakWindow.fromJson(peakJson) else null
    }
    val message = remember(ready, goalMl, interval, peakWindow) {
        if (ready) {
            val timeRangeStr = peakWindow?.let {
                val startHour = it.rangeStart / 60
                val endHour = it.rangeEnd / 60
                " 주로 하루 중 **${startHour}시 ~ ${endHour}시** 사이에 가장 활발하게 수분을 섭취하고 계십니다."
            } ?: ""
            "분석 결과, 사용자님의 맞춤 하루 권장 수분 섭취량은 **${goalMl}ml**입니다. 권장 주기인 **${interval}분**마다 수분을 섭취해 건강한 리듬을 지켜보세요.$timeRangeStr"
        } else {
            "아직 수분 섭취 분석 데이터가 부족하여 기본 설정(2000ml, 180분)이 제공됩니다. 최소 7일간 20회 이상 물을 기록하시면 사용자님만을 위한 최적의 물 권장량과 피크 시간대를 분석해 드릴게요!"
        }
    }
    FeedbackText(text = message)
}

@Composable
private fun MealFeedbackContent(
    ready: Boolean,
    breakfast: com.example.habittracker.domain.model.PeakWindow?,
    lunch: com.example.habittracker.domain.model.PeakWindow?,
    dinner: com.example.habittracker.domain.model.PeakWindow?,
    lateNight: com.example.habittracker.domain.model.PeakWindow?,
    deliveryInterval: Float?
) {
    val message = remember(ready, breakfast, lunch, dinner, lateNight, deliveryInterval) {
        if (ready) {
            val breakfastTime = breakfast?.let { formatMinutesToTime(it.centerMinutes) } ?: "07:30"
            val lunchTime = lunch?.let { formatMinutesToTime(it.centerMinutes) } ?: "12:00"
            val dinnerTime = dinner?.let { formatMinutesToTime(it.centerMinutes) } ?: "18:00"

            var base = "분석된 사용자님의 최적 식사 시간대는 **아침 ${breakfastTime}경**, **점심 ${lunchTime}경**, **저녁 ${dinnerTime}경**입니다."

            if (lateNight != null && !lateNight.isEmpty() && lateNight.concentration > 0.15f) {
                val lateTime = formatMinutesToTime(lateNight.centerMinutes)
                base += " 밤 **${lateTime}** 전후로 야식을 즐겨 섭취하는 패턴이 감지되었으니 수면 전 식사는 가급적 지양해 보세요."
            }
            if (deliveryInterval != null) {
                base += " 배달 앱 주문 평균 주기는 **${"%.1f".format(deliveryInterval)}일**입니다."
            }
            base
        } else {
            "식사 분석이 완료되지 않아 기본 권장 가이드(기상 1시간 후 아침, 12시 점심, 취침 3시간 전 저녁)가 제공됩니다. 식사 습관의 정확한 시간 피크 및 야식 습관을 분석하기 위해 최소 7일간 5회 이상 식사를 기록해 주세요!"
        }
    }
    FeedbackText(text = message)
}

@Composable
private fun StretchFeedbackContent(
    ready: Boolean,
    goalCount: Int,
    preferredSlotsJson: String
) {
    val message = remember(ready, goalCount, preferredSlotsJson) {
        if (ready) {
            val slot = preferredSlotsJson.replace("[", "").replace("]", "").replace("\"", "").trim()
            val slotStr = if (slot.isNotBlank()) " 주로 **${slot}** 시간대에 스트레칭을 가장 성실히 실천하고 계십니다." else ""
            "목표 달성 추이에 맞춰 동적으로 조정한 사용자님의 일일 맞춤 스트레칭 목표는 **${goalCount}회**입니다.$slotStr 선호 시간대에 맞춰 틈틈이 몸을 풀어주세요!"
        } else {
            "아직 스트레칭 맞춤 분석이 준비되지 않았습니다. 기본 스트레칭 목표(일 4회)가 제안됩니다. 최소 7일간 10회 이상 스트레칭을 기록하시면 최적의 추천 횟수와 시간대를 매칭해 드릴게요!"
        }
    }
    FeedbackText(text = message)
}

@Composable
private fun DigitalFeedbackContent(
    ready: Boolean,
    thresholdMinutes: Int,
    tone: String,
    totalInterventions: Int,
    totalReacted: Int
) {
    val message = remember(ready, thresholdMinutes, tone, totalInterventions, totalReacted) {
        val toneKo = when (tone) {
            "EMPATHY" -> "부드러운 위로형 어조"
            "DECISIVE" -> "단호하고 직설적인 어조"
            else -> "공감과 격려형 어조"
        }
        val rateText = if (totalInterventions > 0) {
            " 이번 주 앱 연속 사용 경고 알림이 울렸을 때 사용자님은 **총 ${totalInterventions}회 중 ${totalReacted}회** 사용을 멈추고 제어에 성공하셨습니다."
        } else {
            ""
        }
        if (ready) {
            "스마트폰 과몰입 방지를 위한 사용자님의 맞춤 앱당 1회 연속 사용 임계치는 **${thresholdMinutes}분**입니다.$rateText 분석 결과, 사용자님은 **${toneKo}**에 가장 긍정적으로 반응하여 해당 경고 알림 톤이 유지되고 있습니다."
        } else {
            "앱 몰입 관리 분석 데이터가 준비되지 않아 기본 경고 임계치(30분)와 공감 어조의 알림이 활성화되어 있습니다.$rateText 최소 7일간 사용을 유지하시면 최적의 임계 시간과 맞춤 어조가 적용됩니다."
        }
    }
    FeedbackText(text = message)
}

private fun formatMinutesToTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format("%02d:%02d", h, m)
}

private fun isSameDate(timestamp: Long, localDate: LocalDate): Boolean {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.isEqual(localDate)
}

private fun getDayLabel(localDate: LocalDate): String {
    return when (localDate.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "월"
        java.time.DayOfWeek.TUESDAY -> "화"
        java.time.DayOfWeek.WEDNESDAY -> "수"
        java.time.DayOfWeek.THURSDAY -> "목"
        java.time.DayOfWeek.FRIDAY -> "금"
        java.time.DayOfWeek.SATURDAY -> "토"
        java.time.DayOfWeek.SUNDAY -> "일"
        else -> ""
    }
}

@Composable
private fun InsightsCard(
    waterPattern: WaterPatternResult?,
    mealPattern: MealPatternResult?,
    digitalPattern: DigitalPatternResult?,
    stretchPattern: StretchPatternResult?,
) {
    val insights = remember(waterPattern, mealPattern, digitalPattern, stretchPattern) {
        val list = mutableListOf<String>()
        waterPattern?.let { p ->
            if (p.peakHours.isNotEmpty()) {
                list.add("💧 물은 ${p.peakHours.first()}시 전후로 가장 많이 마셨어요.")
            }
        }
        mealPattern?.let { p ->
            p.lateNightRiskHour?.let { h ->
                list.add("🌙 ${h}시 이후 야식 패턴이 감지됐어요. 주의해봐요!")
            }
            val mostSkipped = p.skippedMealPattern.maxByOrNull { it.value }
            if (mostSkipped != null && mostSkipped.value > 0) {
                list.add("🍽️ ${mostSkipped.key} 식사를 자주 건너뛰고 있어요.")
            }
        }
        digitalPattern?.let { p ->
            if (p.bedtimeUsageScore > 0.5f) {
                list.add("📱 취침 전 디지털 사용이 많아요. 수면에 영향을 줄 수 있어요.")
            } else if (p.peakUsageHours.isNotEmpty()) {
                list.add("📱 ${p.peakUsageHours.first()}시 전후로 디지털 사용이 가장 많아요.")
            }
        }
        stretchPattern?.let { p ->
            if (p.digitalTriggerConversionRate > 0.3f) {
                list.add("🧘 디지털 알림 후 스트레칭 전환율이 ${(p.digitalTriggerConversionRate * 100).toInt()}%예요!")
            }
        }
        if (list.isEmpty()) {
            list.add("💡 꾸준히 기록하면 더 정확한 패턴 분석이 가능해요.")
        }
        list
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
                text = "해빗프렌즈 인사이트",
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
private fun WeeklyContent(
    state: WeeklyReportState?,
    currentWeekStart: LocalDate,
    waterPersonalizationReady: Boolean,
    waterGoalMl: Int,
    waterReminderIntervalMinutes: Int,
    waterPeakJson: String,
    mealPersonalizationReady: Boolean,
    mealBreakfastPeak: com.example.habittracker.domain.model.PeakWindow?,
    mealLunchPeak: com.example.habittracker.domain.model.PeakWindow?,
    mealDinnerPeak: com.example.habittracker.domain.model.PeakWindow?,
    mealLateNightPeak: com.example.habittracker.domain.model.PeakWindow?,
    deliveryIntervalDays: Float?,
    stretchPersonalizationReady: Boolean,
    stretchGoalCount: Int,
    stretchPreferredTimeSlots: String,
    digitalPersonalizationReady: Boolean,
    digitalInterventionThresholdMinutes: Int,
    preferredMessageTone: String
) {
    if (state == null) {
        ReportLoadingCard()
        return
    }

    ReportSummaryCard(
        periodLabel = state.weekLabel,
        overallRate = state.overallAchievementRate,
        isWeekly = true,
    )

    // 카테고리별 확장 상태
    var isWaterExpanded by remember { mutableStateOf(false) }
    var isMealExpanded by remember { mutableStateOf(false) }
    var isDigitalExpanded by remember { mutableStateOf(false) }
    var isStretchExpanded by remember { mutableStateOf(false) }

    val waterRate = remember(state.dailyWaterSummaries) {
        if (state.dailyWaterSummaries.isNotEmpty())
            state.dailyWaterSummaries.map { it.achievementRate }.average().toFloat()
        else 0f
    }

    val mealRate = remember(state.dailyMealSummaries) {
        if (state.dailyMealSummaries.isNotEmpty()) {
            state.dailyMealSummaries.map { summary ->
                val logged = summary.mealMap.values.count { it }
                (logged.toFloat() / 3f).coerceIn(0f, 1f)
            }.average().toFloat()
        } else 0f
    }

    val digitalRate = remember(state.dailyDigitalSummaries) {
        if (state.dailyDigitalSummaries.isNotEmpty()) {
            state.dailyDigitalSummaries.map { summary ->
                if (summary.interventionCount == 0) {
                    1f // 경고 알림이 울리지 않았으므로 과사용 조절 100% 성공
                } else {
                    (summary.reactedCount.toFloat() / summary.interventionCount.toFloat()).coerceIn(0f, 1f)
                }
            }.average().toFloat()
        } else 0f
    }

    val stretchRate = remember(state.dailyStretchSummaries, stretchGoalCount) {
        if (state.dailyStretchSummaries.isNotEmpty()) {
            state.dailyStretchSummaries.map { summary ->
                (summary.count.toFloat() / stretchGoalCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            }.average().toFloat()
        } else 0f
    }

    // 1. 식사
    ExpandableCategoryAchievementCard(
        category = HabitCategoryStyle.MEAL,
        achievementRate = mealRate,
        isExpanded = isMealExpanded,
        onToggleExpand = { isMealExpanded = !isMealExpanded }
    ) {
        val chartData = remember(state.dailyMealSummaries, currentWeekStart) {
            (0..6).map { offset ->
                val date = currentWeekStart.plusDays(offset.toLong())
                val dayLabel = getDayLabel(date)
                val summary = state.dailyMealSummaries.find { isSameDate(it.date, date) }
                val loggedCount = summary?.mealMap?.values?.count { it }?.toFloat() ?: 0f
                dayLabel to loggedCount
            }
        }
        SimpleBarChart(data = chartData, targetValue = 3f, unit = "회", barColor = HabitCategoryStyle.MEAL.primaryColor)
        MealFeedbackContent(
            ready = mealPersonalizationReady,
            breakfast = mealBreakfastPeak,
            lunch = mealLunchPeak,
            dinner = mealDinnerPeak,
            lateNight = mealLateNightPeak,
            deliveryInterval = deliveryIntervalDays
        )
    }

    // 2. 물
    ExpandableCategoryAchievementCard(
        category = HabitCategoryStyle.WATER,
        achievementRate = waterRate,
        isExpanded = isWaterExpanded,
        onToggleExpand = { isWaterExpanded = !isWaterExpanded }
    ) {
        val chartData = remember(state.dailyWaterSummaries, currentWeekStart) {
            (0..6).map { offset ->
                val date = currentWeekStart.plusDays(offset.toLong())
                val dayLabel = getDayLabel(date)
                val summary = state.dailyWaterSummaries.find { isSameDate(it.date, date) }
                dayLabel to (summary?.totalMl?.toFloat() ?: 0f)
            }
        }
        SimpleBarChart(data = chartData, targetValue = waterGoalMl.toFloat(), unit = "ml", barColor = HabitCategoryStyle.WATER.primaryColor)
        WaterFeedbackContent(
            ready = waterPersonalizationReady,
            goalMl = waterGoalMl,
            interval = waterReminderIntervalMinutes,
            peakJson = waterPeakJson
        )
    }

    // 3. 디지털
    ExpandableCategoryAchievementCard(
        category = HabitCategoryStyle.DIGITAL,
        achievementRate = digitalRate,
        isExpanded = isDigitalExpanded,
        onToggleExpand = { isDigitalExpanded = !isDigitalExpanded }
    ) {
        val chartData = remember(state.dailyDigitalSummaries, currentWeekStart) {
            (0..6).map { offset ->
                val date = currentWeekStart.plusDays(offset.toLong())
                val dayLabel = getDayLabel(date)
                val summary = state.dailyDigitalSummaries.find { isSameDate(it.date, date) }
                dayLabel to (summary?.totalMinutes?.toFloat() ?: 0f)
            }
        }
        SimpleBarChart(data = chartData, targetValue = digitalInterventionThresholdMinutes.toFloat(), unit = "분", barColor = HabitCategoryStyle.DIGITAL.primaryColor)
        val totalInterventions = remember(state.dailyDigitalSummaries) {
            state.dailyDigitalSummaries.sumOf { it.interventionCount }
        }
        val totalReacted = remember(state.dailyDigitalSummaries) {
            state.dailyDigitalSummaries.sumOf { it.reactedCount }
        }
        DigitalFeedbackContent(
            ready = digitalPersonalizationReady,
            thresholdMinutes = digitalInterventionThresholdMinutes,
            tone = preferredMessageTone,
            totalInterventions = totalInterventions,
            totalReacted = totalReacted
        )
    }

    // 4. 스트레칭
    ExpandableCategoryAchievementCard(
        category = HabitCategoryStyle.STRETCH,
        achievementRate = stretchRate,
        isExpanded = isStretchExpanded,
        onToggleExpand = { isStretchExpanded = !isStretchExpanded }
    ) {
        val chartData = remember(state.dailyStretchSummaries, currentWeekStart) {
            (0..6).map { offset ->
                val date = currentWeekStart.plusDays(offset.toLong())
                val dayLabel = getDayLabel(date)
                val summary = state.dailyStretchSummaries.find { isSameDate(it.date, date) }
                dayLabel to (summary?.count?.toFloat() ?: 0f)
            }
        }
        SimpleBarChart(data = chartData, targetValue = stretchGoalCount.toFloat(), unit = "회", barColor = HabitCategoryStyle.STRETCH.primaryColor)
        StretchFeedbackContent(
            ready = stretchPersonalizationReady,
            goalCount = stretchGoalCount,
            preferredSlotsJson = stretchPreferredTimeSlots
        )
    }

    InsightsCard(
        waterPattern = state.waterPattern,
        mealPattern = state.mealPattern,
        digitalPattern = state.digitalPattern,
        stretchPattern = state.stretchPattern,
    )
}

@Composable
private fun MonthlyContent(
    state: MonthlyReportState?,
    waterPersonalizationReady: Boolean,
    waterGoalMl: Int,
    waterReminderIntervalMinutes: Int,
    waterPeakJson: String,
    mealPersonalizationReady: Boolean,
    mealBreakfastPeak: com.example.habittracker.domain.model.PeakWindow?,
    mealLunchPeak: com.example.habittracker.domain.model.PeakWindow?,
    mealDinnerPeak: com.example.habittracker.domain.model.PeakWindow?,
    mealLateNightPeak: com.example.habittracker.domain.model.PeakWindow?,
    deliveryIntervalDays: Float?,
    stretchPersonalizationReady: Boolean,
    stretchGoalCount: Int,
    stretchPreferredTimeSlots: String,
    digitalPersonalizationReady: Boolean,
    digitalInterventionThresholdMinutes: Int,
    preferredMessageTone: String
) {
    if (state == null) {
        ReportLoadingCard()
        return
    }

    ReportSummaryCard(
        periodLabel = state.monthLabel,
        overallRate = state.overallAchievementRate,
        isWeekly = false,
    )

    var isSnapshotsExpanded by remember { mutableStateOf(false) }

    if (state.weeklySnapshots.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HabitRadius.card),
            colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
            onClick = { isSnapshotsExpanded = !isSnapshotsExpanded }
        ) {
            Column(modifier = Modifier.padding(HabitSpacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "주차별 달성률",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HabitTextPrimary,
                    )
                    Icon(
                        imageVector = if (isSnapshotsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "펼치기",
                        tint = HabitTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(HabitSpacing.sm))
                LinearProgressIndicator(
                    progress = { state.overallAchievementRate.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = HabitDeepMint,
                    trackColor = HabitLineGray,
                )

                AnimatedVisibility(
                    visible = isSnapshotsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(HabitSpacing.md))
                        HorizontalDivider(color = HabitLineGray)
                        Spacer(modifier = Modifier.height(HabitSpacing.sm))

                        // 주차별 세로 막대그래프
                        val chartData = remember(state.weeklySnapshots) {
                            state.weeklySnapshots.map {
                                val label = it.weekLabel.replace("Week ", "주")
                                label to (it.achievementRate * 100f)
                            }
                        }
                        SimpleBarChart(data = chartData, targetValue = 100f, unit = "%", barColor = HabitDeepMint)

                        Spacer(modifier = Modifier.height(HabitSpacing.md))
                        HorizontalDivider(color = HabitLineGray)
                        Spacer(modifier = Modifier.height(HabitSpacing.sm))

                        // 기존 가로 리스트도 그대로 깔끔하게 제공
                        state.weeklySnapshots.forEach { snapshot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = HabitSpacing.xxs),
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
        }
    }

    // 월간 데이터에 맞는 규칙 기반 개인화 피드백 모음 카드 추가
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HabitRadius.card),
        colors = CardDefaults.cardColors(containerColor = HabitCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = HabitElevation.card),
    ) {
        Column(
            modifier = Modifier.padding(HabitSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HabitSpacing.sm)
        ) {
            Text(
                text = "월간 개인화 분석 종합 리포트",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HabitTextPrimary,
            )
            HorizontalDivider(color = HabitLineGray)

            Text(
                text = "💧 수분 섭취 피드백",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = HabitCategoryStyle.WATER.primaryColor
            )
            WaterFeedbackContent(
                ready = waterPersonalizationReady,
                goalMl = waterGoalMl,
                interval = waterReminderIntervalMinutes,
                peakJson = waterPeakJson
            )
            Spacer(modifier = Modifier.height(HabitSpacing.xs))

            Text(
                text = "🍽️ 식사 습관 피드백",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = HabitCategoryStyle.MEAL.primaryColor
            )
            MealFeedbackContent(
                ready = mealPersonalizationReady,
                breakfast = mealBreakfastPeak,
                lunch = mealLunchPeak,
                dinner = mealDinnerPeak,
                lateNight = mealLateNightPeak,
                deliveryInterval = deliveryIntervalDays
            )
            Spacer(modifier = Modifier.height(HabitSpacing.xs))

            Text(
                text = "🧘 스트레칭 피드백",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = HabitCategoryStyle.STRETCH.primaryColor
            )
            StretchFeedbackContent(
                ready = stretchPersonalizationReady,
                goalCount = stretchGoalCount,
                preferredSlotsJson = stretchPreferredTimeSlots
            )
            Spacer(modifier = Modifier.height(HabitSpacing.xs))

            Text(
                text = "📱 디지털 사용 피드백",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = HabitCategoryStyle.DIGITAL.primaryColor
            )
            DigitalFeedbackContent(
                ready = digitalPersonalizationReady,
                thresholdMinutes = digitalInterventionThresholdMinutes,
                tone = preferredMessageTone,
                totalInterventions = 0,
                totalReacted = 0
            )
        }
    }

    InsightsCard(
        waterPattern = state.waterPattern,
        mealPattern = state.mealPattern,
        digitalPattern = state.digitalPattern,
        stretchPattern = state.stretchPattern,
    )
}
