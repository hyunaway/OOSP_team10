package com.example.habittracker.domain.usecase.stretch

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalculatePersonalizedStretchGoalUseCase @Inject constructor() {

    operator fun invoke(
        activeStartedAtMillis: Long,
        bedTime: String,
    ): Int? {
        val activeStartMinutes = minutesOfDay(activeStartedAtMillis)
        val bedMinutes = parseBedTimeMinutes(bedTime) ?: DEFAULT_BED_MINUTES
        val availableMinutes = minutesUntilBed(activeStartMinutes, bedMinutes)
        if (availableMinutes <= 0) return null

        return when {
            availableMinutes < 180 -> 1
            availableMinutes < 270 -> 2
            availableMinutes < 360 -> 3
            else -> 4
        }
    }

    private fun minutesUntilBed(startMinutes: Int, bedMinutes: Int): Int {
        val normalizedBed = if (bedMinutes == 0) MINUTES_PER_DAY else bedMinutes
        if (normalizedBed == startMinutes) return 0
        return if (normalizedBed > startMinutes) {
            normalizedBed - startMinutes
        } else {
            normalizedBed + MINUTES_PER_DAY - startMinutes
        }
    }

    private fun minutesOfDay(timestampMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun parseBedTimeMinutes(value: String): Int? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (hour !in 0..24 || minute !in 0..59) return null
        if (hour == 24 && minute != 0) return null
        return if (hour == 0 && minute == 0) MINUTES_PER_DAY else hour * 60 + minute
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
        private const val DEFAULT_BED_MINUTES = MINUTES_PER_DAY
    }
}
