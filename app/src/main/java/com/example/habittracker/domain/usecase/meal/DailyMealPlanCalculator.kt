package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.model.MealType
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class DailyMealPlanCalculator @Inject constructor() {

    fun calculate(
        date: String,
        wakeTimeMinutes: Int,
        bedTimeMinutes: Int,
        todayActiveStartedAtMillis: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DailyMealPlan {
        val wake = wakeTimeMinutes.takeIf { it in 0 until MINUTES_PER_DAY } ?: DEFAULT_WAKE_TIME_MINUTES
        val activeStart = todayActiveStartedAtMillis
            ?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalTime() }
            ?.let { it.hour * 60 + it.minute }
        val anchor = activeStart?.let { max(wake, it) } ?: wake
        val bed = normalizeBedTime(bedTimeMinutes, anchor)

        return when {
            anchor < LATE_WAKE_START -> normalDay(date, anchor, bed)
            anchor < VERY_LATE_WAKE_START -> lateWakeDay(date, anchor, bed)
            anchor < EVENING_START -> veryLateWakeDay(date, anchor, bed)
            else -> eveningStartDay(date, anchor, bed)
        }
    }

    private fun normalDay(date: String, anchor: Int, bed: Int): DailyMealPlan {
        val windows = buildList {
            addIfValid(MealType.BREAKFAST, anchor + 30, min(anchor + 180, LUNCH_FIXED_START), "아침")
            addIfValid(MealType.LUNCH, LUNCH_FIXED_START, LUNCH_FIXED_END, "점심")
            addIfValid(MealType.DINNER, DINNER_FIXED_START, min(DINNER_FIXED_END, bed - 60), "저녁")
        }.withoutOverlaps()
        return DailyMealPlan(
            date = date,
            anchorMinutes = anchor,
            expectedMeals = windows,
            skippedByLateWake = emptySet(),
            planType = DailyMealPlanType.NORMAL_DAY,
        )
    }

    private fun lateWakeDay(date: String, anchor: Int, bed: Int): DailyMealPlan {
        val lunch = mealWindow(MealType.LUNCH, anchor + 30, min(anchor + 180, bed - 180), "점심")
        val dinnerStart = max(DINNER_FIXED_START, (lunch?.endMinutes ?: anchor) + MIN_GAP_BETWEEN_MEALS)
        val dinner = mealWindow(MealType.DINNER, dinnerStart, min(DINNER_FIXED_END, bed - 120), "저녁")
        return DailyMealPlan(
            date = date,
            anchorMinutes = anchor,
            expectedMeals = listOfNotNull(lunch, dinner).withoutOverlaps(),
            skippedByLateWake = setOf(MealType.BREAKFAST),
            planType = DailyMealPlanType.LATE_WAKE_DAY,
        )
    }

    private fun veryLateWakeDay(date: String, anchor: Int, bed: Int): DailyMealPlan {
        val lunch = mealWindow(MealType.LUNCH, anchor + 30, min(anchor + 150, bed - 180), "늦은 점심")
        val dinnerStart = max((lunch?.endMinutes ?: anchor) + MIN_GAP_BETWEEN_MEALS, DINNER_FIXED_START)
        val dinner = mealWindow(MealType.DINNER, dinnerStart, min(bed - 60, dinnerStart + 150), "저녁")
        val windows = listOfNotNull(lunch, dinner).withoutOverlaps()
        return DailyMealPlan(
            date = date,
            anchorMinutes = anchor,
            expectedMeals = windows,
            skippedByLateWake = setOf(MealType.BREAKFAST),
            planType = DailyMealPlanType.VERY_LATE_WAKE_DAY,
        )
    }

    private fun eveningStartDay(date: String, anchor: Int, bed: Int): DailyMealPlan {
        val dinner = mealWindow(MealType.DINNER, anchor + 30, min(bed - 60, anchor + 180), "저녁")
        return DailyMealPlan(
            date = date,
            anchorMinutes = anchor,
            expectedMeals = listOfNotNull(dinner),
            skippedByLateWake = setOf(MealType.BREAKFAST, MealType.LUNCH),
            planType = DailyMealPlanType.EVENING_START_DAY,
        )
    }

    private fun MutableList<ExpectedMealWindow>.addIfValid(
        type: MealType,
        startMinutes: Int,
        endMinutes: Int,
        label: String,
    ) {
        mealWindow(type, startMinutes, endMinutes, label)?.let { add(it) }
    }

    private fun mealWindow(
        type: MealType,
        startMinutes: Int,
        endMinutes: Int,
        label: String,
    ): ExpectedMealWindow? {
        val start = startMinutes.coerceIn(0, MINUTES_PER_DAY)
        val end = endMinutes.coerceIn(0, MINUTES_PER_DAY)
        return if (start < end) {
            ExpectedMealWindow(type, start, end, label)
        } else {
            null
        }
    }

    private fun List<ExpectedMealWindow>.withoutOverlaps(): List<ExpectedMealWindow> {
        val sorted = sortedBy { it.startMinutes }
        val result = mutableListOf<ExpectedMealWindow>()
        sorted.forEach { window ->
            if (result.lastOrNull()?.endMinutes?.let { it <= window.startMinutes } != false) {
                result.add(window)
            }
        }
        return result
    }

    private fun normalizeBedTime(bedTimeMinutes: Int, anchorMinutes: Int): Int =
        when {
            bedTimeMinutes == 0 -> MINUTES_PER_DAY
            bedTimeMinutes < 0 -> MINUTES_PER_DAY
            bedTimeMinutes > MINUTES_PER_DAY -> MINUTES_PER_DAY
            bedTimeMinutes <= anchorMinutes -> MINUTES_PER_DAY
            else -> bedTimeMinutes
        }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
        private const val DEFAULT_WAKE_TIME_MINUTES = 8 * 60
        private const val LATE_WAKE_START = 11 * 60
        private const val VERY_LATE_WAKE_START = 14 * 60
        private const val EVENING_START = 17 * 60
        private const val LUNCH_FIXED_START = 12 * 60
        private const val LUNCH_FIXED_END = 14 * 60 + 30
        private const val DINNER_FIXED_START = 18 * 60
        private const val DINNER_FIXED_END = 20 * 60 + 30
        private const val MIN_GAP_BETWEEN_MEALS = 3 * 60
    }
}
