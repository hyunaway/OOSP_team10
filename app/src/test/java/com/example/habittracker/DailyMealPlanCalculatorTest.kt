package com.example.habittracker

import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.usecase.meal.DailyMealPlanCalculator
import com.example.habittracker.domain.usecase.meal.DailyMealPlanType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyMealPlanCalculatorTest {

    private val calculator = DailyMealPlanCalculator()
    private val zoneId: ZoneId = ZoneId.systemDefault()

    @Test
    fun calculate_returnsNormalDayForMorningAnchor() {
        val plan = calculator.calculate(
            date = TEST_DATE,
            wakeTimeMinutes = 8 * 60,
            bedTimeMinutes = 23 * 60,
            todayActiveStartedAtMillis = activeAt(8, 0),
            zoneId = zoneId,
        )

        assertEquals(DailyMealPlanType.NORMAL_DAY, plan.planType)
        assertEquals(listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER), plan.expectedMeals.map { it.type })
        assertTrue(plan.skippedByLateWake.isEmpty())
    }

    @Test
    fun calculate_skipsBreakfastForNoonStart() {
        val plan = calculator.calculate(
            date = TEST_DATE,
            wakeTimeMinutes = 8 * 60,
            bedTimeMinutes = 23 * 60,
            todayActiveStartedAtMillis = activeAt(12, 0),
            zoneId = zoneId,
        )

        assertEquals(DailyMealPlanType.LATE_WAKE_DAY, plan.planType)
        assertEquals(listOf(MealType.LUNCH, MealType.DINNER), plan.expectedMeals.map { it.type })
        assertEquals(setOf(MealType.BREAKFAST), plan.skippedByLateWake)
    }

    @Test
    fun calculate_doesNotOverlapMealsForVeryLateStart() {
        val plan = calculator.calculate(
            date = TEST_DATE,
            wakeTimeMinutes = 8 * 60,
            bedTimeMinutes = 23 * 60,
            todayActiveStartedAtMillis = activeAt(15, 0),
            zoneId = zoneId,
        )

        assertEquals(DailyMealPlanType.VERY_LATE_WAKE_DAY, plan.planType)
        assertTrue(MealType.BREAKFAST in plan.skippedByLateWake)
        plan.expectedMeals.zipWithNext().forEach { (first, second) ->
            assertTrue(first.endMinutes <= second.startMinutes)
        }
    }

    @Test
    fun calculate_usesDinnerOnlyForEveningStart() {
        val plan = calculator.calculate(
            date = TEST_DATE,
            wakeTimeMinutes = 8 * 60,
            bedTimeMinutes = 23 * 60,
            todayActiveStartedAtMillis = activeAt(18, 0),
            zoneId = zoneId,
        )

        assertEquals(DailyMealPlanType.EVENING_START_DAY, plan.planType)
        assertEquals(listOf(MealType.DINNER), plan.expectedMeals.map { it.type })
        assertEquals(setOf(MealType.BREAKFAST, MealType.LUNCH), plan.skippedByLateWake)
    }

    private fun activeAt(hour: Int, minute: Int): Long =
        LocalDateTime.of(LocalDate.parse(TEST_DATE), LocalTime.of(hour, minute))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    companion object {
        private const val TEST_DATE = "2026-06-04"
    }
}
