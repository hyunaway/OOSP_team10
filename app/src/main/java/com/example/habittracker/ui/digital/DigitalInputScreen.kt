// 경로: com/example/habittracker/ui/digital/DigitalInputScreen.kt
package com.example.habittracker.ui.digital

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
import com.example.habittracker.ui.components.OneButtonRow

@Composable
fun DigitalInputScreen(
    navController: NavController,
    interventionId: Long = -1L,
    viewModel: DigitalViewModel = hiltViewModel(),
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
        Text(text = "📱 디지털 사용", style = MaterialTheme.typography.headlineSmall)

        if (status != null) {
            Text(
                text = "오늘 총 ${status.totalUsageMinutes}분 사용",
                style = MaterialTheme.typography.titleLarge,
            )
            status.topApp?.let { app ->
                Text(
                    text = "최다 사용: $app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }

        OneButtonRow(
            buttons = listOf(
                "5분 쉬기" to {
                    if (interventionId >= 0) {
                        viewModel.onInterventionAction(interventionId, "break")
                    }
                    navController.popBackStack()
                },
                "스트레칭" to {
                    if (interventionId >= 0) {
                        viewModel.onInterventionAction(interventionId, "stretch")
                    }
                    navController.navigate("stretch")
                },
                "계속 보기" to {
                    if (interventionId >= 0) {
                        viewModel.onInterventionAction(interventionId, "continue")
                    }
                    navController.popBackStack()
                },
            )
        )
    }
}
