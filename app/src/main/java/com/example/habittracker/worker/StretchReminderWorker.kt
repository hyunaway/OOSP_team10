// 경로: com/example/habittracker/worker/StretchReminderWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.usecase.stretch.CheckStretchInterventionNeededUseCase
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class StretchReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    userPreferenceManager: UserPreferenceManager,
    private val notificationHelper: NotificationHelper,
    private val checkStretchInterventionNeededUseCase: CheckStretchInterventionNeededUseCase,
) : BaseReminderWorker(context, params, userPreferenceManager) {

    override suspend fun doRemind(): Result {
        return try {
            if (userPreferenceManager.todayActiveStartedAtFlow.first() == null) {
                return Result.success()
            }

            val now = System.currentTimeMillis()
            val status = checkStretchInterventionNeededUseCase(now)
            if (status.isNeedStretch) {
                notificationHelper.sendStretchReminder(
                    message = status.message,
                    trigger = "activity_based",
                    isUrgent = false,
                )
                userPreferenceManager.updateLastStretchReminderAt(now)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            try {
                WidgetUpdateHelper.updateAllWidgets(applicationContext)
            } catch (_: Exception) {}
        }
    }

}
