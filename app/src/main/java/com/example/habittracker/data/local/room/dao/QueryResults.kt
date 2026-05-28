// 경로: com/example/habittracker/data/local/room/dao/QueryResults.kt
package com.example.habittracker.data.local.room.dao

data class HourlyCount(val hour: Int, val count: Int)
data class WeekdayCount(val weekday: Int, val count: Int)
data class AppDurationSum(val appPackage: String, val total: Int)
data class AppAvgDuration(val appPackage: String, val avgDuration: Float)
data class ToneReactionRate(val messageTone: String, val rate: Float)
data class BodyPartCount(val bodyPart: String, val count: Int)
data class MealTypeCount(val type: String, val count: Int)
data class CategoryClickRate(val category: String, val rate: Float)
data class HourlyIgnoreRate(val hour: Int, val ignoreRate: Float)
