// 경로: com/example/habittracker/worker/MealReminderWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.util.MessageToneSelector
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class MealReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    userPreferenceManager: UserPreferenceManager,
    private val notificationHelper: NotificationHelper,
    private val messageToneSelector: MessageToneSelector,
) : BaseReminderWorker(context, params, userPreferenceManager) {

    override suspend fun doRemind(): Result {
        return try {
            val message = messageToneSelector.selectByPreference(
                "meal",
                getPreferredTone(),
                getFatigueScore(),
            )
            notificationHelper.sendMealReminder(message, currentMealType())
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

    private fun currentMealType(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..10 -> "BREAKFAST"
            in 11..14 -> "LUNCH"
            in 17..21 -> "DINNER"
            else -> "SNACK"
        }
    }
}
