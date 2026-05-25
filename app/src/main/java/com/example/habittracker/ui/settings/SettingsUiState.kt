// 경로: com/example/habittracker/ui/settings/SettingsUiState.kt
package com.example.habittracker.ui.settings

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.ui.avatar.AvatarGender

data class SettingsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val bedTime: String = UserPreferenceManager.DEFAULT_BED_TIME,
    val wakeTime: String = UserPreferenceManager.DEFAULT_WAKE_TIME,
    val waterReminderIntervalMinutes: Int = UserPreferenceManager.DEFAULT_WATER_REMINDER_INTERVAL_MINUTES,
    val preferredMessageTone: String = UserPreferenceManager.DEFAULT_PREFERRED_MESSAGE_TONE,
    val avatarGender: AvatarGender = AvatarGender.MALE,
    val userName: String = UserPreferenceManager.DEFAULT_USER_NAME,
    val isSaved: Boolean = false,
)
