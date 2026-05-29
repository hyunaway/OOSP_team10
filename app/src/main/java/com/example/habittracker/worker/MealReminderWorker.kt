// 경로: com/example/habittracker/worker/MealReminderWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.MealDao
import com.example.habittracker.data.model.MealType
import com.example.habittracker.util.MessageToneSelector
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class MealReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    userPreferenceManager: UserPreferenceManager,
    private val notificationHelper: NotificationHelper,
    private val messageToneSelector: MessageToneSelector,
    private val mealDao: MealDao,
) : BaseReminderWorker(context, params, userPreferenceManager) {

    override suspend fun doRemind(): Result {
        return try {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            
            val todayStr = getTodayDateString()
            val logs = mealDao.getTodayLogs().first()
            val breakfastLogged = logs.any { it.type == MealType.BREAKFAST }
            val lunchLogged = logs.any { it.type == MealType.LUNCH }
            val dinnerLogged = logs.any { it.type == MealType.DINNER }
            
            val lastReminderId = userPreferenceManager.lastMealReminderIdFlow.first()
            
            suspend fun checkAndSend(reminderId: String, mealType: String, messageTitle: String) {
                if (lastReminderId == reminderId) return
                
                val tone = getPreferredTone()
                val fatigue = getFatigueScore()
                val message = messageToneSelector.selectByPreference("meal", tone, fatigue)
                
                notificationHelper.sendMealReminder("$messageTitle: $message", mealType)
                userPreferenceManager.updateLastMealReminderId(reminderId)
            }
            
            suspend fun triggerMealLack(skipId: String, messageTitle: String) {
                if (lastReminderId == skipId) return
                notificationHelper.sendMealReminder(messageTitle, "LACK")
                userPreferenceManager.updateLastMealReminderId(skipId)
            }
            
            // 1. 아침 시간대 (07:00-10:00)
            if (hour == 7 && minute in 30..44) {
                if (!breakfastLogged) {
                    checkAndSend("$todayStr:BREAKFAST_1", "BREAKFAST", "[아침 식사 알림] 아침 식사 시간입니다")
                }
            } else if (hour == 9 && minute in 0..14) {
                if (!breakfastLogged) {
                    checkAndSend("$todayStr:BREAKFAST_2", "BREAKFAST", "[아침 식사 재알림] 아침 시간 종료 1시간 전입니다")
                }
            }
            
            // 2. 점심 시간대 (11:30-14:00)
            else if (hour == 12 && minute in 0..14) {
                if (!lunchLogged) {
                    checkAndSend("$todayStr:LUNCH_1", "LUNCH", "[점심 식사 알림] 점심 식사 시간입니다")
                }
            } else if (hour == 13 && minute in 0..14) {
                if (!lunchLogged) {
                    checkAndSend("$todayStr:LUNCH_2", "LUNCH", "[점심 식사 재알림] 점심 시간 종료 1시간 전입니다")
                }
            }
            
            // 3. 저녁 시간대 (17:30-20:00)
            else if (hour == 18 && minute in 0..14) {
                if (!dinnerLogged) {
                    checkAndSend("$todayStr:DINNER_1", "DINNER", "[저녁 식사 알림] 저녁 식사 시간입니다")
                }
            } else if (hour == 19 && minute in 0..14) {
                if (!dinnerLogged) {
                    checkAndSend("$todayStr:DINNER_2", "DINNER", "[저녁 식사 재알림] 저녁 시간 종료 1시간 전입니다")
                }
            }
            
            // 4. 오후 4시 경고 알림 (16:00-16:14)
            else if (hour == 16 && minute in 0..14) {
                if (!lunchLogged) {
                    checkAndSend("$todayStr:LUNCH_WARN", "LUNCH", "[점심 결식 경고] 아직 점심 기록이 없습니다. 오후 4시가 지났습니다")
                }
            }
            
            // 4-2. 야식 자제 성공 칭찬 알림 (08:00-08:14)
            else if (hour == 8 && minute in 0..14) {
                val yesterdayStr = java.time.LocalDate.now().minusDays(1).toString()
                val yesterdayLogs = mealDao.getLogsByMealDate(yesterdayStr)
                val lateNightCount = yesterdayLogs.count { it.type == MealType.LATE_NIGHT || it.isLateNight }
                
                val reminderId = "$todayStr:LATE_NIGHT_SUCCESS"
                if (lastReminderId != reminderId) {
                    if (lateNightCount == 0) {
                        notificationHelper.sendMealReminder("어젯밤 야식을 참으셨군요! 대단해요 💪", "LATE_NIGHT")
                    }
                    userPreferenceManager.updateLastMealReminderId(reminderId)
                }
            }
            
            // 5. 결식 감지 및 아바타 상태 저하 트리거 (아침 12:00, 점심 16:00, 저녁 22:00)
            else if (hour == 12 && minute in 15..29) {
                if (!breakfastLogged) {
                    triggerMealLack("$todayStr:SKIP_BREAKFAST", "아침 식사를 거르셨습니다. 아바타의 체력이 저하됩니다 😢")
                }
            } else if (hour == 16 && minute in 15..29) {
                if (!lunchLogged) {
                    triggerMealLack("$todayStr:SKIP_LUNCH", "점심 식사를 거르셨습니다. 아바타의 체력이 저하됩니다 😢")
                }
            } else if (hour == 22 && minute in 0..14) {
                if (!dinnerLogged) {
                    triggerMealLack("$todayStr:SKIP_DINNER", "저녁 식사를 거르셨습니다. 아바타의 체력이 저하됩니다 😢")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            // TODO: 식사 부족 판정 로직 병합 후 MealStatus.LACK 연결
            try {
                WidgetUpdateHelper.updateAllWidgets(applicationContext)
            } catch (_: Exception) {}
        }
    }

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun currentMealType(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..10 -> "BREAKFAST"
            in 11..14 -> "LUNCH"
            in 17..21 -> "DINNER"
            else -> "LATE_NIGHT"
        }
    }
}
