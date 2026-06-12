package com.example.habittracker.domain.usecase.water

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.analysis.PersonalizationResolver
import com.example.habittracker.domain.model.WaterInterventionStatus
import com.example.habittracker.domain.model.WaterShortageLevel
import com.example.habittracker.domain.repository.WaterRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckWaterInterventionNeededUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
    private val userPreferenceManager: UserPreferenceManager,
    private val waterStatusCalculator: WaterStatusCalculator,
    private val personalizationResolver: PersonalizationResolver,
) {

    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
    ): WaterInterventionStatus {
        val status = waterRepository.getTodayStatus().first()
        val wakeMinutes = userPreferenceManager.getWakeTimeAsMinutes().first()
        val bedMinutes = userPreferenceManager.getBedTimeAsMinutes().first()
        val resolvedGoalMl = personalizationResolver.resolveWaterGoalMl()
        val activeStartedAt = userPreferenceManager.todayActiveStartedAtFlow.first()
            ?: return WaterInterventionStatus(
                baseGoalMl = resolvedGoalMl,
                effectiveGoalMl = resolvedGoalMl,
                recommendedAmountMl = 0,
                currentAmountMl = status.totalMl,
                shortageMl = 0,
                isNeedWater = false,
                shortageLevel = WaterShortageLevel.NONE,
                message = "좋아요. 지금 물 섭취 리듬은 괜찮아요.",
            )
        val activeStartMinutes = minutesOfDay(activeStartedAt)
        val interventionStartMinutes = maxOf(wakeMinutes, activeStartMinutes)

        // 오늘 요일 기준 주말 여부 판별
        // 개인화된 물 피크 윈도우 조회
        val waterPeak = personalizationResolver.resolveWaterPeakWindow()

        return waterStatusCalculator.calculate(
            wakeMinutes = interventionStartMinutes,
            bedMinutes = bedMinutes,
            goalMl = resolvedGoalMl,
            currentAmountMl = status.totalMl,
            lastDrankAt = status.lastDrankAt,
            nowMillis = nowMillis,
            waterPeakWindow = waterPeak,
        )
    }

    private fun minutesOfDay(timestampMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }
}
