// 경로: com/example/habittracker/worker/DailySummaryWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.domain.usecase.digital.GetTodayDigitalStatusUseCase
import com.example.habittracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getTodayDigitalStatusUseCase: GetTodayDigitalStatusUseCase,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val status = getTodayDigitalStatusUseCase().first()
            notificationHelper.sendDigitalDailySummary(
                totalMinutes = status.totalUsageMinutes,
                appBreakdown = status.appUsageMap,
            )
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
