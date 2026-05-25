// 경로: com/example/habittracker/ui/home/HomeScreen.kt
package com.example.habittracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.habittracker.ui.components.AvatarHealthView
import com.example.habittracker.ui.components.HabitCard
import com.example.habittracker.ui.dashboard.DashboardViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.loading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val state = uiState.dashboardState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "오늘의 습관",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (state != null) {
            AvatarHealthView(
                score = state.overallScore,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Text(
                text = state.motivationalMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            HabitCard(
                title = "💧 수분",
                subtitle = "${state.waterStatus.totalMl}ml / ${state.waterStatus.goalMl}ml",
                progress = state.waterStatus.achievementRate,
                onClick = { navController.navigate("water") },
            )

            HabitCard(
                title = "🍽 식사",
                subtitle = buildString {
                    if (state.mealStatus.breakfastLogged) append("아침 ✓ ")
                    if (state.mealStatus.lunchLogged) append("점심 ✓ ")
                    if (state.mealStatus.dinnerLogged) append("저녁 ✓")
                    if (isEmpty()) append("아직 기록 없음")
                },
                onClick = { navController.navigate("meal") },
            )

            HabitCard(
                title = "📱 디지털",
                subtitle = "오늘 ${state.digitalStatus.totalUsageMinutes}분 사용",
                onClick = { navController.navigate("digital") },
            )

            HabitCard(
                title = "🧘 스트레칭",
                subtitle = "오늘 ${state.stretchStatus.totalCount}회 완료",
                progress = state.stretchStatus.avatarHealthScore,
                onClick = { navController.navigate("stretch") },
            )
        }

        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
    }
}
