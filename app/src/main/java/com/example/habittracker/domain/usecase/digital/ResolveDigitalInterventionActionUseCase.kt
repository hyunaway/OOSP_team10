package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.model.RecommendedInterventionAction
import com.example.habittracker.domain.model.RecommendedInterventionActionType
import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.usecase.meal.MealDailyStatusCalculator
import com.example.habittracker.domain.usecase.meal.MealDailyStatusLevel
import com.example.habittracker.domain.usecase.stretch.GetTodayStretchStatusUseCase
import com.example.habittracker.domain.usecase.water.CheckWaterInterventionNeededUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResolveDigitalInterventionActionUseCase @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
    private val checkWaterInterventionNeededUseCase: CheckWaterInterventionNeededUseCase,
    private val getTodayStretchStatusUseCase: GetTodayStretchStatusUseCase,
    private val mealRepository: MealRepository,
    private val mealDailyStatusCalculator: MealDailyStatusCalculator,
) {

    suspend operator fun invoke(): RecommendedInterventionAction {
        val priorityOrder = runCatching {
            userPreferenceManager.categoryPriorityOrderFlow.first()
                .split(",")
                .map { it.trim().uppercase() }
                .filter { it in SUPPORTED_CATEGORIES }
                .filter { it != CATEGORY_DIGITAL }
        }.getOrDefault(DEFAULT_PRIORITY_ORDER)
            .ifEmpty { DEFAULT_PRIORITY_ORDER }

        priorityOrder.forEach { category ->
            when (category) {
                CATEGORY_WATER -> resolveWaterAction()?.let { return it }
                CATEGORY_STRETCH -> resolveStretchAction()?.let { return it }
                CATEGORY_MEAL -> resolveMealAction()?.let { return it }
            }
        }

        return digitalBreakAction()
    }

    private suspend fun resolveWaterAction(): RecommendedInterventionAction? =
        runCatching {
            val status = checkWaterInterventionNeededUseCase()
            if (!status.isNeedWater) return@runCatching null
            val message = when (status.shortageLevel) {
                WaterShortageLevel.SEVERE ->
                    "화면도 오래 봤고 수분 리듬도 많이 밀렸어요. 물부터 보충해봐요."
                WaterShortageLevel.MEDIUM ->
                    "화면을 오래 봤어요. 잠깐 쉬면서 물 한 잔 마셔볼까요?"
                WaterShortageLevel.LIGHT ->
                    "잠깐 쉬면서 물 한 잔 마시면 리듬이 딱 맞을 것 같아요."
                WaterShortageLevel.NONE ->
                    "화면을 오래 봤어요. 잠깐 쉬면서 물 한 잔 마셔볼까요?"
            }
            RecommendedInterventionAction(RecommendedInterventionActionType.WATER, message)
        }.getOrNull()

    private suspend fun resolveStretchAction(): RecommendedInterventionAction? =
        runCatching {
            val status = getTodayStretchStatusUseCase().first()
            if (status.totalCount >= STRETCH_GOAL_COUNT) return@runCatching null
            RecommendedInterventionAction(
                type = RecommendedInterventionActionType.STRETCH,
                message = "사용 시간이 길어졌어요. 5분만 몸을 풀어볼까요?",
            )
        }.getOrNull()

    private suspend fun resolveMealAction(): RecommendedInterventionAction? =
        runCatching {
            val today = LocalDate.now().toString()
            val logs = mealRepository.getLogsByMealDate(today)
            val wakeTimeMinutes = parseTimeToMinutes(
                userPreferenceManager.wakeTimeFlow.first(),
                DEFAULT_WAKE_TIME_MINUTES,
            )
            val bedTimeMinutes = parseTimeToMinutes(
                value = userPreferenceManager.bedTimeFlow.first(),
                fallback = DEFAULT_BED_TIME_MINUTES,
                midnightAsEndOfDay = true,
            )
            val status = mealDailyStatusCalculator.calculate(
                logs = logs,
                nowMillis = System.currentTimeMillis(),
                wakeTimeMinutes = wakeTimeMinutes,
                bedTimeMinutes = bedTimeMinutes,
            )
            if (status.statusLevel !in mealInterventionLevels) return@runCatching null
            val message = if (MealType.DINNER in status.missedMealTypes) {
                "화면을 잠깐 내려두고 식사 리듬도 챙겨볼까요?"
            } else {
                "식사 리듬이 조금 비어 있어요. 가볍게 챙겨볼까요?"
            }
            RecommendedInterventionAction(RecommendedInterventionActionType.MEAL, message)
        }.getOrNull()

    private fun digitalBreakAction(): RecommendedInterventionAction =
        RecommendedInterventionAction(
            type = RecommendedInterventionActionType.DIGITAL_BREAK,
            message = "사용 시간이 길어졌어요. 잠깐 눈을 쉬어볼까요?",
        )

    private fun parseTimeToMinutes(
        value: String,
        fallback: Int,
        midnightAsEndOfDay: Boolean = false,
    ): Int {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..24 || minute !in 0..59) {
            return fallback
        }
        if (hour == 24 && minute != 0) return fallback
        if (midnightAsEndOfDay && hour == 0 && minute == 0) return MINUTES_PER_DAY
        return hour * 60 + minute
    }

    companion object {
        private const val CATEGORY_MEAL = "MEAL"
        private const val CATEGORY_WATER = "WATER"
        private const val CATEGORY_DIGITAL = "DIGITAL"
        private const val CATEGORY_STRETCH = "STRETCH"
        private const val STRETCH_GOAL_COUNT = 5
        private const val MINUTES_PER_DAY = 24 * 60
        private const val DEFAULT_WAKE_TIME_MINUTES = 8 * 60
        private const val DEFAULT_BED_TIME_MINUTES = 24 * 60
        private val SUPPORTED_CATEGORIES = setOf(
            CATEGORY_MEAL,
            CATEGORY_WATER,
            CATEGORY_DIGITAL,
            CATEGORY_STRETCH,
        )
        private val DEFAULT_PRIORITY_ORDER = listOf(CATEGORY_MEAL, CATEGORY_WATER, CATEGORY_STRETCH)
        private val mealInterventionLevels = setOf(
            MealDailyStatusLevel.NEED_ATTENTION,
            MealDailyStatusLevel.RISK,
        )
    }
}
