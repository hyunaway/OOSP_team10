// 경로: com/example/habittracker/data/local/PreferenceKeys.kt
package com.example.habittracker.data.local

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val BED_TIME = stringPreferencesKey("bed_time")
    val WAKE_TIME = stringPreferencesKey("wake_time")
    val WATER_REMINDER_INTERVAL_MINUTES = intPreferencesKey("water_reminder_interval_minutes")
    val PERSONAL_LATE_NIGHT_START_TIME = stringPreferencesKey("personal_late_night_start_time")
    val DIGITAL_INTERVENTION_BASE_DURATION = intPreferencesKey("digital_intervention_base_duration")
    val PREFERRED_MESSAGE_TONE = stringPreferencesKey("preferred_message_tone")
    val WEEKDAY_WATER_PEAK_HOURS = stringPreferencesKey("weekday_water_peak_hours")
    val WEEKEND_WATER_PEAK_HOURS = stringPreferencesKey("weekend_water_peak_hours")
    val WEEKDAY_MEAL_TIME_MAP = stringPreferencesKey("weekday_meal_time_map")
    val WEEKEND_MEAL_TIME_MAP = stringPreferencesKey("weekend_meal_time_map")
    val WEEKDAY_DIGITAL_LIMIT_MAP = stringPreferencesKey("weekday_digital_limit_map")
    val WEEKEND_DIGITAL_LIMIT_MAP = stringPreferencesKey("weekend_digital_limit_map")
    val STRETCH_INACTIVE_HOURS = stringPreferencesKey("stretch_inactive_hours")
    val NOTIFICATION_FATIGUE_SCORE = floatPreferencesKey("notification_fatigue_score")
    val STREAK_DAYS = intPreferencesKey("streak_days")
    val SLEEP_PREP_WINDOW_MINUTES = intPreferencesKey("sleep_prep_window_minutes")
    val SEASONALITY_PROFILE = stringPreferencesKey("seasonality_profile")
}
