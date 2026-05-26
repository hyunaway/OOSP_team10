package com.example.habittracker

import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.domain.usecase.water.WaterStatusCalculator
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterStatusCalculatorTest {

    private val calculator = WaterStatusCalculator()

    @Test
    fun calculate_returnsOneEighthOfGoalAfterFirstMorningHour() {
        val result = calculator.calculate(
            wakeMinutes = 9 * 60,
            bedMinutes = 23 * 60,
            goalMl = 2000,
            currentAmountMl = 0,
            lastDrankAt = null,
            nowMillis = todayAt(hour = 10, minute = 0),
        )

        assertEquals(250, result.recommendedAmountMl)
        assertEquals(WaterShortageLevel.LIGHT, result.shortageLevel)
    }

    @Test
    fun calculate_returnsQuarterOfGoalAfterTwoMorningHours() {
        val result = calculator.calculate(
            wakeMinutes = 9 * 60,
            bedMinutes = 23 * 60,
            goalMl = 2000,
            currentAmountMl = 0,
            lastDrankAt = null,
            nowMillis = todayAt(hour = 11, minute = 0),
        )

        assertEquals(500, result.recommendedAmountMl)
        assertEquals(WaterShortageLevel.MEDIUM, result.shortageLevel)
    }

    @Test
    fun calculate_increasesGraduallyDuringMiddleSegment() {
        val result = calculator.calculate(
            wakeMinutes = 9 * 60,
            bedMinutes = 23 * 60,
            goalMl = 2000,
            currentAmountMl = 700,
            lastDrankAt = null,
            nowMillis = todayAt(hour = 16, minute = 0),
        )

        assertEquals(1100, result.recommendedAmountMl)
        assertEquals(400, result.shortageMl)
        assertEquals(WaterShortageLevel.MEDIUM, result.shortageLevel)
    }

    @Test
    fun calculate_increasesTowardGoalDuringEveningSegment() {
        val result = calculator.calculate(
            wakeMinutes = 9 * 60,
            bedMinutes = 23 * 60,
            goalMl = 2000,
            currentAmountMl = 1200,
            lastDrankAt = null,
            nowMillis = todayAt(hour = 22, minute = 0),
        )

        assertEquals(1850, result.recommendedAmountMl)
        assertEquals(650, result.shortageMl)
        assertEquals(WaterShortageLevel.MEDIUM, result.shortageLevel)
    }

    @Test
    fun calculate_returnsNeedWater_whenCurrentAmountIsBehindRecommendedAmount() {
        val now = todayAt(hour = 12, minute = 0)
        val result = calculator.calculate(
            wakeMinutes = 8 * 60,
            bedMinutes = 23 * 60,
            goalMl = 2000,
            currentAmountMl = 200,
            lastDrankAt = now - 2 * 60 * 60 * 1000L,
            nowMillis = now,
        )

        assertTrue(result.isNeedWater)
        assertEquals(718, result.recommendedAmountMl)
        assertEquals(518, result.shortageMl)
        assertEquals(WaterShortageLevel.MEDIUM, result.shortageLevel)
    }

    @Test
    fun calculate_doesNotNeedWater_whenLastDrinkWasTooRecent() {
        val now = todayAt(hour = 12, minute = 0)
        val result = calculator.calculate(
            wakeMinutes = 8 * 60,
            bedMinutes = 23 * 60,
            goalMl = 2000,
            currentAmountMl = 200,
            lastDrankAt = now - 30 * 60 * 1000L,
            nowMillis = now,
        )

        assertFalse(result.isNeedWater)
        assertEquals(518, result.shortageMl)
    }

    @Test
    fun calculate_doesNotNeedWater_beforeWakeTime() {
        val result = calculator.calculate(
            wakeMinutes = 8 * 60,
            bedMinutes = 23 * 60,
            goalMl = 2000,
            currentAmountMl = 0,
            lastDrankAt = null,
            nowMillis = todayAt(hour = 7, minute = 30),
        )

        assertFalse(result.isNeedWater)
        assertEquals(0, result.recommendedAmountMl)
    }

    @Test
    fun calculate_doesNotNeedWater_whenGoalIsAlreadyReached() {
        val result = calculator.calculate(
            wakeMinutes = 8 * 60,
            bedMinutes = 23 * 60,
            goalMl = 2000,
            currentAmountMl = 2000,
            lastDrankAt = null,
            nowMillis = todayAt(hour = 15, minute = 0),
        )

        assertFalse(result.isNeedWater)
        assertEquals(WaterShortageLevel.NONE, result.shortageLevel)
    }

    @Test
    fun calculate_fallsBackToLinearProgress_whenActiveTimeIsShort() {
        val result = calculator.calculate(
            wakeMinutes = 9 * 60,
            bedMinutes = 14 * 60,
            goalMl = 2000,
            currentAmountMl = 0,
            lastDrankAt = null,
            nowMillis = todayAt(hour = 10, minute = 15),
        )

        assertEquals(500, result.recommendedAmountMl)
    }

    private fun todayAt(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
