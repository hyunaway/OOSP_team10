package com.example.habittracker.domain.usecase.water

import com.example.habittracker.domain.model.WaterInterventionStatus
import com.example.habittracker.domain.model.WaterShortageLevel
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WaterStatusCalculator @Inject constructor() {

    fun calculate(
        wakeMinutes: Int,
        bedMinutes: Int,
        goalMl: Int,
        currentAmountMl: Int,
        lastDrankAt: Long?,
        nowMillis: Long,
        toleranceMl: Int = DEFAULT_TOLERANCE_ML,
        minimumIntervalMinutes: Int = DEFAULT_MIN_INTERVAL_MINUTES,
    ): WaterInterventionStatus {
        val currentMinutes = minutesOfDay(nowMillis)
        val activePosition = activePosition(wakeMinutes, bedMinutes, currentMinutes)
        val recommendedAmountMl = activePosition?.let {
            recommendedAmountMl(
                elapsedMinutes = it.elapsedMinutes,
                durationMinutes = it.durationMinutes,
                goalMl = goalMl,
            )
        } ?: 0
        val shortageMl = (recommendedAmountMl - currentAmountMl).coerceAtLeast(0)
        val shortageLevel = shortageLevel(shortageMl)

        val hasReachedGoal = currentAmountMl >= goalMl
        val isEnoughAfterLastDrink = lastDrankAt?.let {
            nowMillis - it >= minimumIntervalMinutes * MILLIS_PER_MINUTE
        } ?: true

        val isNeedWater = activePosition != null &&
            activePosition.elapsedMinutes > 0 &&
            activePosition.elapsedMinutes < activePosition.durationMinutes &&
            !hasReachedGoal &&
            shortageMl >= toleranceMl &&
            isEnoughAfterLastDrink

        return WaterInterventionStatus(
            recommendedAmountMl = recommendedAmountMl,
            currentAmountMl = currentAmountMl,
            shortageMl = shortageMl,
            isNeedWater = isNeedWater,
            shortageLevel = shortageLevel,
            message = messageFor(isNeedWater, shortageLevel),
        )
    }

    private fun activePosition(
        wakeMinutes: Int,
        bedMinutes: Int,
        currentMinutes: Int,
    ): ActivePosition? {
        if (wakeMinutes == bedMinutes) {
            return ActivePosition(
                elapsedMinutes = currentMinutes,
                durationMinutes = MINUTES_PER_DAY,
            )
        }

        if (wakeMinutes < bedMinutes) {
            return when {
                currentMinutes < wakeMinutes || currentMinutes >= bedMinutes -> null
                else -> ActivePosition(
                    elapsedMinutes = currentMinutes - wakeMinutes,
                    durationMinutes = bedMinutes - wakeMinutes,
                )
            }
        }

        val activeDuration = MINUTES_PER_DAY - wakeMinutes + bedMinutes
        return when {
            currentMinutes >= wakeMinutes -> ActivePosition(
                elapsedMinutes = currentMinutes - wakeMinutes,
                durationMinutes = activeDuration,
            )
            currentMinutes < bedMinutes -> ActivePosition(
                elapsedMinutes = MINUTES_PER_DAY - wakeMinutes + currentMinutes,
                durationMinutes = activeDuration,
            )
            else -> null
        }
    }

    private fun recommendedAmountMl(
        elapsedMinutes: Int,
        durationMinutes: Int,
        goalMl: Int,
    ): Int {
        if (durationMinutes < MIN_WEIGHTED_DURATION_MINUTES) {
            return (goalMl * (elapsedMinutes / durationMinutes.toFloat()))
                .roundToInt()
                .coerceIn(0, goalMl)
        }

        val weightedAmount = when {
            elapsedMinutes <= MORNING_SEGMENT_MINUTES -> {
                val progress = elapsedMinutes / MORNING_SEGMENT_MINUTES.toFloat()
                goalMl * MORNING_WEIGHT * progress
            }
            elapsedMinutes <= durationMinutes - EVENING_SEGMENT_MINUTES -> {
                val middleDuration = durationMinutes - MORNING_SEGMENT_MINUTES - EVENING_SEGMENT_MINUTES
                val middleElapsed = elapsedMinutes - MORNING_SEGMENT_MINUTES
                val progress = middleElapsed / middleDuration.toFloat()
                goalMl * (MORNING_WEIGHT + MIDDLE_WEIGHT * progress)
            }
            else -> {
                val eveningElapsed = elapsedMinutes - (durationMinutes - EVENING_SEGMENT_MINUTES)
                val progress = eveningElapsed / EVENING_SEGMENT_MINUTES.toFloat()
                goalMl * (MORNING_WEIGHT + MIDDLE_WEIGHT + EVENING_WEIGHT * progress)
            }
        }

        return weightedAmount.roundToInt().coerceIn(0, goalMl)
    }

    private fun shortageLevel(shortageMl: Int): WaterShortageLevel =
        when {
            shortageMl >= 700 -> WaterShortageLevel.SEVERE
            shortageMl >= 400 -> WaterShortageLevel.MEDIUM
            shortageMl >= 200 -> WaterShortageLevel.LIGHT
            else -> WaterShortageLevel.NONE
        }

    private fun messageFor(
        isNeedWater: Boolean,
        shortageLevel: WaterShortageLevel,
    ): String {
        if (!isNeedWater) return "좋아요. 지금 물 섭취 리듬은 괜찮아요."
        return when (shortageLevel) {
            WaterShortageLevel.LIGHT -> "물 한 잔 마시면 리듬이 딱 맞을 것 같아요."
            WaterShortageLevel.MEDIUM -> "목이 조금 마른 상태예요. 물 한 잔 어때요?"
            WaterShortageLevel.SEVERE -> "오늘 수분 리듬이 많이 밀렸어요. 천천히 물을 보충해봐요."
            WaterShortageLevel.NONE -> "좋아요. 지금 물 섭취 리듬은 괜찮아요."
        }
    }

    private fun minutesOfDay(nowMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    companion object {
        const val DEFAULT_TOLERANCE_ML = 200
        const val DEFAULT_MIN_INTERVAL_MINUTES = 60
        private const val MORNING_SEGMENT_MINUTES = 2 * 60
        private const val EVENING_SEGMENT_MINUTES = 2 * 60
        private const val MIN_WEIGHTED_DURATION_MINUTES = 6 * 60
        private const val MORNING_WEIGHT = 0.25f
        private const val MIDDLE_WEIGHT = 0.60f
        private const val EVENING_WEIGHT = 0.15f
        private const val MINUTES_PER_DAY = 24 * 60
        private const val MILLIS_PER_MINUTE = 60_000L
    }

    private data class ActivePosition(
        val elapsedMinutes: Int,
        val durationMinutes: Int,
    )
}
