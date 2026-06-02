package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs

enum class MealInputMeaning {
    NORMAL_MEAL,
    FIRST_MEAL_AFTER_SKIPPED_BREAKFAST,
    FIRST_MEAL_AFTER_SKIPPED_BREAKFAST_AND_LUNCH,
    ADDITIONAL_INTAKE_SAME_SESSION,
    LATE_NIGHT_SNACK,
    OUT_OF_RECOMMENDED_TIME,
}

enum class MealTimingQuality {
    EARLY,
    ON_TIME,
    LATE,
    VERY_LATE,
}

data class MealClassificationResult(
    val mealDate: String,
    val mealType: MealType,
    val isLateNight: Boolean,
    val inputMeaning: MealInputMeaning,
    val timingQuality: MealTimingQuality,
    val minutesFromRecommendedCenter: Int?,
    val message: String,
)

class MealClassifier @Inject constructor(
    private val mealWindowCalculator: MealWindowCalculator,
) {

    fun resolveMealDate(
        timestamp: Long,
        requestedMealType: MealType?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val localDateTime = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDateTime()
        return when {
            isEarlyMorningLateNight(localDateTime.hour) -> localDateTime.toLocalDate().minusDays(1).toString()
            requestedMealType == MealType.LATE_NIGHT -> localDateTime.toLocalDate().toString()
            else -> localDateTime.toLocalDate().toString()
        }
    }

    fun classify(
        requestedMealType: MealType?,
        timestamp: Long,
        existingLogs: List<MealLogEntity>,
        wakeTimeMinutes: Int,
        sleepTimeMinutes: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MealClassificationResult {
        val localDateTime = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDateTime()
        val mealDate = resolveMealDate(timestamp, requestedMealType, zoneId)
        val currentMinutes = localDateTime.hour * 60 + localDateTime.minute

        if (isEarlyMorningLateNight(localDateTime.hour)) {
            return MealClassificationResult(
                mealDate = mealDate,
                mealType = MealType.LATE_NIGHT,
                isLateNight = true,
                inputMeaning = MealInputMeaning.LATE_NIGHT_SNACK,
                timingQuality = MealTimingQuality.VERY_LATE,
                minutesFromRecommendedCenter = null,
                message = "전날 밤의 야식으로 기록했어요.",
            )
        }

        if (requestedMealType == MealType.LATE_NIGHT) {
            return MealClassificationResult(
                mealDate = mealDate,
                mealType = MealType.LATE_NIGHT,
                isLateNight = true,
                inputMeaning = MealInputMeaning.LATE_NIGHT_SNACK,
                timingQuality = MealTimingQuality.VERY_LATE,
                minutesFromRecommendedCenter = null,
                message = "야식으로 기록했어요.",
            )
        }

        val windows = mealWindowCalculator.calculate(wakeTimeMinutes, sleepTimeMinutes)
        val containingWindow = mealWindowCalculator.findCurrentWindow(windows, currentMinutes)
        val inferredWindow = containingWindow
            ?: requestedMealType?.let { type -> windows.firstOrNull { it.mealType == type } }
            ?: mealWindowCalculator.nearestWindow(windows, currentMinutes)

        val ordinaryLogs = existingLogs
            .filter { it.type in ordinaryMealTypes && !it.isLateNight }
            .sortedBy { it.timestamp }
        val lastOrdinaryLog = ordinaryLogs.lastOrNull()

        if (lastOrdinaryLog != null) {
            val minutesSinceLastMeal = (timestamp - lastOrdinaryLog.timestamp) / MILLIS_PER_MINUTE
            val nextWindow = mealWindowCalculator.nextWindowAfter(lastOrdinaryLog.type, windows)
            val reachedNextLayer = nextWindow != null &&
                inferredWindow.mealType == nextWindow.mealType &&
                nextWindow.contains(currentMinutes) &&
                nextWindow.minutesSinceStart(currentMinutes) >= NEXT_LAYER_GRACE_MINUTES

            if (
                minutesSinceLastMeal in 0..SAME_SESSION_MINUTES &&
                inferredWindow.mealType == lastOrdinaryLog.type &&
                !reachedNextLayer
            ) {
                return buildResult(
                    mealDate = mealDate,
                    mealType = lastOrdinaryLog.type,
                    isLateNight = false,
                    inputMeaning = MealInputMeaning.ADDITIONAL_INTAKE_SAME_SESSION,
                    timingQuality = timingQuality(currentMinutes, inferredWindow),
                    minutesFromRecommendedCenter = mealWindowCalculator.signedCircularDistance(
                        currentMinutes,
                        inferredWindow.centerMinutes,
                    ),
                    message = "이전 식사와 가까워 같은 식사 세션에 추가로 기록했어요.",
                )
            }
        }

        val noOrdinaryMealYet = ordinaryLogs.isEmpty()
        val inputMeaning = when {
            noOrdinaryMealYet && inferredWindow.mealType == MealType.LUNCH -> {
                MealInputMeaning.FIRST_MEAL_AFTER_SKIPPED_BREAKFAST
            }
            noOrdinaryMealYet && inferredWindow.mealType == MealType.DINNER -> {
                MealInputMeaning.FIRST_MEAL_AFTER_SKIPPED_BREAKFAST_AND_LUNCH
            }
            containingWindow == null -> MealInputMeaning.OUT_OF_RECOMMENDED_TIME
            else -> MealInputMeaning.NORMAL_MEAL
        }

        return buildResult(
            mealDate = mealDate,
            mealType = inferredWindow.mealType,
            isLateNight = false,
            inputMeaning = inputMeaning,
            timingQuality = timingQuality(currentMinutes, inferredWindow),
            minutesFromRecommendedCenter = mealWindowCalculator.signedCircularDistance(
                currentMinutes,
                inferredWindow.centerMinutes,
            ),
            message = messageFor(inputMeaning),
        )
    }

    private fun buildResult(
        mealDate: String,
        mealType: MealType,
        isLateNight: Boolean,
        inputMeaning: MealInputMeaning,
        timingQuality: MealTimingQuality,
        minutesFromRecommendedCenter: Int?,
        message: String,
    ): MealClassificationResult =
        MealClassificationResult(
            mealDate = mealDate,
            mealType = mealType,
            isLateNight = isLateNight,
            inputMeaning = inputMeaning,
            timingQuality = timingQuality,
            minutesFromRecommendedCenter = minutesFromRecommendedCenter,
            message = message,
        )

    private fun timingQuality(currentMinutes: Int, window: MealWindow): MealTimingQuality {
        if (window.contains(currentMinutes)) return MealTimingQuality.ON_TIME

        val fromCenter = mealWindowCalculator.signedCircularDistance(currentMinutes, window.centerMinutes)
        return when {
            fromCenter < 0 -> MealTimingQuality.EARLY
            abs(fromCenter) >= VERY_LATE_THRESHOLD_MINUTES -> MealTimingQuality.VERY_LATE
            else -> MealTimingQuality.LATE
        }
    }

    private fun messageFor(inputMeaning: MealInputMeaning): String =
        when (inputMeaning) {
            MealInputMeaning.FIRST_MEAL_AFTER_SKIPPED_BREAKFAST -> {
                "아침을 건너뛰고 점심으로 첫 끼를 기록했어요."
            }
            MealInputMeaning.FIRST_MEAL_AFTER_SKIPPED_BREAKFAST_AND_LUNCH -> {
                "아침과 점심을 건너뛰고 저녁으로 첫 끼를 기록했어요."
            }
            MealInputMeaning.ADDITIONAL_INTAKE_SAME_SESSION -> {
                "이전 식사와 가까워 같은 식사 세션에 추가로 기록했어요."
            }
            MealInputMeaning.LATE_NIGHT_SNACK -> "전날 밤의 야식으로 기록했어요."
            MealInputMeaning.OUT_OF_RECOMMENDED_TIME -> "권장 식사 시간 밖의 기록으로 저장했어요."
            MealInputMeaning.NORMAL_MEAL -> "식사 기록을 저장했어요."
        }

    private fun isEarlyMorningLateNight(hour: Int): Boolean = hour in 0..4

    companion object {
        private val ordinaryMealTypes = setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val SAME_SESSION_MINUTES = 120L
        private const val NEXT_LAYER_GRACE_MINUTES = 30
        private const val VERY_LATE_THRESHOLD_MINUTES = 240
    }
}
