// 경로: com/example/habittracker/ui/settings/SettingsUiState.kt
package com.example.habittracker.ui.settings

import com.example.habittracker.data.local.UserPreferenceManager

data class SettingsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val bedTime: String = UserPreferenceManager.DEFAULT_BED_TIME,
    val wakeTime: String = UserPreferenceManager.DEFAULT_WAKE_TIME,
    val waterReminderIntervalMinutes: Int = UserPreferenceManager.DEFAULT_WATER_REMINDER_INTERVAL_MINUTES,
    val preferredMessageTone: String = UserPreferenceManager.DEFAULT_PREFERRED_MESSAGE_TONE,
    val isSaved: Boolean = false,
)
