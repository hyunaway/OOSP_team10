// 경로: com/example/habittracker/ui/meal/MealInputScreen.kt
package com.example.habittracker.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.habittracker.data.model.MealType
import com.example.habittracker.ui.components.OneButtonRow

@Composable
fun MealInputScreen(
    navController: NavController,
    viewModel: MealViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val status = uiState.todayStatus

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🍽 식사 기록", style = MaterialTheme.typography.headlineSmall)

        if (status != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MealBadge(label = "아침", checked = status.breakfastLogged)
                MealBadge(label = "점심", checked = status.lunchLogged)
                MealBadge(label = "저녁", checked = status.dinnerLogged)
            }
        }

        Text(text = "어떤 끼니를 드셨나요?", style = MaterialTheme.typography.bodyLarge)

        OneButtonRow(
            buttons = listOf(
                "아침" to { viewModel.onMealButtonClick(MealType.BREAKFAST) },
                "점심" to { viewModel.onMealButtonClick(MealType.LUNCH) },
                "저녁" to { viewModel.onMealButtonClick(MealType.DINNER) },
            )
        )

        OneButtonRow(
            buttons = listOf(
                "간식" to { viewModel.onMealButtonClick(MealType.SNACK) },
                "나중에" to { navController.popBackStack() },
            )
        )

        ElevatedButton(
            onClick = { viewModel.onLateNightButtonClick(true) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("🌙 야식 먹었어요")
        }

        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MealBadge(label: String, checked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (checked) "✓" else "○",
            style = MaterialTheme.typography.titleLarge,
            color = if (checked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
