// 경로: com/example/habittracker/HabitTrackerApplication.kt
package com.example.habittracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.habittracker.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HabitTrackerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        WorkScheduler.scheduleAllDefaults(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            NotificationChannel(CHANNEL_WATER, "Water Reminder", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_MEAL, "Meal Reminder", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_DIGITAL, "Digital Habit", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_STRETCH, "Stretch Reminder", NotificationManager.IMPORTANCE_DEFAULT),
        )

        manager.createNotificationChannels(channels)
    }

    companion object {
        const val CHANNEL_WATER = "CHANNEL_WATER"
        const val CHANNEL_MEAL = "CHANNEL_MEAL"
        const val CHANNEL_DIGITAL = "CHANNEL_DIGITAL"
        const val CHANNEL_STRETCH = "CHANNEL_STRETCH"
    }
}
