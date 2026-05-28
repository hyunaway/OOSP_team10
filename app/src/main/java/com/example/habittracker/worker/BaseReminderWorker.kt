// 경로: com/example/habittracker/worker/BaseReminderWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import kotlinx.coroutines.flow.first
import java.util.Calendar

abstract class BaseReminderWorker(
    context: Context,
    params: WorkerParameters,
    protected val userPreferenceManager: UserPreferenceManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (isInSleepTime()) return Result.success()
            doRemind()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    abstract suspend fun doRemind(): Result

    protected suspend fun isInSleepTime(): Boolean {
        val bedMinutes = userPreferenceManager.getBedTimeAsMinutes().first()
        val wakeMinutes = userPreferenceManager.getWakeTimeAsMinutes().first()
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return if (bedMinutes > wakeMinutes) {
            currentMinutes >= bedMinutes || currentMinutes < wakeMinutes
        } else {
            currentMinutes in bedMinutes until wakeMinutes
        }
    }

    protected suspend fun getPreferredTone(): String =
        userPreferenceManager.preferredMessageToneFlow.first()

    protected suspend fun getFatigueScore(): Float =
        userPreferenceManager.notificationFatigueScoreFlow.first()
}
