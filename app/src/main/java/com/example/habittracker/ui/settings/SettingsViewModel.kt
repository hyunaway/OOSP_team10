// 경로: com/example/habittracker/ui/settings/SettingsViewModel.kt
package com.example.habittracker.ui.settings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.usage.UsageStatsHelper
import com.example.habittracker.ui.avatar.AvatarGender
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
    private val usageStatsHelper: UsageStatsHelper,
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
                .combine(userPreferenceManager.avatarGenderFlow) { state, gender ->
                    state.copy(avatarGender = AvatarGender.fromString(gender))
                }
                .combine(userPreferenceManager.userNameFlow) { state, name ->
                    state.copy(userName = name)
                }
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { state ->
                    val current = _uiState.value
                    _uiState.value = state.copy(
                        notificationPermissionGranted = current.notificationPermissionGranted,
                        usageAccessGranted = current.usageAccessGranted,
                    )
                    refreshPermissionStates()
                }
        }
        refreshPermissionStates()
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

    fun updateAvatarGender(gender: AvatarGender) {
        _uiState.update { it.copy(avatarGender = gender, isSaved = false) }
    }

    fun updateUserName(name: String) {
        _uiState.update { it.copy(userName = name, isSaved = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                userPreferenceManager.updateBedTime(state.bedTime)
                userPreferenceManager.updateWakeTime(state.wakeTime)
                userPreferenceManager.updateWaterReminderIntervalMinutes(state.waterReminderIntervalMinutes)
                userPreferenceManager.updatePreferredMessageTone(state.preferredMessageTone)
                userPreferenceManager.updateAvatarGender(state.avatarGender.name)
                userPreferenceManager.updateUserName(state.userName.trim().ifEmpty { "나" })
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

    fun refreshPermissionStates() {
        _uiState.update {
            it.copy(
                notificationPermissionGranted = hasNotificationPermission(),
                usageAccessGranted = usageStatsHelper.hasUsageAccess(),
            )
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(notificationPermissionGranted = granted || hasNotificationPermission())
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
