// 경로: com/example/habittracker/ui/settings/SettingsUiState.kt
package com.example.habittracker.ui.settings

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.ui.avatar.AvatarGender

data class SettingsUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val notificationPermissionGranted: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val bedTime: String = UserPreferenceManager.DEFAULT_BED_TIME,
    val wakeTime: String = UserPreferenceManager.DEFAULT_WAKE_TIME,
    val waterReminderIntervalMinutes: Int = UserPreferenceManager.DEFAULT_WATER_REMINDER_INTERVAL_MINUTES,
    val selectedDigitalPackages: Set<String> = emptySet(),
    val digitalInterventionThresholdMinutes: Int = UserPreferenceManager.DEFAULT_DIGITAL_INTERVENTION_THRESHOLD_MINUTES,
    val digitalInterventionCooldownMinutes: Int = UserPreferenceManager.DEFAULT_DIGITAL_INTERVENTION_COOLDOWN_MINUTES,
    val preferredMessageTone: String = UserPreferenceManager.DEFAULT_PREFERRED_MESSAGE_TONE,
    val avatarGender: AvatarGender = AvatarGender.MALE,
    val userName: String = UserPreferenceManager.DEFAULT_USER_NAME,
    val isSaved: Boolean = false,
)
