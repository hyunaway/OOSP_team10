// 경로: com/example/habittracker/ui/meal/MealUiState.kt
package com.example.habittracker.ui.meal

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.model.DailyMealSummary
import com.example.habittracker.domain.model.MealTodayStatus
import com.example.habittracker.domain.usecase.meal.DailyMealPlanType
import com.example.habittracker.domain.usecase.meal.MealInterventionIntensity
import com.example.habittracker.domain.usecase.meal.MealDailyStatus

data class MealUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val todayStatus: MealTodayStatus? = null,
    val todayLogs: List<MealLogEntity> = emptyList(),
    val displayLogs: List<MealLogEntity> = emptyList(),
    val history: List<DailyMealSummary> = emptyList(),
    val classificationMessage: String? = null,
    val dailyStatusMessage: String? = null,
    val dailyMealStatus: MealDailyStatus? = null,
    val mealLogsExpanded: Boolean = false,
    val transientMessage: String? = null,
    val expectedMealCount: Int = 0,
    val completedExpectedMealCount: Int = 0,
    val skippedByLateWake: Set<MealType> = emptySet(),
    val mealPlanType: DailyMealPlanType? = null,
    val currentActionableMealType: MealType? = null,
    val currentMealWindowType: MealType? = null,
    val currentMealInterventionReason: String? = null,
    val currentMealInterventionIntensity: MealInterventionIntensity = MealInterventionIntensity.NONE,
    val mealPlanMessage: String? = null,
    val additionalIntakeCount: Int = 0,
    val hasIrregularIntake: Boolean = false,
)
