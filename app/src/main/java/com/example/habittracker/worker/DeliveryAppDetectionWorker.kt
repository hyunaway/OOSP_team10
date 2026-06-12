package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.MealDao
import com.example.habittracker.data.model.MealType
import com.example.habittracker.data.usage.UsageEventWrapper
import com.example.habittracker.data.usage.UsageStatsHelper
import com.example.habittracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@HiltWorker
class DeliveryAppDetectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userPreferenceManager: UserPreferenceManager,
    private val usageStatsHelper: UsageStatsHelper,
    private val notificationHelper: NotificationHelper,
    private val mealDao: MealDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!usageStatsHelper.hasUsageAccess()) return Result.success()

        return try {
            if (isInSleepTime()) return Result.success()
            if (!isLateNightRiskHour()) return Result.success()

            val today = todayDateString()
            if (hasLateNightLog(today)) return Result.success()
            if (hasRecentLateNightReminder(today)) return Result.success()

            val deliveryApps = userPreferenceManager.registeredDeliveryPackagesFlow.first()
            if (deliveryApps.isEmpty()) return Result.success()

            val events = usageStatsHelper.getRecentEvents(
                packageList = deliveryApps,
                intervalMs = POLL_INTERVAL_MS,
            )
            val isAppLaunched = events.any {
                it.eventType == UsageEventWrapper.FOREGROUND
            }

            if (isAppLaunched) {
                notificationHelper.sendMealReminder(
                    message = "늦은 시간 배달앱을 열었어요. 야식 대신 물 한 잔이나 가벼운 스트레칭으로 넘겨볼까요?",
                    mealType = MealType.LATE_NIGHT.name,
                )
                userPreferenceManager.updateLastMealReminderId("$today:$ID_DELIVERY_LATE_NIGHT_WARN")
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun hasLateNightLog(today: String): Boolean =
        mealDao.getLogsByMealDate(today)
            .any { it.type == MealType.LATE_NIGHT || it.isLateNight }

    private suspend fun hasRecentLateNightReminder(today: String): Boolean {
        val lastReminderId = userPreferenceManager.lastMealReminderIdFlow.first()
        return lastReminderId == "$today:$ID_LATE_NIGHT_WARN" ||
            lastReminderId == "$today:$ID_DELIVERY_LATE_NIGHT_WARN"
    }

    private fun isLateNightRiskHour(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 21 || hour < 1
    }

    private suspend fun isInSleepTime(): Boolean {
        val bedMinutes = userPreferenceManager.getBedTimeAsMinutes().first()
        val wakeMinutes = userPreferenceManager.getWakeTimeAsMinutes().first()
        val currentMinutes = currentMinutesOfDay()
        return if (bedMinutes > wakeMinutes) {
            currentMinutes >= bedMinutes || currentMinutes < wakeMinutes
        } else {
            currentMinutes in bedMinutes until wakeMinutes
        }
    }

    private fun currentMinutesOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    companion object {
        private const val POLL_INTERVAL_MS = 15 * 60 * 1000L
        private const val ID_LATE_NIGHT_WARN = "LATE_NIGHT_WARN"
        private const val ID_DELIVERY_LATE_NIGHT_WARN = "DELIVERY_LATE_NIGHT_WARN"
    }
}
