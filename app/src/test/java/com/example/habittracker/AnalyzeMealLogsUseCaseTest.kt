package com.example.habittracker

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.usecase.meal.AnalyzeMealLogsUseCase
import com.example.habittracker.domain.usecase.meal.DailyMealPlan
import com.example.habittracker.domain.usecase.meal.DailyMealPlanType
import com.example.habittracker.domain.usecase.meal.ExpectedMealWindow
import com.example.habittracker.domain.usecase.meal.MealLogRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeMealLogsUseCaseTest {

    private val useCase = AnalyzeMealLogsUseCase()
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val plan = DailyMealPlan(
        date = TEST_DATE,
        anchorMinutes = 8 * 60,
        expectedMeals = listOf(
            ExpectedMealWindow(MealType.BREAKFAST, 8 * 60 + 30, 10 * 60, "아침"),
            ExpectedMealWindow(MealType.LUNCH, 12 * 60, 14 * 60 + 30, "점심"),
            ExpectedMealWindow(MealType.DINNER, 18 * 60, 20 * 60 + 30, "저녁"),
        ),
        skippedByLateWake = emptySet(),
        planType = DailyMealPlanType.NORMAL_DAY,
    )

    @Test
    fun invoke_marksFirstOrdinaryMealAsPrimary() {
        val analysis = useCase(
            logs = listOf(mealLog(MealType.LUNCH, 12, 30)),
            plan = plan,
            zoneId = zoneId,
        )

        assertEquals(setOf(MealType.LUNCH), analysis.primaryMealTypes)
        assertEquals(MealLogRole.PRIMARY_MEAL, analysis.analyzedLogs.single().role)
    }

    @Test
    fun invoke_marksSecondSameMealTypeAsAdditionalIntake() {
        val analysis = useCase(
            logs = listOf(
                mealLog(MealType.LUNCH, 12, 30),
                mealLog(MealType.LUNCH, 14, 0),
            ),
            plan = plan,
            zoneId = zoneId,
        )

        assertEquals(setOf(MealType.LUNCH), analysis.primaryMealTypes)
        assertEquals(1, analysis.additionalIntakeCount)
        assertEquals(MealLogRole.ADDITIONAL_INTAKE, analysis.analyzedLogs.last().role)
        assertFalse(analysis.hasIrregularIntake)
    }

    @Test
    fun invoke_marksLateNightAsLateNightEvent() {
        val analysis = useCase(
            logs = listOf(mealLog(MealType.LATE_NIGHT, 1, 0, isLateNight = true)),
            plan = plan,
            zoneId = zoneId,
        )

        assertTrue(analysis.primaryMealTypes.isEmpty())
        assertEquals(MealLogRole.LATE_NIGHT_EVENT, analysis.analyzedLogs.single().role)
    }

    @Test
    fun invoke_marksAdditionalLogNearNextWindowAsEarlyNextMealCandidate() {
        val analysis = useCase(
            logs = listOf(
                mealLog(MealType.LUNCH, 12, 30),
                mealLog(MealType.LUNCH, 16, 50),
            ),
            plan = plan,
            zoneId = zoneId,
        )

        val additional = analysis.analyzedLogs.last()
        assertEquals(MealLogRole.EARLY_NEXT_MEAL, additional.role)
        assertTrue(additional.affectsNextMealWindow)
        assertEquals(setOf(MealType.DINNER), analysis.earlyNextMealCandidates)
        assertEquals(60, analysis.nextMealDelayMinutesByType[MealType.DINNER])
    }

    @Test
    fun invoke_marksTwoOrMoreAdditionalLogsAsIrregularIntake() {
        val analysis = useCase(
            logs = listOf(
                mealLog(MealType.LUNCH, 12, 30),
                mealLog(MealType.LUNCH, 14, 0),
                mealLog(MealType.LUNCH, 16, 50),
            ),
            plan = plan,
            zoneId = zoneId,
        )

        assertEquals(2, analysis.additionalIntakeCount)
        assertTrue(analysis.hasIrregularIntake)
        assertEquals(90, analysis.nextMealDelayMinutesByType[MealType.DINNER])
    }

    private fun mealLog(
        type: MealType,
        hour: Int,
        minute: Int,
        isLateNight: Boolean = false,
    ): MealLogEntity =
        MealLogEntity(
            timestamp = LocalDateTime.of(LocalDate.parse(TEST_DATE), LocalTime.of(hour, minute))
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli(),
            type = type,
            isLateNight = isLateNight,
            viaDeliveryApp = false,
            source = "test",
            mealDate = TEST_DATE,
            recordedTime = "%02d:%02d".format(hour, minute),
        )

    companion object {
        private const val TEST_DATE = "2026-06-04"
    }
}
