package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class MealDailyStatus(
    val expectedMealTypesUntilNow: Set<MealType>,
    val completedMealTypes: Set<MealType>,
    val missedMealTypes: Set<MealType>,
    val currentMealWindow: MealWindow?,
    val nextMealType: MealType?,
    val statusLevel: MealDailyStatusLevel,
    val message: String,
    val dailyEvaluation: MealDailyEvaluation? = null,
    val mealPlanMessage: String = "",
)

enum class MealDailyStatusLevel {
    GOOD,
    NORMAL,
    NEED_ATTENTION,
    RISK,
    LATE_NIGHT,
    REVIEW_ONLY,
}

class MealDailyStatusCalculator @Inject constructor(
    private val dailyMealPlanCalculator: DailyMealPlanCalculator,
    private val analyzeMealLogsUseCase: AnalyzeMealLogsUseCase,
) {

    fun calculate(
        logs: List<MealLogEntity>,
        nowMillis: Long,
        wakeTimeMinutes: Int,
        bedTimeMinutes: Int,
        todayActiveStartedAtMillis: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MealDailyStatus {
        val localDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val currentMinutes = localDateTime.hour * 60 + localDateTime.minute
        val plan = dailyMealPlanCalculator.calculate(
            date = LocalDate.now(zoneId).toString(),
            wakeTimeMinutes = wakeTimeMinutes,
            bedTimeMinutes = bedTimeMinutes,
            todayActiveStartedAtMillis = todayActiveStartedAtMillis,
            zoneId = zoneId,
        )
        val expectedTypes = plan.expectedMeals.map { it.type }.toSet()
        val mealLogAnalysis = analyzeMealLogsUseCase(
            logs = logs,
            plan = plan,
            zoneId = zoneId,
        )
        val completedExpected = mealLogAnalysis.primaryMealTypes
            .filter { it in expectedTypes }
            .toSet()
        val currentExpectedWindow = plan.expectedMeals.firstOrNull { it.contains(currentMinutes) }
        val missedForReview = plan.expectedMeals
            .filter { it.endMinutes <= currentMinutes && it.type !in completedExpected }
            .map { it.type }
            .toSet()
        val expectedUntilNow = plan.expectedMeals
            .filter { it.endMinutes <= currentMinutes || it == currentExpectedWindow }
            .map { it.type }
            .toSet()
        val statusLevel = resolveStatusLevel(
            hour = localDateTime.hour,
            bedTimeMinutes = bedTimeMinutes,
            currentMinutes = currentMinutes,
            currentWindow = currentExpectedWindow,
            completed = completedExpected,
            missedForReview = missedForReview,
            expectedMealCount = plan.expectedMeals.size,
        )
        val evaluation = MealDailyEvaluation(
            plan = plan,
            completedMealTypes = completedExpected,
            missedForReview = missedForReview,
            skippedByLateWake = plan.skippedByLateWake,
            lateNightLogged = logs.any { it.type == MealType.LATE_NIGHT || it.isLateNight },
            statusLevel = statusLevel,
            additionalIntakeCount = mealLogAnalysis.additionalIntakeCount,
            hasIrregularIntake = mealLogAnalysis.hasIrregularIntake,
            earlyNextMealCandidates = mealLogAnalysis.earlyNextMealCandidates,
        )
        return MealDailyStatus(
            expectedMealTypesUntilNow = expectedUntilNow,
            completedMealTypes = completedExpected,
            missedMealTypes = missedForReview,
            currentMealWindow = currentExpectedWindow?.toMealWindow(),
            nextMealType = plan.expectedMeals.firstOrNull { it.startMinutes > currentMinutes }?.type,
            statusLevel = statusLevel,
            message = messageFor(statusLevel, currentExpectedWindow, missedForReview, completedExpected, plan),
            dailyEvaluation = evaluation,
            mealPlanMessage = planMessage(plan),
        )
    }

    private fun resolveStatusLevel(
        hour: Int,
        bedTimeMinutes: Int,
        currentMinutes: Int,
        currentWindow: ExpectedMealWindow?,
        completed: Set<MealType>,
        missedForReview: Set<MealType>,
        expectedMealCount: Int,
    ): MealDailyStatusLevel {
        if (hour in 0..4) return MealDailyStatusLevel.LATE_NIGHT
        if (isSleepPrepOrAfterBed(bedTimeMinutes, currentMinutes)) return MealDailyStatusLevel.REVIEW_ONLY
        if (currentWindow != null && currentWindow.type !in completed) {
            return MealDailyStatusLevel.NEED_ATTENTION
        }
        if (missedForReview.isNotEmpty()) return MealDailyStatusLevel.RISK
        return when {
            expectedMealCount == 0 -> MealDailyStatusLevel.REVIEW_ONLY
            completed.isNotEmpty() -> MealDailyStatusLevel.GOOD
            else -> MealDailyStatusLevel.NORMAL
        }
    }

    private fun isSleepPrepOrAfterBed(bedTimeMinutes: Int, currentMinutes: Int): Boolean {
        val bed = when {
            bedTimeMinutes == 0 -> MINUTES_PER_DAY
            bedTimeMinutes < 0 -> MINUTES_PER_DAY
            else -> bedTimeMinutes.coerceAtMost(MINUTES_PER_DAY)
        }
        val sleepPrepStart = (bed - SLEEP_PREP_WINDOW_MINUTES).coerceAtLeast(0)
        return currentMinutes in sleepPrepStart until bed || currentMinutes >= bed && bed < MINUTES_PER_DAY
    }

    private fun messageFor(
        statusLevel: MealDailyStatusLevel,
        currentWindow: ExpectedMealWindow?,
        missedForReview: Set<MealType>,
        completed: Set<MealType>,
        plan: DailyMealPlan,
    ): String =
        when (statusLevel) {
            MealDailyStatusLevel.LATE_NIGHT ->
                "늦은 시간대에는 야식은 참고 기록으로만 남기고, 내일 리듬을 다시 맞춰봐요."
            MealDailyStatusLevel.REVIEW_ONLY ->
                if (missedForReview.isEmpty()) {
                    "오늘 식사 리듬은 여기까지 괜찮았어요. 이제 쉬어갈 시간이에요."
                } else {
                    "지나간 식사는 회고용으로만 남겨둘게요. 지금은 무리하지 않아도 괜찮아요."
                }
            MealDailyStatusLevel.NEED_ATTENTION ->
                when (currentWindow?.type) {
                    MealType.BREAKFAST -> "아침을 챙기기 좋은 시간이에요."
                    MealType.LUNCH -> "지금 챙길 수 있는 점심 시간이예요."
                    MealType.DINNER -> "저녁을 너무 늦지 않게 챙겨볼까요?"
                    else -> "지금 챙길 수 있는 식사 시간이예요."
                }
            MealDailyStatusLevel.RISK ->
                "지나간 식사 공백은 회고에 남겨둘게요. 현재 가능한 식사만 차분히 챙겨봐요."
            MealDailyStatusLevel.GOOD ->
                if (completed.size >= plan.expectedMeals.size) {
                    "오늘 계획한 식사를 잘 챙겼어요."
                } else {
                    "현재까지 식사 리듬이 괜찮아요."
                }
            MealDailyStatusLevel.NORMAL ->
                "아직 식사 판단을 서두르지 않아도 괜찮아요."
        }

    private fun planMessage(plan: DailyMealPlan): String =
        when (plan.planType) {
            DailyMealPlanType.NORMAL_DAY ->
                "오늘은 아침·점심·저녁 3끼를 기준으로 관리해요."
            DailyMealPlanType.LATE_WAKE_DAY ->
                "오늘은 늦게 시작했어요. 아침은 제외하고 점심·저녁을 기준으로 관리해요."
            DailyMealPlanType.VERY_LATE_WAKE_DAY ->
                "오늘은 남은 시간에 맞춰 식사 목표를 조정했어요."
            DailyMealPlanType.EVENING_START_DAY ->
                if (plan.expectedMeals.isEmpty()) {
                    "오늘은 취침까지 시간이 짧아 식사 개입을 무리하게 보내지 않아요."
                } else {
                    "오늘은 저녁 식사 중심으로 가볍게 관리해요."
                }
        }

    private fun ExpectedMealWindow.toMealWindow(): MealWindow =
        MealWindow(
            mealType = type,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            centerMinutes = startMinutes + (endMinutes - startMinutes) / 2,
        )

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
        private const val SLEEP_PREP_WINDOW_MINUTES = 60
    }
}
