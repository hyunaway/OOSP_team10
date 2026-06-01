package com.example.habittracker.domain.usecase.stretch

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.model.StretchInterventionStatus
import com.example.habittracker.domain.repository.StretchRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckStretchInterventionNeededUseCase @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
    private val stretchRepository: StretchRepository,
    private val calculatePersonalizedStretchGoalUseCase: CalculatePersonalizedStretchGoalUseCase,
) {

    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
    ): StretchInterventionStatus {
        val activeStartedAt = userPreferenceManager.todayActiveStartedAtFlow.first()
            ?: return noNeed(goal = 0, todayCount = 0)
        val bedTime = userPreferenceManager.bedTimeFlow.first()
        if (isAfterBedTime(activeStartedAt, nowMillis, bedTime)) {
            return noNeed(goal = 0, todayCount = 0)
        }
        val personalizedGoal = calculatePersonalizedStretchGoalUseCase(
            activeStartedAtMillis = activeStartedAt,
            bedTime = bedTime,
        ) ?: return noNeed(goal = 0, todayCount = 0)

        val todayStatus = stretchRepository.getTodayStatus().first()
        val todayCount = todayStatus.totalCount
        if (todayCount >= personalizedGoal) {
            return StretchInterventionStatus(
                isNeedStretch = false,
                message = "오늘 스트레칭 리듬은 충분해요.",
                personalizedGoalCount = personalizedGoal,
                todayCount = todayCount,
                minutesUntilNextRecommended = null,
            )
        }

        val lastReminderAt = userPreferenceManager.lastStretchReminderAtFlow.first()
        val reminderRemainingMinutes = remainingMinutesAfterInterval(
            sinceMillis = lastReminderAt,
            nowMillis = nowMillis,
            intervalMinutes = REMINDER_COOLDOWN_MINUTES,
        )
        if (reminderRemainingMinutes != null && reminderRemainingMinutes > 0) {
            return noNeed(
                goal = personalizedGoal,
                todayCount = todayCount,
                minutesUntilNextRecommended = reminderRemainingMinutes,
            )
        }

        if (todayCount == 0) {
            val firstReminderRemainingMinutes = remainingMinutesAfterInterval(
                sinceMillis = activeStartedAt,
                nowMillis = nowMillis,
                intervalMinutes = FIRST_REMINDER_DELAY_MINUTES,
            )
            if (firstReminderRemainingMinutes != null && firstReminderRemainingMinutes > 0) {
                return noNeed(
                    goal = personalizedGoal,
                    todayCount = todayCount,
                    minutesUntilNextRecommended = firstReminderRemainingMinutes,
                )
            }
            return StretchInterventionStatus(
                isNeedStretch = true,
                message = "하루를 시작한 지 조금 지났어요. 5분만 가볍게 몸을 풀어볼까요?",
                personalizedGoalCount = personalizedGoal,
                todayCount = todayCount,
                minutesUntilNextRecommended = null,
            )
        }

        val stretchRemainingMinutes = remainingMinutesAfterInterval(
            sinceMillis = todayStatus.lastStretchAt,
            nowMillis = nowMillis,
            intervalMinutes = STRETCH_INTERVAL_MINUTES,
        )
        if (stretchRemainingMinutes != null && stretchRemainingMinutes > 0) {
            return noNeed(
                goal = personalizedGoal,
                todayCount = todayCount,
                minutesUntilNextRecommended = stretchRemainingMinutes,
            )
        }

        return StretchInterventionStatus(
            isNeedStretch = true,
            message = "마지막 스트레칭 이후 시간이 꽤 지났어요. 5분만 몸을 풀어볼까요?",
            personalizedGoalCount = personalizedGoal,
            todayCount = todayCount,
            minutesUntilNextRecommended = null,
        )
    }

    private fun isAfterBedTime(
        activeStartedAtMillis: Long,
        nowMillis: Long,
        bedTime: String,
    ): Boolean {
        val activeStartMinutes = minutesOfDay(activeStartedAtMillis)
        val nowMinutes = minutesOfDay(nowMillis)
        val bedMinutes = parseBedTimeMinutes(bedTime) ?: MINUTES_PER_DAY
        val activeDuration = minutesUntilBed(activeStartMinutes, bedMinutes)
        val elapsedSinceActiveStart = minutesBetween(activeStartMinutes, nowMinutes)
        return elapsedSinceActiveStart >= activeDuration
    }

    private fun minutesUntilBed(startMinutes: Int, bedMinutes: Int): Int {
        val normalizedStart = normalizeMinutes(startMinutes)
        val normalizedBed = if (bedMinutes == MINUTES_PER_DAY) {
            MINUTES_PER_DAY
        } else {
            normalizeMinutes(bedMinutes)
        }
        if (normalizedBed == normalizedStart) return 0
        return if (normalizedBed > normalizedStart) {
            normalizedBed - normalizedStart
        } else {
            normalizedBed + MINUTES_PER_DAY - normalizedStart
        }
    }

    private fun minutesBetween(startMinutes: Int, endMinutes: Int): Int {
        val normalizedStart = normalizeMinutes(startMinutes)
        val normalizedEnd = normalizeMinutes(endMinutes)
        return if (normalizedEnd >= normalizedStart) {
            normalizedEnd - normalizedStart
        } else {
            normalizedEnd + MINUTES_PER_DAY - normalizedStart
        }
    }

    private fun minutesOfDay(timestampMillis: Long): Int {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestampMillis }
        return calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
    }

    private fun parseBedTimeMinutes(value: String): Int? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (hour !in 0..24 || minute !in 0..59) return null
        if (hour == 24 && minute != 0) return null
        return if (hour == 0 && minute == 0) MINUTES_PER_DAY else hour * 60 + minute
    }

    private fun normalizeMinutes(minutes: Int): Int =
        if (minutes == MINUTES_PER_DAY) 0 else minutes.mod(MINUTES_PER_DAY)

    private fun noNeed(
        goal: Int,
        todayCount: Int,
        minutesUntilNextRecommended: Int? = null,
    ): StretchInterventionStatus =
        StretchInterventionStatus(
            isNeedStretch = false,
            message = "지금은 스트레칭 알림이 필요하지 않아요.",
            personalizedGoalCount = goal,
            todayCount = todayCount,
            minutesUntilNextRecommended = minutesUntilNextRecommended,
        )

    private fun remainingMinutesAfterInterval(
        sinceMillis: Long?,
        nowMillis: Long,
        intervalMinutes: Int,
    ): Int? {
        if (sinceMillis == null) return null
        val elapsedMillis = nowMillis - sinceMillis
        val remainingMillis = intervalMinutes * MILLIS_PER_MINUTE - elapsedMillis
        return if (remainingMillis <= 0) {
            0
        } else {
            ((remainingMillis + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt()
        }
    }

    companion object {
        private const val FIRST_REMINDER_DELAY_MINUTES = 15
        private const val STRETCH_INTERVAL_MINUTES = 90
        private const val REMINDER_COOLDOWN_MINUTES = 90
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val MINUTES_PER_DAY = 24 * 60
    }
}
