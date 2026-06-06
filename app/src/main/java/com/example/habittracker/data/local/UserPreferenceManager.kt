// 경로: com/example/habittracker/data/local/UserPreferenceManager.kt
package com.example.habittracker.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.habittracker.domain.model.PeakWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
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
    val waterPeakJsonFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.WATER_PEAK_JSON] ?: ""
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

    val avatarGenderFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.AVATAR_GENDER] ?: DEFAULT_AVATAR_GENDER
    }
    val userNameFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.USER_NAME] ?: DEFAULT_USER_NAME
    }
    val hasCompletedOnboardingFlow: Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.HAS_ONBOARDING_COMPLETED] ?: false
    }
    val lastMealReminderIdFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.LAST_MEAL_REMINDER_ID] ?: ""
    }
    val registeredDeliveryPackagesFlow: Flow<Set<String>> = dataStore.data.map {
        parsePackageSet(it[PreferenceKeys.REGISTERED_DELIVERY_PACKAGES] ?: DEFAULT_DELIVERY_PACKAGES)
    }
    val stretchSlotAmEnabledFlow: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.STRETCH_SLOT_AM_ENABLED] ?: true }
    val stretchSlotPmEnabledFlow: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.STRETCH_SLOT_PM_ENABLED] ?: true }
    val stretchSlotEveEnabledFlow: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.STRETCH_SLOT_EVE_ENABLED] ?: true }
    val stretchSlotNightEnabledFlow: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.STRETCH_SLOT_NIGHT_ENABLED] ?: true }
    val lastStretchReminderIdFlow: Flow<String> = dataStore.data.map { it[PreferenceKeys.LAST_STRETCH_REMINDER_ID] ?: "" }
    val lastStretchReminderAtFlow: Flow<Long?> = dataStore.data.map { it[PreferenceKeys.LAST_STRETCH_REMINDER_AT] }
    val categoryPriorityOrderFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.CATEGORY_PRIORITY_ORDER] ?: DEFAULT_CATEGORY_PRIORITY_ORDER
    }
    val todayActiveDateFlow: Flow<String?> = dataStore.data.map { it[PreferenceKeys.TODAY_ACTIVE_DATE] }
    val todayActiveStartedAtFlow: Flow<Long?> = dataStore.data.map { preferences ->
        val today = LocalDate.now().toString()
        if (preferences[PreferenceKeys.TODAY_ACTIVE_DATE] == today) {
            preferences[PreferenceKeys.TODAY_ACTIVE_STARTED_AT]
        } else {
            null
        }
    }
    val lastUserActivityAtFlow: Flow<Long?> = dataStore.data.map { it[PreferenceKeys.LAST_USER_ACTIVITY_AT] }

    val userHeightCmFlow: Flow<Float> = dataStore.data.map { it[PreferenceKeys.USER_HEIGHT_CM] ?: 0f }
    val userWeightKgFlow: Flow<Float> = dataStore.data.map { it[PreferenceKeys.USER_WEIGHT_KG] ?: 0f }
    val userBmiFlow: Flow<Float> = dataStore.data.map { it[PreferenceKeys.USER_BMI] ?: 0f }

    // ── 개인화 분석 결과 Flows ────────────────────────────────────────────────

    val userChronotypeFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.USER_CHRONOTYPE] ?: ""
    }

    // 카테고리별 개인화 게이트
    val waterPersonalizationReadyFlow: Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.WATER_PERSONALIZATION_READY] ?: false
    }
    val mealPersonalizationReadyFlow: Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.MEAL_PERSONALIZATION_READY] ?: false
    }
    val stretchPersonalizationReadyFlow: Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.STRETCH_PERSONALIZATION_READY] ?: false
    }
    val digitalPersonalizationReadyFlow: Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.DIGITAL_PERSONALIZATION_READY] ?: false
    }

    // 식사 타입별 PeakWindow (null = 미분석)
    val mealBreakfastPeakFlow: Flow<PeakWindow?> = dataStore.data.map {
        PeakWindow.fromJson(it[PreferenceKeys.MEAL_BREAKFAST_PEAK_JSON] ?: "")
    }
    val mealLunchPeakFlow: Flow<PeakWindow?> = dataStore.data.map {
        PeakWindow.fromJson(it[PreferenceKeys.MEAL_LUNCH_PEAK_JSON] ?: "")
    }
    val mealDinnerPeakFlow: Flow<PeakWindow?> = dataStore.data.map {
        PeakWindow.fromJson(it[PreferenceKeys.MEAL_DINNER_PEAK_JSON] ?: "")
    }
    val mealLateNightPeakFlow: Flow<PeakWindow?> = dataStore.data.map {
        PeakWindow.fromJson(it[PreferenceKeys.MEAL_LATE_NIGHT_PEAK_JSON] ?: "")
    }

    // 배달 앱 주문 주기 (-1f = 미확정)
    val deliveryIntervalDaysFlow: Flow<Float?> = dataStore.data.map {
        val v = it[PreferenceKeys.DELIVERY_INTERVAL_DAYS] ?: -1f
        if (v < 0f) null else v
    }

    // 스트레칭
    val stretchGoalCountFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.STRETCH_GOAL_COUNT] ?: DEFAULT_STRETCH_GOAL_COUNT
    }
    val stretchPreferredTimeSlotsFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.STRETCH_PREFERRED_TIME_SLOTS_JSON] ?: ""
    }

    // 디지털 앱별 프로필 JSON
    val perAppProfileJsonFlow: Flow<String> = dataStore.data.map {
        it[PreferenceKeys.PER_APP_PROFILE_JSON] ?: ""
    }

    // 개인화 성숙도
    val personalizationLevelFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.PERSONALIZATION_LEVEL] ?: 0
    }
    val totalActiveDaysFlow: Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.TOTAL_ACTIVE_DAYS] ?: 0
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

    suspend fun updateWaterPeakJson(value: String) {
        dataStore.edit { it[PreferenceKeys.WATER_PEAK_JSON] = value }
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


    suspend fun updateAvatarGender(value: String) {
        dataStore.edit { it[PreferenceKeys.AVATAR_GENDER] = value }
    }


    suspend fun updateUserName(value: String) {
        dataStore.edit { it[PreferenceKeys.USER_NAME] = value }
    }

    suspend fun updateHasCompletedOnboarding(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.HAS_ONBOARDING_COMPLETED] = value }
    }

    suspend fun updateLastMealReminderId(value: String) {
        dataStore.edit { it[PreferenceKeys.LAST_MEAL_REMINDER_ID] = value }
    }
    suspend fun updateRegisteredDeliveryPackages(value: Set<String>) {
        dataStore.edit {
            it[PreferenceKeys.REGISTERED_DELIVERY_PACKAGES] = value.sorted().joinToString(",")
        }
    }
    suspend fun updateStretchSlotAmEnabled(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.STRETCH_SLOT_AM_ENABLED] = value }
    }
    suspend fun updateStretchSlotPmEnabled(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.STRETCH_SLOT_PM_ENABLED] = value }
    }
    suspend fun updateStretchSlotEveEnabled(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.STRETCH_SLOT_EVE_ENABLED] = value }
    }
    suspend fun updateStretchSlotNightEnabled(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.STRETCH_SLOT_NIGHT_ENABLED] = value }
    }
    suspend fun updateLastStretchReminderId(value: String) {
        dataStore.edit { it[PreferenceKeys.LAST_STRETCH_REMINDER_ID] = value }
    }

    suspend fun updateLastStretchReminderAt(value: Long) {
        dataStore.edit { it[PreferenceKeys.LAST_STRETCH_REMINDER_AT] = value }
    }

    suspend fun updateBodyInfo(heightCm: Float, weightKg: Float) {
        val bmi = if (heightCm > 0f) weightKg / ((heightCm / 100f) * (heightCm / 100f)) else 0f
        dataStore.edit {
            it[PreferenceKeys.USER_HEIGHT_CM] = heightCm
            it[PreferenceKeys.USER_WEIGHT_KG] = weightKg
            it[PreferenceKeys.USER_BMI] = bmi
        }
    }

    // ── 개인화 분석 결과 update 함수 ─────────────────────────────────────────

    suspend fun updateUserChronotype(value: String) {
        dataStore.edit { it[PreferenceKeys.USER_CHRONOTYPE] = value }
    }

    suspend fun updateWaterPersonalizationReady(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.WATER_PERSONALIZATION_READY] = value }
    }

    suspend fun updateMealPersonalizationReady(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.MEAL_PERSONALIZATION_READY] = value }
    }

    suspend fun updateStretchPersonalizationReady(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.STRETCH_PERSONALIZATION_READY] = value }
    }

    suspend fun updateDigitalPersonalizationReady(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.DIGITAL_PERSONALIZATION_READY] = value }
    }

    suspend fun updateMealBreakfastPeak(peak: PeakWindow?) {
        dataStore.edit { it[PreferenceKeys.MEAL_BREAKFAST_PEAK_JSON] = peak?.toJson() ?: "" }
    }

    suspend fun updateMealLunchPeak(peak: PeakWindow?) {
        dataStore.edit { it[PreferenceKeys.MEAL_LUNCH_PEAK_JSON] = peak?.toJson() ?: "" }
    }

    suspend fun updateMealDinnerPeak(peak: PeakWindow?) {
        dataStore.edit { it[PreferenceKeys.MEAL_DINNER_PEAK_JSON] = peak?.toJson() ?: "" }
    }

    suspend fun updateMealLateNightPeak(peak: PeakWindow?) {
        dataStore.edit { it[PreferenceKeys.MEAL_LATE_NIGHT_PEAK_JSON] = peak?.toJson() ?: "" }
    }

    /** null = 배달 주기 미확정, 내부적으로 -1f 로 저장 */
    suspend fun updateDeliveryIntervalDays(value: Float?) {
        dataStore.edit { it[PreferenceKeys.DELIVERY_INTERVAL_DAYS] = value ?: -1f }
    }

    suspend fun updateStretchGoalCount(value: Int) {
        dataStore.edit { it[PreferenceKeys.STRETCH_GOAL_COUNT] = value }
    }

    suspend fun updateStretchPreferredTimeSlots(json: String) {
        dataStore.edit { it[PreferenceKeys.STRETCH_PREFERRED_TIME_SLOTS_JSON] = json }
    }

    suspend fun updatePerAppProfileJson(json: String) {
        dataStore.edit { it[PreferenceKeys.PER_APP_PROFILE_JSON] = json }
    }

    suspend fun updatePersonalizationLevel(value: Int) {
        dataStore.edit { it[PreferenceKeys.PERSONALIZATION_LEVEL] = value }
    }

    suspend fun updateTotalActiveDays(value: Int) {
        dataStore.edit { it[PreferenceKeys.TOTAL_ACTIVE_DAYS] = value }
    }

    suspend fun updateCategoryPriorityOrder(value: String) {
        dataStore.edit { it[PreferenceKeys.CATEGORY_PRIORITY_ORDER] = value }
    }

    suspend fun markUserActive(nowMillis: Long = System.currentTimeMillis()) {
        val today = LocalDate.now().toString()
        dataStore.edit { preferences ->
            val activeDate = preferences[PreferenceKeys.TODAY_ACTIVE_DATE]
            val activeStartedAt = preferences[PreferenceKeys.TODAY_ACTIVE_STARTED_AT]
            if (activeDate != today || activeStartedAt == null) {
                preferences[PreferenceKeys.TODAY_ACTIVE_DATE] = today
                preferences[PreferenceKeys.TODAY_ACTIVE_STARTED_AT] = nowMillis
            }
            preferences[PreferenceKeys.LAST_USER_ACTIVITY_AT] = nowMillis
        }
    }

    suspend fun clearAllPersonalizationData() {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.WATER_PERSONALIZATION_READY] = false
            prefs[PreferenceKeys.MEAL_PERSONALIZATION_READY] = false
            prefs[PreferenceKeys.STRETCH_PERSONALIZATION_READY] = false
            prefs[PreferenceKeys.DIGITAL_PERSONALIZATION_READY] = false
            prefs[PreferenceKeys.TOTAL_ACTIVE_DAYS] = 0
            prefs[PreferenceKeys.PERSONALIZATION_LEVEL] = 0
            
            prefs[PreferenceKeys.WATER_PEAK_JSON] = ""
            
            prefs[PreferenceKeys.MEAL_BREAKFAST_PEAK_JSON] = ""
            prefs[PreferenceKeys.MEAL_LUNCH_PEAK_JSON] = ""
            prefs[PreferenceKeys.MEAL_DINNER_PEAK_JSON] = ""
            prefs[PreferenceKeys.MEAL_LATE_NIGHT_PEAK_JSON] = ""
            
            prefs[PreferenceKeys.DELIVERY_INTERVAL_DAYS] = -1f
            prefs[PreferenceKeys.STRETCH_GOAL_COUNT] = 0
            prefs[PreferenceKeys.STRETCH_PREFERRED_TIME_SLOTS_JSON] = ""
            prefs[PreferenceKeys.PER_APP_PROFILE_JSON] = ""
            
            prefs[PreferenceKeys.USER_CHRONOTYPE] = ""
            prefs[PreferenceKeys.NOTIFICATION_FATIGUE_SCORE] = 0.0f
        }
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
        const val DEFAULT_AVATAR_GENDER = "MALE"
        const val DEFAULT_USER_NAME = ""
        const val DEFAULT_DELIVERY_PACKAGES = "com.sample.baemin,com.sample.coupangeats"
        const val DEFAULT_CATEGORY_PRIORITY_ORDER = "MEAL,WATER,DIGITAL,STRETCH"
        // 개인화 기본값
        const val DEFAULT_STRETCH_GOAL_COUNT = 4
    }
}
