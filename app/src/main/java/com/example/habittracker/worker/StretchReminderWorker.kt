// 경로: com/example/habittracker/worker/StretchReminderWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.util.MessageToneSelector
import com.example.habittracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class StretchReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    userPreferenceManager: UserPreferenceManager,
    private val notificationHelper: NotificationHelper,
    private val messageToneSelector: MessageToneSelector,
) : BaseReminderWorker(context, params, userPreferenceManager) {

    override suspend fun doRemind(): Result {
        return try {
            val message = messageToneSelector.selectByPreference(
                "stretch",
                getPreferredTone(),
                getFatigueScore(),
            )
            notificationHelper.sendStretchReminder(message)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
