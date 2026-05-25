// 경로: com/example/habittracker/ui/water/WaterInputScreen.kt
package com.example.habittracker.ui.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.habittracker.ui.components.HabitProgressIndicator
import com.example.habittracker.ui.components.OneButtonRow

@Composable
fun WaterInputScreen(
    navController: NavController,
    viewModel: WaterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val status = uiState.todayStatus

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "💧 수분 보충", style = MaterialTheme.typography.headlineSmall)

        if (status != null) {
            Text(
                text = "${status.totalMl}ml / ${status.goalMl}ml",
                style = MaterialTheme.typography.titleLarge,
            )
            HabitProgressIndicator(
                label = "오늘 달성률 ${(status.achievementRate * 100).toInt()}%",
                progress = status.achievementRate,
            )
        }

        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }

        OneButtonRow(
            buttons = listOf(
                "250ml" to { viewModel.onDrinkButtonClick(250) },
                "500ml" to { viewModel.onDrinkButtonClick(500) },
                "나중에" to { navController.popBackStack() },
            )
        )
    }
}
