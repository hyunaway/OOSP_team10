package com.example.habittracker.domain.usecase.activity

import com.example.habittracker.data.local.UserPreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkUserActiveUseCase @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
) {
    @Suppress("UNUSED_PARAMETER")
    suspend operator fun invoke(
        source: String = SOURCE_UNKNOWN,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        userPreferenceManager.markUserActive(nowMillis)
    }

    companion object {
        const val SOURCE_APP_RESUME = "app_resume"
        const val SOURCE_WATER_LOG = "water_log"
        const val SOURCE_MEAL_LOG = "meal_log"
        const val SOURCE_STRETCH_LOG = "stretch_log"
        const val SOURCE_UNKNOWN = "unknown"
    }
}
