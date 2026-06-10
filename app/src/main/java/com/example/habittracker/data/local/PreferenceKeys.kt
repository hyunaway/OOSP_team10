// 경로: com/example/habittracker/data/local/PreferenceKeys.kt
package com.example.habittracker.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val BED_TIME = stringPreferencesKey("bed_time")
    val WAKE_TIME = stringPreferencesKey("wake_time")
    val WATER_REMINDER_INTERVAL_MINUTES = intPreferencesKey("water_reminder_interval_minutes")
    val PERSONAL_LATE_NIGHT_START_TIME = stringPreferencesKey("personal_late_night_start_time")
    val DIGITAL_INTERVENTION_BASE_DURATION = intPreferencesKey("digital_intervention_base_duration")
    val PREFERRED_MESSAGE_TONE = stringPreferencesKey("preferred_message_tone")
    val WATER_PEAK_JSON = stringPreferencesKey("water_peak_json")

    val SELECTED_DIGITAL_PACKAGES = stringPreferencesKey("selected_digital_packages")
    val DIGITAL_INTERVENTION_THRESHOLD_MINUTES = intPreferencesKey("digital_intervention_threshold_minutes")
    val DIGITAL_INTERVENTION_COOLDOWN_MINUTES = intPreferencesKey("digital_intervention_cooldown_minutes")
    val STRETCH_INACTIVE_HOURS = stringPreferencesKey("stretch_inactive_hours")
    val NOTIFICATION_FATIGUE_SCORE = floatPreferencesKey("notification_fatigue_score")


    // 아바타
    val AVATAR_GENDER = stringPreferencesKey("avatar_gender")
    val USER_NAME = stringPreferencesKey("user_name")
    val HAS_ONBOARDING_COMPLETED = booleanPreferencesKey("has_completed_onboarding")
    val REGISTERED_DELIVERY_PACKAGES = stringPreferencesKey("registered_delivery_packages")
    val LAST_MEAL_REMINDER_ID = stringPreferencesKey("last_meal_reminder_id")
    val STRETCH_SLOT_AM_ENABLED = booleanPreferencesKey("stretch_slot_am_enabled")
    val STRETCH_SLOT_PM_ENABLED = booleanPreferencesKey("stretch_slot_pm_enabled")
    val STRETCH_SLOT_EVE_ENABLED = booleanPreferencesKey("stretch_slot_eve_enabled")
    val STRETCH_SLOT_NIGHT_ENABLED = booleanPreferencesKey("stretch_slot_night_enabled")
    val LAST_STRETCH_REMINDER_ID = stringPreferencesKey("last_stretch_reminder_id")
    val LAST_STRETCH_REMINDER_AT = longPreferencesKey("last_stretch_reminder_at")
    val CATEGORY_PRIORITY_ORDER = stringPreferencesKey("category_priority_order")
    val TODAY_ACTIVE_DATE = stringPreferencesKey("today_active_date")
    val TODAY_ACTIVE_STARTED_AT = longPreferencesKey("today_active_started_at")
    val LAST_USER_ACTIVITY_AT = longPreferencesKey("last_user_activity_at")

    // 신체 정보
    val USER_HEIGHT_CM = floatPreferencesKey("user_height_cm")
    val USER_WEIGHT_KG = floatPreferencesKey("user_weight_kg")
    val USER_BMI = floatPreferencesKey("user_bmi")

    // ── 개인화 분석 결과 ──────────────────────────────────────────────────────

    // 크로노타입: 기존 단순 문자열로 충분하나 개인화 파이프라인이 직접 쓰는 별도 키
    // (bedTime/wakeTime 은 사용자 설정값 → 여기는 분석으로 추론된 생체 리듬 타입)
    val USER_CHRONOTYPE = stringPreferencesKey("user_chronotype") // MORNING / EVENING / INTERMEDIATE

    // 카테고리별 개인화 준비 완료 게이트 (신규 — 단순 Boolean 4개로 분리 필요)
    // "분석 데이터가 충분히 쌓여 해당 카테고리 개인화를 적용해도 되는가"를 나타냄.
    // 하나의 필드로 4개 상태를 표현하면 비트 연산이 필요해 가독성·유지보수 저하.
    val WATER_PERSONALIZATION_READY   = booleanPreferencesKey("water_personalization_ready")
    val MEAL_PERSONALIZATION_READY    = booleanPreferencesKey("meal_personalization_ready")
    val STRETCH_PERSONALIZATION_READY = booleanPreferencesKey("stretch_personalization_ready")
    val DIGITAL_PERSONALIZATION_READY = booleanPreferencesKey("digital_personalization_ready")

    // 식사 타입별 peak 창 (신규 — 기존 WEEKDAY/WEEKEND_MEAL_TIME_MAP 은 평균 시각 문자열만 저장)
    // PeakWindow(center, rangeStart, rangeEnd, concentration) 직렬화 JSON이 필요해 별도 키 추가.
    val MEAL_BREAKFAST_PEAK_JSON  = stringPreferencesKey("meal_breakfast_peak_json")
    val MEAL_LUNCH_PEAK_JSON      = stringPreferencesKey("meal_lunch_peak_json")
    val MEAL_DINNER_PEAK_JSON     = stringPreferencesKey("meal_dinner_peak_json")
    val MEAL_LATE_NIGHT_PEAK_JSON = stringPreferencesKey("meal_late_night_peak_json")

    // 배달 앱 주문 주기 — Float? 표현이 필요하나 DataStore는 null 불가 → -1f를 sentinel로 사용
    // (기존 REGISTERED_DELIVERY_PACKAGES 는 패키지 목록만 저장, 주기 정보 없음)
    val DELIVERY_INTERVAL_DAYS = floatPreferencesKey("delivery_interval_days") // -1f = 미확정

    // 스트레칭 목표 횟수 — 기존 STRETCH_SLOT_*_ENABLED 는 on/off만, 횟수 값이 없음
    val STRETCH_GOAL_COUNT = intPreferencesKey("stretch_goal_count") // 기본 4

    // 스트레칭 선호 시간대 JSON — 기존 STRETCH_INACTIVE_HOURS 는 "비활성" 시간대,
    // 여기는 "선호(활성)" 시간대를 별도로 저장해 알림 타이밍에 활용
    val STRETCH_PREFERRED_TIME_SLOTS_JSON = stringPreferencesKey("stretch_preferred_time_slots_json")

    // 앱별 디지털 사용 프로필 JSON — 기존 키들은 전체 집계값만 저장,
    // 앱 단위 threshold·패턴을 Map<packageName, AppProfile> 형태로 직렬화해 저장 필요
    val PER_APP_PROFILE_JSON = stringPreferencesKey("per_app_profile_json")

    // 개인화 성숙도 — 분석 주기가 쌓일수록 증가하는 레벨(0=초기, 최대 5 등)
    val PERSONALIZATION_LEVEL = intPreferencesKey("personalization_level")

    // 총 활동 일수 — 개인화 레벨 판정·신뢰도 게이트에 사용
    // (기존 STREAK_DAYS 는 "연속" 일수, 여기는 누적 총 일수로 의미가 다름)
    val TOTAL_ACTIVE_DAYS = intPreferencesKey("total_active_days")
}

