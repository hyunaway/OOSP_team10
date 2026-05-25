// 경로: com/example/habittracker/worker/WorkScheduler.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.habittracker.data.local.UserPreferenceManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit


object WorkScheduler {

    const val TAG_WATER = "water_reminder"
    const val TAG_MEAL = "meal_reminder"
    const val TAG_STRETCH = "stretch_reminder"
    const val TAG_DIGITAL = "digital_usage"
    const val TAG_PERSONALIZATION = "personalization"
    const val TAG_DAILY_SUMMARY = "daily_summary"

    private val batteryConstraint = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    suspend fun scheduleAll(context: Context, prefs: UserPreferenceManager) {
        val intervalMinutes = prefs.waterReminderIntervalMinutesFlow.first()
        scheduleWater(context, intervalMinutes)
        scheduleMeal(context)
        scheduleStretch(context)
        scheduleDigitalUsage(context)
        schedulePersonalization(context)
        scheduleDailySummary(context)
    }

    fun scheduleWater(context: Context, intervalMinutes: Int) {
        val safeInterval = maxOf(15, intervalMinutes).toLong()
        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(safeInterval, TimeUnit.MINUTES)
            .addTag(TAG_WATER)
            .setConstraints(batteryConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TAG_WATER, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleMeal(context: Context) {
        val request = PeriodicWorkRequestBuilder<MealReminderWorker>(3, TimeUnit.HOURS)
            .addTag(TAG_MEAL)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TAG_MEAL, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleStretch(context: Context) {
        val request = PeriodicWorkRequestBuilder<StretchReminderWorker>(1, TimeUnit.HOURS)
            .addTag(TAG_STRETCH)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TAG_STRETCH, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleDigitalUsage(context: Context) {
        val request = PeriodicWorkRequestBuilder<DigitalUsageWorker>(30, TimeUnit.MINUTES)
            .addTag(TAG_DIGITAL)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TAG_DIGITAL, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun schedulePersonalization(context: Context) {
        val request = PeriodicWorkRequestBuilder<PersonalizationWorker>(1, TimeUnit.DAYS)
            .addTag(TAG_PERSONALIZATION)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TAG_PERSONALIZATION, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleDailySummary(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .addTag(TAG_DAILY_SUMMARY)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TAG_DAILY_SUMMARY, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        listOf(TAG_WATER, TAG_MEAL, TAG_STRETCH, TAG_DIGITAL, TAG_PERSONALIZATION, TAG_DAILY_SUMMARY)
            .forEach { wm.cancelUniqueWork(it) }
    }

    suspend fun rescheduleAll(context: Context, prefs: UserPreferenceManager) {
        cancelAll(context)
        scheduleAll(context, prefs)
    }

    fun scheduleAllDefaults(context: Context) {
        scheduleWater(context, UserPreferenceManager.DEFAULT_WATER_REMINDER_INTERVAL_MINUTES)
        scheduleMeal(context)
        scheduleStretch(context)
        scheduleDigitalUsage(context)
        schedulePersonalization(context)
        scheduleDailySummary(context)
    }
}
