// 경로: com/example/habittracker/data/usage/UsageEventWrapper.kt
package com.example.habittracker.data.usage

data class UsageEventWrapper(
    val appPackage: String,
    val timeStamp: Long,
    val eventType: Int,
) {
    companion object {
        // android.app.usage.UsageEvents.Event 상수값과 동일
        const val FOREGROUND = 1
        const val BACKGROUND = 2
    }
}
