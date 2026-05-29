// 경로: com/example/habittracker/worker/WaterReminderWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.usecase.water.CheckWaterInterventionNeededUseCase
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    userPreferenceManager: UserPreferenceManager,
    private val notificationHelper: NotificationHelper,
    private val checkWaterInterventionNeededUseCase: CheckWaterInterventionNeededUseCase,
) : BaseReminderWorker(context, params, userPreferenceManager) {

    override suspend fun doRemind(): Result {
        return try {
            val status = checkWaterInterventionNeededUseCase()
            if (!status.isNeedWater) return Result.success()

            notificationHelper.sendWaterReminder(status.message)
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
