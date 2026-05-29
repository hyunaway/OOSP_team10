// 경로: com/example/habittracker/worker/StretchReminderWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.StretchDao
import com.example.habittracker.util.MessageToneSelector
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class StretchReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    userPreferenceManager: UserPreferenceManager,
    private val notificationHelper: NotificationHelper,
    private val messageToneSelector: MessageToneSelector,
    private val stretchDao: StretchDao,
) : BaseReminderWorker(context, params, userPreferenceManager) {

    override suspend fun doRemind(): Result {
        return try {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val todayStr = getTodayDateString()
            val lastReminderId = userPreferenceManager.lastStretchReminderIdFlow.first()

            val amEnabled = userPreferenceManager.stretchSlotAmEnabledFlow.first()
            val pmEnabled = userPreferenceManager.stretchSlotPmEnabledFlow.first()
            val eveEnabled = userPreferenceManager.stretchSlotEveEnabledFlow.first()
            val nightEnabled = userPreferenceManager.stretchSlotNightEnabledFlow.first()

            var targetSlot: String? = null
            var slotReminderId: String? = null
            var isFirstActiveSlot = false

            // 1. 오전 슬롯: 10:30 ~ 10:44
            if (hour == 10 && minute in 30..44) {
                if (amEnabled) {
                    targetSlot = "아침"
                    slotReminderId = "$todayStr:STRETCH_AM"
                    isFirstActiveSlot = true
                }
            }
            // 2. 점심 후 슬롯: 14:00 ~ 14:14
            else if (hour == 14 && minute in 0..14) {
                if (pmEnabled) {
                    targetSlot = "점심"
                    slotReminderId = "$todayStr:STRETCH_PM"
                    isFirstActiveSlot = !amEnabled
                }
            }
            // 3. 저녁 슬롯: 19:30 ~ 19:44
            else if (hour == 19 && minute in 30..44) {
                if (eveEnabled) {
                    targetSlot = "저녁"
                    slotReminderId = "$todayStr:STRETCH_EVE"
                    isFirstActiveSlot = !amEnabled && !pmEnabled
                }
            }
            // 4. 취침 전 슬롯: 22:00 ~ 22:14
            else if (hour == 22 && minute in 0..14) {
                if (nightEnabled) {
                    targetSlot = "기타"
                    slotReminderId = "$todayStr:STRETCH_NIGHT"
                    isFirstActiveSlot = !amEnabled && !pmEnabled && !eveEnabled
                }
            }

            if (targetSlot != null && slotReminderId != null) {
                if (lastReminderId == slotReminderId) return Result.success()

                // 어제 기록 개수 확인
                val yesterdayStr = java.time.LocalDate.now().minusDays(1).toString()
                val yesterdayCount = stretchDao.getTodayStretchCount(yesterdayStr)

                val shouldUpgrade = yesterdayCount == 0 && isFirstActiveSlot

                val tone = getPreferredTone()
                val fatigue = getFatigueScore()
                val baseMsg = messageToneSelector.selectByPreference("stretch", tone, fatigue)

                val finalMessage = if (shouldUpgrade) {
                    "어제 스트레칭을 못 하셨네요. 오늘은 함께해요! $baseMsg"
                } else {
                    baseMsg
                }

                notificationHelper.sendStretchReminder(
                    message = finalMessage,
                    trigger = "slot_$targetSlot",
                    isUrgent = shouldUpgrade
                )

                userPreferenceManager.updateLastStretchReminderId(slotReminderId)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            // TODO: 스트레칭 부족 판정 로직 병합 후 StretchStatus.LACK 연결
            try {
                WidgetUpdateHelper.updateAllWidgets(applicationContext)
            } catch (_: Exception) {}
        }
    }

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
