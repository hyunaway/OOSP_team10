// 경로: com/example/habittracker/ui/stretch/StretchInputScreen.kt
package com.example.habittracker.ui.stretch

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
import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.ui.components.AvatarHealthView
import com.example.habittracker.ui.components.OneButtonRow

@Composable
fun StretchInputScreen(
    navController: NavController,
    viewModel: StretchViewModel = hiltViewModel(),
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
        Text(text = "🧘 스트레칭", style = MaterialTheme.typography.headlineSmall)

        AvatarHealthView(score = status?.avatarHealthScore ?: 0f)

        if (status != null) {
            Text(
                text = "오늘 ${status.totalCount}회 완료",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Text(text = "어느 부위를 스트레칭할까요?", style = MaterialTheme.typography.bodyLarge)

        OneButtonRow(
            buttons = listOf(
                "목" to { viewModel.onStretchButtonClick(BodyPartType.NECK) },
                "어깨" to { viewModel.onStretchButtonClick(BodyPartType.SHOULDER) },
            )
        )

        OneButtonRow(
            buttons = listOf(
                "허리" to { viewModel.onStretchButtonClick(BodyPartType.BACK) },
                "전신" to { viewModel.onStretchButtonClick(BodyPartType.FULL) },
            )
        )

        OneButtonRow(
            buttons = listOf(
                "나중에" to { navController.popBackStack() },
            )
        )

        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
    }
}
