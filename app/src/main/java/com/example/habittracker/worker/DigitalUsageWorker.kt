// 경로: com/example/habittracker/worker/DigitalUsageWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.usage.UsageSessionCalculator
import com.example.habittracker.data.usage.UsageStatsHelper
import com.example.habittracker.domain.usecase.digital.SaveDigitalSessionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DigitalUsageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userPreferenceManager: UserPreferenceManager,
    private val usageStatsHelper: UsageStatsHelper,
    private val usageSessionCalculator: UsageSessionCalculator,
    private val saveDigitalSessionUseCase: SaveDigitalSessionUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!usageStatsHelper.hasUsageAccess()) return Result.success()
        return try {
            val selectedPackages = userPreferenceManager.selectedDigitalPackagesFlow.first()
            if (selectedPackages.isEmpty()) return Result.success()

            val events = usageStatsHelper.getRecentEvents(
                packageList = selectedPackages,
                intervalMs = POLL_INTERVAL_MS,
            )
            usageSessionCalculator.calculate(events).forEach { session ->
                saveDigitalSessionUseCase(
                    appPackage = session.appPackage,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    durationMinutes = session.durationMinutes,
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30 * 60 * 1_000L
    }
}
