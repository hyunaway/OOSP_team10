package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.model.MealType
import javax.inject.Inject
import kotlin.math.abs

data class MealWindow(
    val mealType: MealType,
    val startMinutes: Int,
    val endMinutes: Int,
    val centerMinutes: Int,
) {
    fun contains(minutes: Int): Boolean {
        val normalized = normalize(minutes)
        return if (startMinutes <= endMinutes) {
            normalized in startMinutes..endMinutes
        } else {
            normalized >= startMinutes || normalized <= endMinutes
        }
    }

    fun minutesSinceStart(minutes: Int): Int {
        val normalized = normalize(minutes)
        return if (normalized >= startMinutes) {
            normalized - startMinutes
        } else {
            normalized + MealWindowCalculator.MINUTES_PER_DAY - startMinutes
        }
    }
}

class MealWindowCalculator @Inject constructor() {

    fun calculate(
        wakeTimeMinutes: Int,
        bedTimeMinutes: Int,
    ): List<MealWindow> {
        val wake = if (wakeTimeMinutes < 0) DEFAULT_WAKE_TIME_MINUTES else wakeTimeMinutes
        val bed = when {
            bedTimeMinutes == 0 -> MINUTES_PER_DAY
            bedTimeMinutes < 0 -> MINUTES_PER_DAY
            else -> bedTimeMinutes
        }

        return listOf(
            createWindow(MealType.BREAKFAST, wake + 30, wake + 180),
            createWindow(MealType.LUNCH, wake + 300, wake + 480),
            createWindow(MealType.DINNER, bed - 360, bed - 180),
        )
    }

    fun findCurrentWindow(
        windows: List<MealWindow>,
        currentMinutes: Int,
    ): MealWindow? = windows.firstOrNull { it.contains(currentMinutes) }

    fun nextWindowAfter(
        mealType: MealType,
        windows: List<MealWindow>,
    ): MealWindow? =
        when (mealType) {
            MealType.BREAKFAST -> windows.firstOrNull { it.mealType == MealType.LUNCH }
            MealType.LUNCH -> windows.firstOrNull { it.mealType == MealType.DINNER }
            MealType.DINNER,
            MealType.LATE_NIGHT -> null
        }

    fun nearestWindow(
        windows: List<MealWindow>,
        currentMinutes: Int,
    ): MealWindow = windows.minBy { abs(signedCircularDistance(currentMinutes, it.centerMinutes)) }

    fun signedCircularDistance(current: Int, target: Int): Int {
        var diff = normalize(current) - normalize(target)
        if (diff > MINUTES_PER_DAY / 2) diff -= MINUTES_PER_DAY
        if (diff < -MINUTES_PER_DAY / 2) diff += MINUTES_PER_DAY
        return diff
    }

    private fun createWindow(
        mealType: MealType,
        startMinutes: Int,
        endMinutes: Int,
    ): MealWindow {
        val start = normalize(startMinutes)
        val end = normalize(endMinutes)
        val duration = if (start <= end) {
            end - start
        } else {
            end + MINUTES_PER_DAY - start
        }
        return MealWindow(
            mealType = mealType,
            startMinutes = start,
            endMinutes = end,
            centerMinutes = normalize(start + duration / 2),
        )
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val DEFAULT_WAKE_TIME_MINUTES = 8 * 60
    }
}

internal fun normalize(minutes: Int): Int =
    ((minutes % MealWindowCalculator.MINUTES_PER_DAY) + MealWindowCalculator.MINUTES_PER_DAY) %
        MealWindowCalculator.MINUTES_PER_DAY
