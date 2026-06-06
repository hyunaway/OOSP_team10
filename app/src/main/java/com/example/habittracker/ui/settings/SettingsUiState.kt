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
    val categoryPriorityOrder: List<String> = listOf("MEAL", "WATER", "DIGITAL", "STRETCH"),
    val isSaved: Boolean = false,
    // 신체 정보
    val userHeightCm: Float = 0f,
    val userWeightKg: Float = 0f,
    // 디버그 정보
    val isWaterReady: Boolean = false,
    val isMealReady: Boolean = false,
    val isStretchReady: Boolean = false,
    val isDigitalReady: Boolean = false,
    val resolvedBreakfastTime: String = "",
    val resolvedLunchTime: String = "",
    val resolvedDinnerTime: String = "",
    val resolvedLateNightTime: String = "",
    val resolvedStretchGoal: Int = 0,
    val resolvedWaterGoalMl: Int = 0,
    val resolvedWaterInterval: Int = 0,
    val resolvedWaterPeak: String = "",
    val resolvedStretchPreferredSlot: String = "",
    val resolvedYoutubeThreshold: Int = 0,
    val resolvedYoutubeAvgSession: Float = 0f,
    val resolvedMessageTone: String = "",
    val debugInfoText: String = "",
)
