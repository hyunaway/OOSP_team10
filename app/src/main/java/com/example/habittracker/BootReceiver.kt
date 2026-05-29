// 경로: com/example/habittracker/BootReceiver.kt
package com.example.habittracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.widget.WidgetUpdateHelper
import com.example.habittracker.worker.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPreferenceManager: UserPreferenceManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "BOOT_COMPLETED received — rescheduling workers")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WorkScheduler.rescheduleAll(context.applicationContext, userPreferenceManager)
                Log.d(TAG, "Workers rescheduled successfully")
                try {
                    WidgetUpdateHelper.updateAllWidgets(context.applicationContext)
                    Log.d(TAG, "Widget updated on boot")
                } catch (e: Exception) {
                    Log.e(TAG, "Widget update failed on boot", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule workers on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
