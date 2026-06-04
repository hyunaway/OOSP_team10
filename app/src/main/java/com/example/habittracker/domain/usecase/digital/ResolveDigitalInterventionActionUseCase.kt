package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.model.RecommendedInterventionAction
import com.example.habittracker.domain.model.RecommendedInterventionActionType
import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.domain.usecase.meal.GetCurrentMealInterventionStatusUseCase
import com.example.habittracker.domain.usecase.stretch.GetTodayStretchStatusUseCase
import com.example.habittracker.domain.usecase.water.CheckWaterInterventionNeededUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResolveDigitalInterventionActionUseCase @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
    private val checkWaterInterventionNeededUseCase: CheckWaterInterventionNeededUseCase,
    private val getTodayStretchStatusUseCase: GetTodayStretchStatusUseCase,
    private val getCurrentMealInterventionStatusUseCase: GetCurrentMealInterventionStatusUseCase,
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
            val status = getCurrentMealInterventionStatusUseCase()
            if (!status.isActionable) return@runCatching null
            RecommendedInterventionAction(
                type = RecommendedInterventionActionType.MEAL,
                message = "지금 챙길 수 있는 식사 시간이 비어 있어요. 가볍게 챙겨볼까요?",
            )
        }.getOrNull()

    private fun digitalBreakAction(): RecommendedInterventionAction =
        RecommendedInterventionAction(
            type = RecommendedInterventionActionType.DIGITAL_BREAK,
            message = "사용 시간이 길어졌어요. 잠깐 눈을 쉬어볼까요?",
        )

    companion object {
        private const val CATEGORY_MEAL = "MEAL"
        private const val CATEGORY_WATER = "WATER"
        private const val CATEGORY_DIGITAL = "DIGITAL"
        private const val CATEGORY_STRETCH = "STRETCH"
        private const val STRETCH_GOAL_COUNT = 5
        private val SUPPORTED_CATEGORIES = setOf(
            CATEGORY_MEAL,
            CATEGORY_WATER,
            CATEGORY_DIGITAL,
            CATEGORY_STRETCH,
        )
        private val DEFAULT_PRIORITY_ORDER = listOf(CATEGORY_MEAL, CATEGORY_WATER, CATEGORY_STRETCH)
    }
}
