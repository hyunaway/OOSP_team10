package com.example.habittracker.domain.usecase.stretch

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.analysis.PersonalizationResolver
import com.example.habittracker.domain.model.StretchInterventionStatus
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.util.TimeCalculationUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckStretchInterventionNeededUseCase @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
    private val stretchRepository: StretchRepository,
    private val calculatePersonalizedStretchGoalUseCase: CalculatePersonalizedStretchGoalUseCase,
    private val personalizationResolver: PersonalizationResolver,
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
            bedTime               = bedTime,
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

        // 1. 마지막 알림 쿨다운 검증 (90분)
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

        // 2. 활동 시작 후 첫 딜레이 검증 (15분)
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
        }

        // --- 개인화 선호 슬롯(Peak Slot) 체크 및 우회 로직 추가 ---
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(nowMillis))
        val currentSlot = resolveSlotFromMillis(nowMillis)
        val preferredSlot = personalizationResolver.resolveStretchPreferredSlot()

        val isPreferredSlotActive = preferredSlot != null && preferredSlot == currentSlot
        val hasStretchedInPreferredSlot = if (isPreferredSlotActive) {
            stretchRepository.getRecordByTimeSlot(todayDate, currentSlot) != null
        } else {
            false
        }

        if (isPreferredSlotActive && !hasStretchedInPreferredSlot) {
            return StretchInterventionStatus(
                isNeedStretch = true,
                message = "지금은 자주 스트레칭하시는 시간대예요! 가볍게 몸을 움직여볼까요?",
                personalizedGoalCount = personalizedGoal,
                todayCount = todayCount,
                minutesUntilNextRecommended = null
            )
        }
        // ------------------------------------------------------

        if (todayCount == 0) {
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
        val activeStartMinutes = TimeCalculationUtils.minutesOfDay(activeStartedAtMillis)
        val nowMinutes = TimeCalculationUtils.minutesOfDay(nowMillis)
        val bedMinutes = TimeCalculationUtils.parseBedTimeMinutes(bedTime) ?: MINUTES_PER_DAY
        val activeDuration = TimeCalculationUtils.minutesUntilBed(activeStartMinutes, bedMinutes)
        val elapsedSinceActiveStart = TimeCalculationUtils.minutesBetween(activeStartMinutes, nowMinutes)
        return elapsedSinceActiveStart >= activeDuration
    }

    private fun resolveSlotFromMillis(timestampMillis: Long): String {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "아침"
            in 12..16 -> "점심"
            in 17..21 -> "저녁"
            else -> "기타"
        }
    }

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
