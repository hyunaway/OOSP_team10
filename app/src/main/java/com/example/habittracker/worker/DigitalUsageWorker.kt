// 경로: com/example/habittracker/worker/DigitalUsageWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.usage.UsageSessionCalculator
import com.example.habittracker.data.usage.UsageStatsHelper
import com.example.habittracker.domain.usecase.digital.GetLatestDigitalInterventionTimestampUseCase
import com.example.habittracker.domain.usecase.digital.GetTodayDigitalStatusUseCase
import com.example.habittracker.domain.usecase.digital.LogDigitalInterventionUseCase
import com.example.habittracker.domain.usecase.digital.SaveDigitalSessionUseCase
import com.example.habittracker.util.NotificationHelper
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
    private val getTodayDigitalStatusUseCase: GetTodayDigitalStatusUseCase,
    private val getLatestDigitalInterventionTimestampUseCase: GetLatestDigitalInterventionTimestampUseCase,
    private val logDigitalInterventionUseCase: LogDigitalInterventionUseCase,
    private val notificationHelper: NotificationHelper,
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

            sendInterventionIfNeeded(selectedPackages)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun sendInterventionIfNeeded(selectedPackages: Set<String>) {
        val thresholdMinutes = userPreferenceManager.digitalInterventionThresholdMinutesFlow
            .first()
            .coerceAtLeast(1)
        val cooldownMillis = userPreferenceManager.digitalInterventionCooldownMinutesFlow
            .first()
            .coerceAtLeast(1)
            .times(60_000L)

        val status = getTodayDigitalStatusUseCase().first()
        val target = status.appUsageMap
            .filterKeys { it in selectedPackages }
            .filterValues { it >= thresholdMinutes }
            .maxByOrNull { it.value }
            ?: return

        val now = System.currentTimeMillis()
        val latestInterventionAt = getLatestDigitalInterventionTimestampUseCase(target.key)
        if (latestInterventionAt != null && now - latestInterventionAt < cooldownMillis) return

        val messageTone = userPreferenceManager.preferredMessageToneFlow
            .first()
            .ifBlank { DEFAULT_MESSAGE_TONE }
        val interventionId = logDigitalInterventionUseCase(
            appPackage = target.key,
            triggerDuration = target.value,
            messageTone = messageTone,
            timestamp = now,
            actionType = ACTION_TYPE_NOTIFICATION,
        )
        notificationHelper.sendDigitalIntervention(
            message = buildInterventionMessage(target.key, target.value),
            appPackage = target.key,
            interventionId = interventionId,
        )
    }

    private fun buildInterventionMessage(appPackage: String, totalMinutes: Int): String {
        val appName = resolveAppName(appPackage)
        return "${appName}를 벌써 ${totalMinutes}분 사용했어요. 잠깐 눈을 쉬어볼까요?"
    }

    private fun resolveAppName(appPackage: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val appInfo = packageManager.getApplicationInfo(appPackage, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            appPackage
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30 * 60 * 1_000L
        private const val DEFAULT_MESSAGE_TONE = "gentle"
        private const val ACTION_TYPE_NOTIFICATION = "notification"
    }
}
