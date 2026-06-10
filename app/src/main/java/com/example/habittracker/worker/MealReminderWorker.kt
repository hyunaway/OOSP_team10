// 경로: com/example/habittracker/worker/MealReminderWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.MealDao
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.analysis.DefaultValues
import com.example.habittracker.domain.analysis.PersonalizationResolver
import com.example.habittracker.util.MessageToneSelector
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 식사 알림 Worker.
 *
 * 개인화 적용 범위:
 *  - 1차 알림 기준점 → PersonalizationResolver.resolve*PeakMinutes() (peak 또는 fallback)
 *  - 2차(재알림) · 결식경고 · 야식칭찬 → 1차 기준점 + 고정 오프셋 / DefaultValues 고정 시각
 *  - 발송 메커니즘(NotificationHelper) · 중복 방지(lastMealReminderId) → 수정 없음
 *
 * ready = false 이면 DefaultValues 상수를 사용 → 기존 동작 100% 동일.
 */
@HiltWorker
class MealReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    userPreferenceManager: UserPreferenceManager,
    personalizationResolver: PersonalizationResolver,
    private val notificationHelper: NotificationHelper,
    private val messageToneSelector: MessageToneSelector,
    private val mealDao: MealDao,
) : BaseReminderWorker(context, params, userPreferenceManager, personalizationResolver) {

    override suspend fun doRemind(): Result {
        return try {
            val nowMinutes = run {
                val cal = Calendar.getInstance()
                cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            }

            val todayStr   = getTodayDateString()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val logs = mealDao.getLogsBetween(todayStart, Long.MAX_VALUE).first()
            val breakfastLogged = logs.any { it.type == MealType.BREAKFAST }
            val lunchLogged     = logs.any { it.type == MealType.LUNCH }
            val dinnerLogged    = logs.any { it.type == MealType.DINNER }
            val lastReminderId  = userPreferenceManager.lastMealReminderIdFlow.first()

            // ── 개인화 1차 기준점 해결 (4단계 fallback 포함) ─────────────────
            val bfPeak  = personalizationResolver.resolveBreakfastPeakMinutes()
            val luPeak  = personalizationResolver.resolveLunchPeakMinutes()
            val diPeak  = personalizationResolver.resolveDinnerPeakMinutes()
            val lnPeak  = personalizationResolver.resolveLateNightPeakMinutes()
            val win     = DefaultValues.MEAL_NOTIFICATION_WINDOW_MINUTES

            // ── 2차 알림 = 1차 기준점 + 고정 오프셋 ─────────────────────────
            val bfPeak2 = bfPeak + DefaultValues.BREAKFAST_REMINDER2_OFFSET_MINUTES
            val luPeak2 = luPeak + DefaultValues.LUNCH_REMINDER2_OFFSET_MINUTES
            val diPeak2 = diPeak + DefaultValues.DINNER_REMINDER2_OFFSET_MINUTES
            
            // 야식 알림 조건
            val isMealReady = userPreferenceManager.mealPersonalizationReadyFlow.first()
            val lnWarnStart = lnPeak - 15

            suspend fun checkAndSend(reminderId: String, mealType: String, title: String) {
                if (lastReminderId == reminderId) return
                val message = messageToneSelector.selectByPreference(
                    "meal", getPreferredTone(), getFatigueScore(),
                )
                notificationHelper.sendMealReminder("$title: $message", mealType)
                userPreferenceManager.updateLastMealReminderId(reminderId)
            }

            suspend fun triggerMealLack(skipId: String, msg: String) {
                if (lastReminderId == skipId) return
                notificationHelper.sendMealReminder(msg, "LACK")
                userPreferenceManager.updateLastMealReminderId(skipId)
            }

            // ── 아침 ─────────────────────────────────────────────────────────
            when {
                // ── 야식 방지 알림 (개인화 완료 시 피크 15분 전 발송) ───────────────
                isMealReady && nowMinutes in lnWarnStart..lnPeak -> {
                    val reminderId = "$todayStr:LATE_NIGHT_WARN"
                    if (lastReminderId != reminderId) {
                        notificationHelper.sendMealReminder(
                            "야식 생각이 나는 시간이에요! 따뜻한 물 한 잔으로 속을 달래보는 건 어떨까요?",
                            "LATE_NIGHT"
                        )
                        userPreferenceManager.updateLastMealReminderId(reminderId)
                    }
                }

                nowMinutes in bfPeak..(bfPeak + win) && !breakfastLogged ->
                    checkAndSend("$todayStr:BREAKFAST_1", "BREAKFAST", "[아침 식사 알림] 아침 식사 시간입니다")

                nowMinutes in bfPeak2..(bfPeak2 + win) && !breakfastLogged ->
                    checkAndSend("$todayStr:BREAKFAST_2", "BREAKFAST", "[아침 식사 재알림] 아침 시간 종료 1시간 전입니다")

                // ── 점심 ─────────────────────────────────────────────────────
                nowMinutes in luPeak..(luPeak + win) && !lunchLogged ->
                    checkAndSend("$todayStr:LUNCH_1", "LUNCH", "[점심 식사 알림] 점심 식사 시간입니다")

                nowMinutes in luPeak2..(luPeak2 + win) && !lunchLogged ->
                    checkAndSend("$todayStr:LUNCH_2", "LUNCH", "[점심 식사 재알림] 점심 시간 종료 1시간 전입니다")

                // ── 저녁 ─────────────────────────────────────────────────────
                nowMinutes in diPeak..(diPeak + win) && !dinnerLogged ->
                    checkAndSend("$todayStr:DINNER_1", "DINNER", "[저녁 식사 알림] 저녁 식사 시간입니다")

                nowMinutes in diPeak2..(diPeak2 + win) && !dinnerLogged ->
                    checkAndSend("$todayStr:DINNER_2", "DINNER", "[저녁 식사 재알림] 저녁 시간 종료 1시간 전입니다")

                // ── 고정 시각 알림 (DefaultValues 상수, 개인화 미적용) ─────────
                nowMinutes in DefaultValues.LATE_NIGHT_PRAISE_MINUTES..(DefaultValues.LATE_NIGHT_PRAISE_MINUTES + win) -> {
                    val reminderId = "$todayStr:LATE_NIGHT_SUCCESS"
                    if (lastReminderId != reminderId) {
                        val yesterdayStr  = java.time.LocalDate.now().minusDays(1).toString()
                        val lateNightCount = mealDao.getLogsByMealDate(yesterdayStr)
                            .count { it.type == MealType.LATE_NIGHT || it.isLateNight }
                        if (lateNightCount == 0) {
                            notificationHelper.sendMealReminder("어젯밤 야식을 참으셨군요! 대단해요 💪", "LATE_NIGHT")
                        }
                        userPreferenceManager.updateLastMealReminderId(reminderId)
                    }
                }

                nowMinutes in DefaultValues.LUNCH_WARN_MINUTES..(DefaultValues.LUNCH_WARN_MINUTES + win) && !lunchLogged ->
                    checkAndSend("$todayStr:LUNCH_WARN", "LUNCH", "[점심 결식 경고] 아직 점심 기록이 없습니다. 오후 4시가 지났습니다")

                // ── 결식 감지 (고정 시각, 개인화 미적용) ──────────────────────
                nowMinutes in DefaultValues.SKIP_BREAKFAST_MINUTES..(DefaultValues.SKIP_BREAKFAST_MINUTES + win) && !breakfastLogged ->
                    triggerMealLack("$todayStr:SKIP_BREAKFAST", "아침 식사를 거르셨습니다. 아바타의 체력이 저하됩니다 😢")

                nowMinutes in DefaultValues.SKIP_LUNCH_MINUTES..(DefaultValues.SKIP_LUNCH_MINUTES + win) && !lunchLogged ->
                    triggerMealLack("$todayStr:SKIP_LUNCH", "점심 식사를 거르셨습니다. 아바타의 체력이 저하됩니다 😢")

                nowMinutes in DefaultValues.SKIP_DINNER_MINUTES..(DefaultValues.SKIP_DINNER_MINUTES + win) && !dinnerLogged ->
                    triggerMealLack("$todayStr:SKIP_DINNER", "저녁 식사를 거르셨습니다. 아바타의 체력이 저하됩니다 😢")
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            try { WidgetUpdateHelper.updateAllWidgets(applicationContext) } catch (_: Exception) {}
        }
    }

    private fun getTodayDateString(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
}
