// 경로: com/example/habittracker/domain/usecase/meal/AddMealLogUseCase.kt
package com.example.habittracker.domain.usecase.meal

import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.util.NotificationHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddMealLogUseCase @Inject constructor(
    private val mealRepository: MealRepository,
    private val notificationHelper: NotificationHelper,
) {
    suspend operator fun invoke(
        type: MealType,
        isLateNight: Boolean,
        viaDeliveryApp: Boolean,
        source: String,
        timestamp: Long = System.currentTimeMillis(),
        mealDate: String = java.time.LocalDate.now().toString(),
        recordedTime: String = java.time.LocalTime.now().toString(),
        inputMethod: String = "",
        triggerType: String = "",
    ) {
        mealRepository.addLog(
            type = type,
            timestamp = timestamp,
            isLateNight = isLateNight,
            viaDeliveryApp = viaDeliveryApp,
            source = source,
            mealDate = mealDate,
            recordedTime = recordedTime,
            inputMethod = inputMethod,
            triggerType = triggerType
        )
        
        // 삼시세끼 달성 검사 및 즉시 칭찬 알림 발송
        try {
            val status = mealRepository.getTodayStatus().first()
            if (status.breakfastLogged && status.lunchLogged && status.dinnerLogged) {
                notificationHelper.sendMealReminder("오늘 세 끼 모두 챙겨 드셨네요! 훌륭해요 🎉", "CONGRATS")
            }
        } catch (_: Exception) {
            // 실패 시 무시
        }
    }
}
