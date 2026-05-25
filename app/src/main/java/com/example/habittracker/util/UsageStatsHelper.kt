// 경로: com/example/habittracker/util/UsageStatsHelper.kt
package com.example.habittracker.util

import android.content.Context

// TODO: 다음 세션에서 UsageStats 실제 조회 로직을 구현한다.
class UsageStatsHelper(private val context: Context) {

    fun hasPermission(): Boolean {
        // TODO: check PACKAGE_USAGE_STATS permission via AppOpsManager
        return false
    }

    fun getUsageMap(startTime: Long, endTime: Long): Map<String, Long> {
        // TODO: query UsageStatsManager
        return emptyMap()
    }
}
