// 경로: com/example/habittracker/worker/DeliveryAppDetectionWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.usage.UsageStatsHelper
import com.example.habittracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class DeliveryAppDetectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userPreferenceManager: UserPreferenceManager,
    private val usageStatsHelper: UsageStatsHelper,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!usageStatsHelper.hasUsageAccess()) return Result.success()

        return try {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            // 야식 위험 시간대: 21:00 ~ 01:00 (21:00 ~ 23:59 또는 00:00 ~ 00:59)
            val isLateNightRiskHour = hour >= 21 || hour < 1
            if (!isLateNightRiskHour) return Result.success()

            val deliveryApps = userPreferenceManager.registeredDeliveryPackagesFlow.first()
            if (deliveryApps.isEmpty()) return Result.success()

            val events = usageStatsHelper.getRecentEvents(
                packageList = deliveryApps,
                intervalMs = POLL_INTERVAL_MS,
            )

            // MOVE_TO_FOREGROUND 이벤트 감지
            val isAppLaunched = events.any {
                it.eventType == 1 // UsageEvents.Event.MOVE_TO_FOREGROUND = 1
            }

            if (isAppLaunched) {
                notificationHelper.sendMealReminder(
                    message = "야식의 유혹이 찾아왔나요? 시원한 물 한 잔을 마시거나 스트레칭으로 몸을 가볍게 해보는 건 어떨까요? 💪",
                    mealType = "LATE_NIGHT"
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 15 * 60 * 1000L
    }
}
