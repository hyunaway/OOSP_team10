// 경로: com/example/habittracker/data/usage/UsageStatsHelper.kt
package com.example.habittracker.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        val INTERESTED_PACKAGES = listOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.zhiliaoapp.musically",   // TikTok
            "com.twitter.android",         // X (Twitter)
            "com.facebook.katana",         // Facebook
            "com.facebook.orca",           // Messenger
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.netflix.mediaclient",
        )
    }

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getRecentEvents(
        packageList: List<String>,
        intervalMs: Long,
    ): List<UsageEventWrapper> {
        if (!hasUsageAccess()) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - intervalMs

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val result = mutableListOf<UsageEventWrapper>()
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.packageName !in packageList) continue
            if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND &&
                event.eventType != UsageEvents.Event.MOVE_TO_BACKGROUND
            ) continue

            result.add(
                UsageEventWrapper(
                    appPackage = event.packageName,
                    timeStamp = event.timeStamp,
                    eventType = event.eventType,
                )
            )
        }

        return result
    }
}
