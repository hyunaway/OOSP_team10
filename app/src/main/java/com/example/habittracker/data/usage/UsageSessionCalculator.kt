// 경로: com/example/habittracker/data/usage/UsageSessionCalculator.kt
package com.example.habittracker.data.usage

import javax.inject.Inject
import javax.inject.Singleton

data class AppSession(
    val appPackage: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
)

@Singleton
class UsageSessionCalculator @Inject constructor() {

    fun calculate(events: List<UsageEventWrapper>): List<AppSession> {
        val sessions = mutableListOf<AppSession>()
        val openSessions = mutableMapOf<String, Long>()

        for (event in events.sortedBy { it.timeStamp }) {
            when (event.eventType) {
                UsageEventWrapper.FOREGROUND -> {
                    openSessions[event.appPackage] = event.timeStamp
                }
                UsageEventWrapper.BACKGROUND -> {
                    val startTime = openSessions.remove(event.appPackage) ?: continue
                    sessions.add(buildSession(event.appPackage, startTime, event.timeStamp))
                }
            }
        }

        val now = System.currentTimeMillis()
        for ((pkg, startTime) in openSessions) {
            sessions.add(buildSession(pkg, startTime, now))
        }

        return sessions
    }

    private fun buildSession(appPackage: String, startTime: Long, endTime: Long): AppSession {
        val durationMinutes = ((endTime - startTime) / 60_000L).toInt().coerceAtLeast(0)
        return AppSession(
            appPackage = appPackage,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
        )
    }
}
