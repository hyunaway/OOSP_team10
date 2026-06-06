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
import com.example.habittracker.domain.usecase.digital.ResolveDigitalInterventionActionUseCase
import com.example.habittracker.domain.usecase.digital.SaveDigitalSessionUseCase
import com.example.habittracker.domain.analysis.PersonalizationResolver
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.util.formatMinutes
import com.example.habittracker.widget.WidgetUpdateHelper
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
    private val resolveDigitalInterventionActionUseCase: ResolveDigitalInterventionActionUseCase,
    private val notificationHelper: NotificationHelper,
    private val personalizationResolver: PersonalizationResolver,
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
        } finally {
            try {
                WidgetUpdateHelper.updateAllWidgets(applicationContext)
            } catch (_: Exception) {}
        }
    }

    private suspend fun sendInterventionIfNeeded(selectedPackages: Set<String>) {
        val status = getTodayDigitalStatusUseCase().first()
        val target = status.appUsageMap
            .filterKeys { it in selectedPackages }
            .map { (pkg, usage) ->
                val appThreshold = personalizationResolver.resolveDigitalThresholdMinutes(pkg)
                Triple(pkg, usage, appThreshold)
            }
            .filter { (_, usage, threshold) -> usage >= threshold }
            .maxByOrNull { (_, usage, _) -> usage }
            ?: return

        val appPackage = target.first
        val usageMinutes = target.second
        val thresholdMinutes = target.third

        val cooldownMillis = userPreferenceManager.digitalInterventionCooldownMinutesFlow
            .first()
            .coerceAtLeast(1)
            .times(60_000L)

        val now = System.currentTimeMillis()
        val latestInterventionAt = getLatestDigitalInterventionTimestampUseCase(appPackage)
        if (latestInterventionAt != null && now - latestInterventionAt < cooldownMillis) return

        val messageTone = userPreferenceManager.preferredMessageToneFlow
            .first()
            .ifBlank { DEFAULT_MESSAGE_TONE }
        val interventionId = logDigitalInterventionUseCase(
            appPackage = appPackage,
            triggerDuration = usageMinutes,
            messageTone = messageTone,
            timestamp = now,
            actionType = ACTION_TYPE_NOTIFICATION,
        )
        val recommendedAction = resolveDigitalInterventionActionUseCase()
        notificationHelper.sendDigitalIntervention(
            message = buildInterventionMessage(appPackage, usageMinutes, recommendedAction.message, messageTone),
            appPackage = appPackage,
            interventionId = interventionId,
        )
    }

    private fun buildInterventionMessage(
        appPackage: String,
        totalMinutes: Int,
        recommendedMessage: String,
        messageTone: String,
    ): String {
        val appName = resolveAppName(appPackage)
        val baseMessage = "${appName}를 ${formatMinutes(totalMinutes)} 사용했어요. $recommendedMessage"
        return applyMessageTone(baseMessage, messageTone)
    }

    private fun applyMessageTone(message: String, tone: String): String {
        return when (tone.uppercase()) {
            "PRAISE" -> "$message 지금까지 잘 참아온 스스로를 칭찬해 주세요! 조금만 더 힘내볼까요? 👏"
            "HUMOR" -> "$message 스마트폰이 피곤해서 기절하기 일보직전이에요! 잠깐 쉬어주는 게 신상에 좋습니다. 🤭"
            "CHALLENGE" -> "$message 오늘 스마트폰 사용 줄이기 도전 중이신가요? 지금 멈추면 도전에 성공할 확률이 올라갑니다! 🔥"
            "EMPATHY" -> "$message 화면을 오래 보느라 눈이 많이 피로하셨죠? 잠깐 눈을 감고 휴식을 취해보세요. 🌿"
            else -> message
        }
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
