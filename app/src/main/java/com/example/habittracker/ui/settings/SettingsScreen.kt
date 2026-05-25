// 경로: com/example/habittracker/ui/settings/SettingsScreen.kt
package com.example.habittracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Local state for fields not yet persisted via ViewModel
    var lateNightStart by remember { mutableStateOf("22:00") }
    var digitalBaseDuration by remember { mutableStateOf("30") }
    var waterNotifEnabled by remember { mutableStateOf(true) }
    var mealNotifEnabled by remember { mutableStateOf(true) }
    var stretchNotifEnabled by remember { mutableStateOf(true) }
    var digitalNotifEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "설정", style = MaterialTheme.typography.headlineSmall)

        SectionLabel("수면 시간")
        OutlinedTextField(
            value = uiState.bedTime,
            onValueChange = { viewModel.updateBedTime(it) },
            label = { Text("취침 시간 (HH:mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.wakeTime,
            onValueChange = { viewModel.updateWakeTime(it) },
            label = { Text("기상 시간 (HH:mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        HorizontalDivider()
        SectionLabel("알림 주기")
        OutlinedTextField(
            value = uiState.waterReminderIntervalMinutes.toString(),
            onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateWaterReminderInterval(v) } },
            label = { Text("물 알림 주기 (분)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        HorizontalDivider()
        SectionLabel("야식 설정")
        OutlinedTextField(
            value = lateNightStart,
            onValueChange = { lateNightStart = it },
            label = { Text("야식 시작 시간 (HH:mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        HorizontalDivider()
        SectionLabel("디지털 기준")
        OutlinedTextField(
            value = digitalBaseDuration,
            onValueChange = { digitalBaseDuration = it },
            label = { Text("기준 사용 시간 (분)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        HorizontalDivider()
        SectionLabel("메시지 톤")
        OutlinedTextField(
            value = uiState.preferredMessageTone,
            onValueChange = { viewModel.updatePreferredMessageTone(it) },
            label = { Text("PRAISE / HUMOR / EMPATHY / CHALLENGE") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        HorizontalDivider()
        SectionLabel("카테고리별 알림")
        NotifToggleRow(label = "💧 물", enabled = waterNotifEnabled, onToggle = { waterNotifEnabled = it })
        NotifToggleRow(label = "🍽 식사", enabled = mealNotifEnabled, onToggle = { mealNotifEnabled = it })
        NotifToggleRow(label = "🧘 스트레칭", enabled = stretchNotifEnabled, onToggle = { stretchNotifEnabled = it })
        NotifToggleRow(label = "📱 디지털", enabled = digitalNotifEnabled, onToggle = { digitalNotifEnabled = it })

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.saveSettings() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("저장")
        }

        if (uiState.isSaved) {
            Snackbar { Text("설정이 저장되었습니다.") }
        }

        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun NotifToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
