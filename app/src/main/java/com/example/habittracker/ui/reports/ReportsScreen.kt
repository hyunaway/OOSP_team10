// 경로: com/example/habittracker/ui/reports/ReportsScreen.kt
package com.example.habittracker.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.domain.model.MonthlyReportState
import com.example.habittracker.domain.model.WeeklyReportState
import com.example.habittracker.ui.components.HabitProgressIndicator

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

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("주간") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("월간") },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectedTab == 0) {
                WeeklyContent(weeklyState)
            } else {
                MonthlyContent(monthlyState)
            }
        }
    }
}

@Composable
private fun WeeklyContent(state: WeeklyReportState?) {
    if (state == null) {
        Text("데이터를 불러오는 중...", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Text(text = state.weekLabel, style = MaterialTheme.typography.titleMedium)
    HabitProgressIndicator(
        label = "주간 달성률 ${(state.overallAchievementRate * 100).toInt()}%",
        progress = state.overallAchievementRate,
        modifier = Modifier.fillMaxWidth(),
    )
    val bestDay = state.dailyWaterSummaries.maxByOrNull { it.achievementRate }
    if (bestDay != null) {
        Text(
            text = "Best Day 달성률: ${(bestDay.achievementRate * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Text(
        text = "💡 꾸준히 기록하면 더 정확한 패턴 분석이 가능해요.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MonthlyContent(state: MonthlyReportState?) {
    if (state == null) {
        Text("데이터를 불러오는 중...", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Text(text = state.monthLabel, style = MaterialTheme.typography.titleMedium)
    HabitProgressIndicator(
        label = "월간 달성률 ${(state.overallAchievementRate * 100).toInt()}%",
        progress = state.overallAchievementRate,
        modifier = Modifier.fillMaxWidth(),
    )
    state.weeklySnapshots.forEach { snapshot ->
        Text(
            text = "${snapshot.weekLabel}: ${(snapshot.achievementRate * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
        )
        HabitProgressIndicator(
            label = "",
            progress = snapshot.achievementRate,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(
        text = "💡 월간 패턴을 분석해 더 나은 습관을 만들어 보세요.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
