package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType

data class DailyMealPlan(
    val date: String,
    val anchorMinutes: Int,
    val expectedMeals: List<ExpectedMealWindow>,
    val skippedByLateWake: Set<MealType>,
    val planType: DailyMealPlanType,
)

data class ExpectedMealWindow(
    val type: MealType,
    val startMinutes: Int,
    val endMinutes: Int,
    val label: String,
) {
    fun contains(minutes: Int): Boolean =
        minutes in startMinutes until endMinutes
}

enum class DailyMealPlanType {
    NORMAL_DAY,
    LATE_WAKE_DAY,
    VERY_LATE_WAKE_DAY,
    EVENING_START_DAY,
}

data class MealDailyEvaluation(
    val plan: DailyMealPlan,
    val completedMealTypes: Set<MealType>,
    val missedForReview: Set<MealType>,
    val skippedByLateWake: Set<MealType>,
    val lateNightLogged: Boolean,
    val statusLevel: MealDailyStatusLevel,
    val additionalIntakeCount: Int = 0,
    val hasIrregularIntake: Boolean = false,
    val earlyNextMealCandidates: Set<MealType> = emptySet(),
)

enum class MealLogRole {
    PRIMARY_MEAL,
    ADDITIONAL_INTAKE,
    EARLY_NEXT_MEAL,
    LATE_NIGHT_EVENT,
}

data class AnalyzedMealLog(
    val log: MealLogEntity,
    val role: MealLogRole,
    val mealType: MealType,
    val minutesFromPrimary: Int?,
    val affectsNextMealWindow: Boolean,
)

data class MealLogAnalysis(
    val analyzedLogs: List<AnalyzedMealLog>,
    val primaryMealTypes: Set<MealType>,
    val additionalIntakeCount: Int,
    val earlyNextMealCandidates: Set<MealType>,
    val hasIrregularIntake: Boolean,
    val nextMealDelayMinutesByType: Map<MealType, Int>,
)
