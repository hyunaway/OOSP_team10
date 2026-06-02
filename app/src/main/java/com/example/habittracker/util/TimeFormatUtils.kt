package com.example.habittracker.util

fun formatMinutes(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val remainingMinutes = safeMinutes % 60

    return when {
        hours == 0 -> "${remainingMinutes}분"
        remainingMinutes == 0 -> "${hours}시간"
        else -> "${hours}시간 ${remainingMinutes}분"
    }
}
