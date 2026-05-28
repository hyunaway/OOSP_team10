// 경로: com/example/habittracker/data/local/UserPreferenceManager.kt
package com.example.habittracker.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferenceManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // ── Getters ──────────────────────────────────────────────────────────────

    val bedTimeFlow: Flow<String> = dataStore.data.map { it[PreferenceKeys.BED_TIME] ?: DEFAULT_BED_TIME }
    val wakeTimeFlow: Flow<String> = dataStore.data.map { it[PreferenceKeys.WAKE_TIME] ?: DEFAULT_WAKE_TIME }
    val waterReminderIntervalMinutesFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.WATER_REMINDER_INTERVAL_MINUTES] ?: DEFAULT_WATER_REMINDER_INTERVAL_MINUTES
    }
    val lateNightStartTimeFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.PERSONAL_LATE_NIGHT_START_TIME] ?: DEFAULT_LATE_NIGHT_START_TIME
    }
    val digitalInterventionBaseDurationFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.DIGITAL_INTERVENTION_BASE_DURATION] ?: DEFAULT_DIGITAL_INTERVENTION_BASE_DURATION
    }
    val preferredMessageToneFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.PREFERRED_MESSAGE_TONE] ?: DEFAULT_PREFERRED_MESSAGE_TONE
    }
    val weekdayWaterPeakHoursFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.WEEKDAY_WATER_PEAK_HOURS] ?: ""
    }
    val weekendWaterPeakHoursFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.WEEKEND_WATER_PEAK_HOURS] ?: ""
    }
    val weekdayMealTimeMapFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.WEEKDAY_MEAL_TIME_MAP] ?: ""
    }
    val weekendMealTimeMapFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.WEEKEND_MEAL_TIME_MAP] ?: ""
    }
    val weekdayDigitalLimitMapFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.WEEKDAY_DIGITAL_LIMIT_MAP] ?: ""
    }
    val weekendDigitalLimitMapFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.WEEKEND_DIGITAL_LIMIT_MAP] ?: ""
    }
    val selectedDigitalPackagesFlow: Flow<Set<String>> = dataStore.data.map {
        parsePackageSet(it[PreferenceKeys.SELECTED_DIGITAL_PACKAGES] ?: "")
    }
    val digitalInterventionThresholdMinutesFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.DIGITAL_INTERVENTION_THRESHOLD_MINUTES] ?: DEFAULT_DIGITAL_INTERVENTION_THRESHOLD_MINUTES
    }
    val digitalInterventionCooldownMinutesFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.DIGITAL_INTERVENTION_COOLDOWN_MINUTES] ?: DEFAULT_DIGITAL_INTERVENTION_COOLDOWN_MINUTES
    }
    val stretchInactiveHoursFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.STRETCH_INACTIVE_HOURS] ?: ""
    }
    val notificationFatigueScoreFlow: Flow<Float> = dataStore.data.map {
        it[PreferenceKeys.NOTIFICATION_FATIGUE_SCORE] ?: DEFAULT_NOTIFICATION_FATIGUE_SCORE
    }
    val streakDaysFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.STREAK_DAYS] ?: DEFAULT_STREAK_DAYS
    }
    val sleepPrepWindowMinutesFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.SLEEP_PREP_WINDOW_MINUTES] ?: DEFAULT_SLEEP_PREP_WINDOW_MINUTES
    }
    val seasonalityProfileFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.SEASONALITY_PROFILE] ?: ""
    }
    val avatarGenderFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.AVATAR_GENDER] ?: DEFAULT_AVATAR_GENDER
    }
    val userNameFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.USER_NAME] ?: DEFAULT_USER_NAME
    }
    val hasCompletedOnboardingFlow: Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.HAS_ONBOARDING_COMPLETED] ?: false
    }

    // ── Update functions ─────────────────────────────────────────────────────

    suspend fun updateBedTime(value: String) {
        dataStore.edit { it[PreferenceKeys.BED_TIME] = value }
    }

    suspend fun updateWakeTime(value: String) {
        dataStore.edit { it[PreferenceKeys.WAKE_TIME] = value }
    }

    suspend fun updateWaterReminderIntervalMinutes(value: Int) {
        dataStore.edit { it[PreferenceKeys.WATER_REMINDER_INTERVAL_MINUTES] = value }
    }

    suspend fun updateLateNightStartTime(value: String) {
        dataStore.edit { it[PreferenceKeys.PERSONAL_LATE_NIGHT_START_TIME] = value }
    }

    suspend fun updateDigitalInterventionBaseDuration(value: Int) {
        dataStore.edit { it[PreferenceKeys.DIGITAL_INTERVENTION_BASE_DURATION] = value }
    }

    suspend fun updatePreferredMessageTone(value: String) {
        dataStore.edit { it[PreferenceKeys.PREFERRED_MESSAGE_TONE] = value }
    }

    suspend fun updateWeekdayWaterPeakHours(value: String) {
        dataStore.edit { it[PreferenceKeys.WEEKDAY_WATER_PEAK_HOURS] = value }
    }

    suspend fun updateWeekendWaterPeakHours(value: String) {
        dataStore.edit { it[PreferenceKeys.WEEKEND_WATER_PEAK_HOURS] = value }
    }

    suspend fun updateWeekdayMealTimeMap(value: String) {
        dataStore.edit { it[PreferenceKeys.WEEKDAY_MEAL_TIME_MAP] = value }
    }

    suspend fun updateWeekendMealTimeMap(value: String) {
        dataStore.edit { it[PreferenceKeys.WEEKEND_MEAL_TIME_MAP] = value }
    }

    suspend fun updateWeekdayDigitalLimitMap(value: String) {
        dataStore.edit { it[PreferenceKeys.WEEKDAY_DIGITAL_LIMIT_MAP] = value }
    }

    suspend fun updateWeekendDigitalLimitMap(value: String) {
        dataStore.edit { it[PreferenceKeys.WEEKEND_DIGITAL_LIMIT_MAP] = value }
    }

    suspend fun updateSelectedDigitalPackages(value: Set<String>) {
        dataStore.edit {
            it[PreferenceKeys.SELECTED_DIGITAL_PACKAGES] = value.sorted().joinToString(",")
        }
    }

    suspend fun updateDigitalInterventionThresholdMinutes(value: Int) {
        dataStore.edit { it[PreferenceKeys.DIGITAL_INTERVENTION_THRESHOLD_MINUTES] = value }
    }

    suspend fun updateDigitalInterventionCooldownMinutes(value: Int) {
        dataStore.edit { it[PreferenceKeys.DIGITAL_INTERVENTION_COOLDOWN_MINUTES] = value }
    }

    suspend fun updateStretchInactiveHours(value: String) {
        dataStore.edit { it[PreferenceKeys.STRETCH_INACTIVE_HOURS] = value }
    }

    suspend fun updateNotificationFatigueScore(value: Float) {
        dataStore.edit { it[PreferenceKeys.NOTIFICATION_FATIGUE_SCORE] = value }
    }

    suspend fun updateStreakDays(value: Int) {
        dataStore.edit { it[PreferenceKeys.STREAK_DAYS] = value }
    }

    suspend fun updateSleepPrepWindowMinutes(value: Int) {
        dataStore.edit { it[PreferenceKeys.SLEEP_PREP_WINDOW_MINUTES] = value }
    }

    suspend fun updateSeasonalityProfile(value: String) {
        dataStore.edit { it[PreferenceKeys.SEASONALITY_PROFILE] = value }
    }

    suspend fun updateAvatarGender(value: String) {
        dataStore.edit { it[PreferenceKeys.AVATAR_GENDER] = value }
    }

    suspend fun updateUserName(value: String) {
        dataStore.edit { it[PreferenceKeys.USER_NAME] = value }
    }

    suspend fun updateHasCompletedOnboarding(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.HAS_ONBOARDING_COMPLETED] = value }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    fun getBedTimeAsMinutes(): Flow<Int> = bedTimeFlow.map { parseTimeToMinutes(it) }

    fun getWakeTimeAsMinutes(): Flow<Int> = wakeTimeFlow.map { parseTimeToMinutes(it) }

    private fun parseTimeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (_: Exception) {
            0
        }
    }

    private fun parsePackageSet(value: String): Set<String> =
        value.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    // ── Defaults ─────────────────────────────────────────────────────────────

    companion object {
        const val DEFAULT_BED_TIME = "23:00"
        const val DEFAULT_WAKE_TIME = "08:00"
        const val DEFAULT_WATER_REMINDER_INTERVAL_MINUTES = 180
        const val DEFAULT_LATE_NIGHT_START_TIME = "22:00"
        const val DEFAULT_DIGITAL_INTERVENTION_BASE_DURATION = 30
        const val DEFAULT_DIGITAL_INTERVENTION_THRESHOLD_MINUTES = 30
        const val DEFAULT_DIGITAL_INTERVENTION_COOLDOWN_MINUTES = 60
        const val DEFAULT_PREFERRED_MESSAGE_TONE = "EMPATHY"
        const val DEFAULT_NOTIFICATION_FATIGUE_SCORE = 0.0f
        const val DEFAULT_STREAK_DAYS = 0
        const val DEFAULT_SLEEP_PREP_WINDOW_MINUTES = 60
        const val DEFAULT_AVATAR_GENDER = "MALE"
        const val DEFAULT_USER_NAME = ""
    }
}
