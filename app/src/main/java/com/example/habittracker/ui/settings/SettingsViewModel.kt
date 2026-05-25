// 경로: com/example/habittracker/ui/settings/SettingsViewModel.kt
package com.example.habittracker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userPreferenceManager: UserPreferenceManager,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferenceManager.bedTimeFlow,
                userPreferenceManager.wakeTimeFlow,
                userPreferenceManager.waterReminderIntervalMinutesFlow,
                userPreferenceManager.preferredMessageToneFlow,
            ) { bedTime, wakeTime, interval, tone ->
                SettingsUiState(
                    loading = false,
                    bedTime = bedTime,
                    wakeTime = wakeTime,
                    waterReminderIntervalMinutes = interval,
                    preferredMessageTone = tone,
                )
            }
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { state -> _uiState.value = state }
        }
    }

    fun updateBedTime(value: String) {
        _uiState.update { it.copy(bedTime = value, isSaved = false) }
    }

    fun updateWakeTime(value: String) {
        _uiState.update { it.copy(wakeTime = value, isSaved = false) }
    }

    fun updateWaterReminderInterval(minutes: Int) {
        _uiState.update { it.copy(waterReminderIntervalMinutes = minutes, isSaved = false) }
    }

    fun updatePreferredMessageTone(tone: String) {
        _uiState.update { it.copy(preferredMessageTone = tone, isSaved = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                userPreferenceManager.updateBedTime(state.bedTime)
                userPreferenceManager.updateWakeTime(state.wakeTime)
                userPreferenceManager.updateWaterReminderIntervalMinutes(state.waterReminderIntervalMinutes)
                userPreferenceManager.updatePreferredMessageTone(state.preferredMessageTone)
                WorkScheduler.rescheduleAll(getApplication(), userPreferenceManager)
                _uiState.update { it.copy(isSaved = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
