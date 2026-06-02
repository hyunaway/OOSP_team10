package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.model.MealType
import java.time.Instant
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
    private val mealWindowCalculator: MealWindowCalculator,
) {

    fun calculate(
        logs: List<MealLogEntity>,
        nowMillis: Long,
        wakeTimeMinutes: Int,
        bedTimeMinutes: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MealDailyStatus {
        val localDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val currentMinutes = localDateTime.hour * 60 + localDateTime.minute
        val windows = mealWindowCalculator.calculate(wakeTimeMinutes, bedTimeMinutes)
        val currentWindow = mealWindowCalculator.findCurrentWindow(windows, currentMinutes)
        val completed = logs
            .filter { it.type in ordinaryMealTypes && !it.isLateNight }
            .map { it.type }
            .toSet()

        if (localDateTime.hour in 0..4) {
            return buildStatus(
                windows = windows,
                currentMinutes = currentMinutes,
                completed = completed,
                currentWindow = currentWindow,
                statusLevel = MealDailyStatusLevel.LATE_NIGHT,
                message = "늦은 시간이에요. 야식은 가볍게 기록하고 내일 리듬을 다시 맞춰봐요.",
            )
        }

        val bed = if (bedTimeMinutes == 0) MealWindowCalculator.MINUTES_PER_DAY else bedTimeMinutes
        val sleepPrepStart = normalize(bed - SLEEP_PREP_WINDOW_MINUTES)
        val inSleepPrep = minutesSince(sleepPrepStart, currentMinutes) in 0..SLEEP_PREP_WINDOW_MINUTES
        if (inSleepPrep || currentMinutes >= bed && bed < MealWindowCalculator.MINUTES_PER_DAY) {
            val missed = endedWindowsUntil(windows, currentMinutes)
                .map { it.mealType }
                .filter { it !in completed }
                .toSet()
            val message = if (missed.isEmpty()) {
                "오늘 식사 리듬을 여기까지 잘 이어왔어요. 이제는 쉬어갈 시간이에요."
            } else {
                "오늘 식사 리듬이 조금 늦어졌어요. 지금은 무리하기보다 내일 리듬을 맞춰봐요."
            }
            return buildStatus(
                windows = windows,
                currentMinutes = currentMinutes,
                completed = completed,
                currentWindow = currentWindow,
                statusLevel = MealDailyStatusLevel.REVIEW_ONLY,
                message = message,
            )
        }

        if (currentWindow != null && currentWindow.mealType !in completed) {
            return buildStatus(
                windows = windows,
                currentMinutes = currentMinutes,
                completed = completed,
                currentWindow = currentWindow,
                statusLevel = MealDailyStatusLevel.NEED_ATTENTION,
                message = attentionMessage(currentWindow.mealType),
            )
        }

        val missed = endedWindowsUntil(windows, currentMinutes)
            .map { it.mealType }
            .filter { it !in completed }
            .toSet()
        if (missed.isNotEmpty()) {
            return buildStatus(
                windows = windows,
                currentMinutes = currentMinutes,
                completed = completed,
                currentWindow = currentWindow,
                statusLevel = MealDailyStatusLevel.RISK,
                message = riskMessage(missed, completed),
            )
        }

        val statusLevel = if (completed.isNotEmpty()) {
            MealDailyStatusLevel.GOOD
        } else {
            MealDailyStatusLevel.NORMAL
        }
        return buildStatus(
            windows = windows,
            currentMinutes = currentMinutes,
            completed = completed,
            currentWindow = currentWindow,
            statusLevel = statusLevel,
            message = normalMessage(completed),
        )
    }

    private fun buildStatus(
        windows: List<MealWindow>,
        currentMinutes: Int,
        completed: Set<MealType>,
        currentWindow: MealWindow?,
        statusLevel: MealDailyStatusLevel,
        message: String,
    ): MealDailyStatus {
        val expected = endedWindowsUntil(windows, currentMinutes)
            .map { it.mealType }
            .toMutableSet()
        if (currentWindow != null) expected.add(currentWindow.mealType)
        val missed = expected.filter { it !in completed }.toSet()
        return MealDailyStatus(
            expectedMealTypesUntilNow = expected,
            completedMealTypes = completed,
            missedMealTypes = missed,
            currentMealWindow = currentWindow,
            nextMealType = windows.firstOrNull { minutesSince(currentMinutes, it.startMinutes) in 1 until MealWindowCalculator.MINUTES_PER_DAY }
                ?.mealType,
            statusLevel = statusLevel,
            message = message,
        )
    }

    private fun endedWindowsUntil(
        windows: List<MealWindow>,
        currentMinutes: Int,
    ): List<MealWindow> =
        windows.filter { window ->
            !window.contains(currentMinutes) &&
                minutesSince(window.endMinutes, currentMinutes) in 0 until MealWindowCalculator.MINUTES_PER_DAY / 2
        }

    private fun attentionMessage(type: MealType): String =
        when (type) {
            MealType.BREAKFAST -> "아침 챙기기 좋은 시간이에요."
            MealType.LUNCH -> "아직 늦지 않았어요. 점심을 챙겨볼까요?"
            MealType.DINNER -> "저녁을 너무 늦지 않게 챙겨볼까요?"
            MealType.LATE_NIGHT -> "야식은 권장보다 기록과 자각이 중요해요."
        }

    private fun riskMessage(missed: Set<MealType>, completed: Set<MealType>): String =
        when {
            MealType.BREAKFAST in missed && MealType.LUNCH in completed -> {
                "오늘 첫 끼가 점심이었어요. 저녁은 너무 늦지 않게 챙겨봐요."
            }
            MealType.DINNER in missed -> {
                "오늘 저녁 기록이 아직 없어요. 너무 늦기 전에 식사 리듬을 확인해봐요."
            }
            MealType.LUNCH in missed -> {
                "점심 기록이 아직 없어요. 무리하지 말고 지금 리듬을 한번 확인해봐요."
            }
            else -> "놓친 식사가 있을 수 있어요. 오늘 리듬을 한번 확인해봐요."
        }

    private fun normalMessage(completed: Set<MealType>): String =
        when {
            MealType.LUNCH in completed && MealType.DINNER !in completed -> {
                "점심을 챙겼어요. 남은 끼니도 자연스럽게 이어가봐요."
            }
            completed.isNotEmpty() -> "현재까지 식사 리듬이 괜찮아요."
            else -> "아직 식사 판단을 서두르지 않아도 괜찮아요."
        }

    private fun minutesSince(startMinutes: Int, currentMinutes: Int): Int {
        val start = normalize(startMinutes)
        val current = normalize(currentMinutes)
        return if (current >= start) current - start else current + MealWindowCalculator.MINUTES_PER_DAY - start
    }

    companion object {
        private val ordinaryMealTypes = setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
        private const val SLEEP_PREP_WINDOW_MINUTES = 60
    }
}
