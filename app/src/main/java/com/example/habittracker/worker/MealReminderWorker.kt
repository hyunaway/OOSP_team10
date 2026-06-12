package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.MealDao
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.analysis.PersonalizationResolver
import com.example.habittracker.domain.usecase.meal.ExpectedMealWindow
import com.example.habittracker.domain.usecase.meal.GetCurrentMealInterventionStatusUseCase
import com.example.habittracker.domain.usecase.meal.MealInterventionIntensity
import com.example.habittracker.util.MessageToneSelector
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@HiltWorker
class MealReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    userPreferenceManager: UserPreferenceManager,
    personalizationResolver: PersonalizationResolver,
    private val notificationHelper: NotificationHelper,
    private val messageToneSelector: MessageToneSelector,
    private val mealDao: MealDao,
    private val getCurrentMealInterventionStatusUseCase: GetCurrentMealInterventionStatusUseCase,
) : BaseReminderWorker(context, params, userPreferenceManager, personalizationResolver) {

    override suspend fun doRemind(): Result {
        return try {
            if (userPreferenceManager.todayActiveStartedAtFlow.first() == null) {
                return Result.success()
            }

            val nowMinutes = currentMinutesOfDay()
            val today = todayDateString()
            val lastReminderId = userPreferenceManager.lastMealReminderIdFlow.first()

            val lateNightReminderId = "$today:$ID_LATE_NIGHT_WARN"
            if (
                !lastReminderId.isRecentLateNightReminder(today) &&
                shouldSendLateNightWarning(nowMinutes, today)
            ) {
                notificationHelper.sendMealReminder(
                    message = "야식이 생각나는 시간이에요. 물 한 잔이나 가벼운 스트레칭으로 흐름을 바꿔볼까요?",
                    mealType = MealType.LATE_NIGHT.name,
                )
                userPreferenceManager.updateLastMealReminderId(lateNightReminderId)
                return Result.success()
            }

            val status = getCurrentMealInterventionStatusUseCase()
            val window = status.currentWindow ?: return Result.success()
            val mealType = status.actionableMealType ?: return Result.success()

            if (!status.isActionable || status.intensity == MealInterventionIntensity.NONE) {
                return Result.success()
            }
            if (mealType == MealType.LATE_NIGHT) return Result.success()

            val peak = resolvePeakFor(mealType).clampToWindow(window)
            val stage = reminderStage(
                nowMinutes = nowMinutes,
                peakMinutes = peak,
                window = window,
            ) ?: return Result.success()

            val reminderId = "$today:${mealType.name}_$stage"
            if (lastReminderId == reminderId) return Result.success()

            val message = messageToneSelector.selectByPreference(
                "meal",
                getPreferredTone(),
                getFatigueScore(),
            )
            notificationHelper.sendMealReminder(
                message = "${mealLabel(mealType)} 식사 ${stage}차 알림: $message",
                mealType = mealType.name,
            )
            userPreferenceManager.updateLastMealReminderId(reminderId)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            try {
                WidgetUpdateHelper.updateAllWidgets(applicationContext)
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun shouldSendLateNightWarning(
        nowMinutes: Int,
        today: String,
    ): Boolean {
        val lateNightPeak = personalizationResolver.resolveLateNightPeakMinutes()
        val start = (lateNightPeak - LATE_NIGHT_WARNING_LEAD_MINUTES).coerceAtLeast(0)
        if (nowMinutes !in start..lateNightPeak) return false

        val todayLogs = mealDao.getLogsByMealDate(today)
        return todayLogs.none { it.type == MealType.LATE_NIGHT || it.isLateNight }
    }

    private suspend fun resolvePeakFor(mealType: MealType): Int =
        when (mealType) {
            MealType.BREAKFAST -> personalizationResolver.resolveBreakfastPeakMinutes()
            MealType.LUNCH -> personalizationResolver.resolveLunchPeakMinutes()
            MealType.DINNER -> personalizationResolver.resolveDinnerPeakMinutes()
            MealType.LATE_NIGHT -> personalizationResolver.resolveLateNightPeakMinutes()
        }

    private fun Int.clampToWindow(window: ExpectedMealWindow): Int {
        val safeEnd = (window.endMinutes - 1).coerceAtLeast(window.startMinutes)
        return coerceIn(window.startMinutes, safeEnd)
    }

    private fun reminderStage(
        nowMinutes: Int,
        peakMinutes: Int,
        window: ExpectedMealWindow,
    ): Int? {
        if (!window.contains(nowMinutes)) return null

        val firstEnd = (peakMinutes + REMINDER_WINDOW_MINUTES)
            .coerceAtMost(window.endMinutes - 1)
        if (nowMinutes in peakMinutes..firstEnd) return 1

        val secondStart = maxOf(
            peakMinutes + REMINDER_WINDOW_MINUTES + 1,
            window.endMinutes - REMINDER_WINDOW_MINUTES,
            window.startMinutes,
        ).coerceAtMost(window.endMinutes - 1)
        return if (nowMinutes in secondStart until window.endMinutes) 2 else null
    }

    private fun currentMinutesOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun mealLabel(mealType: MealType): String =
        when (mealType) {
            MealType.BREAKFAST -> "아침"
            MealType.LUNCH -> "점심"
            MealType.DINNER -> "저녁"
            MealType.LATE_NIGHT -> "야식"
        }

    private fun String.isRecentLateNightReminder(today: String): Boolean =
        this == "$today:$ID_LATE_NIGHT_WARN" ||
            this == "$today:$ID_DELIVERY_LATE_NIGHT_WARN"

    companion object {
        private const val REMINDER_WINDOW_MINUTES = 30
        private const val LATE_NIGHT_WARNING_LEAD_MINUTES = 30
        private const val ID_LATE_NIGHT_WARN = "LATE_NIGHT_WARN"
        private const val ID_DELIVERY_LATE_NIGHT_WARN = "DELIVERY_LATE_NIGHT_WARN"
    }
}
