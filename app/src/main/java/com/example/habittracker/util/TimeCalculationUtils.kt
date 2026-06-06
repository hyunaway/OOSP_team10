package com.example.habittracker.util

import java.util.Calendar

/**
 * 프로젝트 내에서 공통으로 사용되는 시간 및 분 단위 계산 유틸리티 클래스입니다.
 */
object TimeCalculationUtils {
    private const val MINUTES_PER_DAY = 24 * 60

    /**
     * 타임스탬프(밀리초)를 하루 기준 분(0..1439)으로 변환합니다.
     */
    fun minutesOfDay(timestampMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    /**
     * "HH:mm" 형식의 문자열 취침 시각을 분(Minutes) 단위 수치로 파싱합니다.
     */
    fun parseBedTimeMinutes(value: String): Int? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (hour !in 0..24 || minute !in 0..59) return null
        if (hour == 24 && minute != 0) return null
        return if (hour == 0 && minute == 0) MINUTES_PER_DAY else hour * 60 + minute
    }

    /**
     * 시작 분(startMinutes)부터 취침 분(bedMinutes)까지 남은 시간을 계산합니다.
     */
    fun minutesUntilBed(startMinutes: Int, bedMinutes: Int): Int {
        val normalizedStart = startMinutes.mod(MINUTES_PER_DAY)
        val normalizedBed = if (bedMinutes == MINUTES_PER_DAY) MINUTES_PER_DAY else bedMinutes.mod(MINUTES_PER_DAY)
        if (normalizedBed == normalizedStart) return 0
        return if (normalizedBed > normalizedStart) {
            normalizedBed - normalizedStart
        } else {
            normalizedBed + MINUTES_PER_DAY - normalizedStart
        }
    }

    /**
     * 두 시점(startMinutes, endMinutes) 사이의 분 차이를 계산합니다.
     */
    fun minutesBetween(startMinutes: Int, endMinutes: Int): Int {
        val normalizedStart = startMinutes.mod(MINUTES_PER_DAY)
        val normalizedEnd = endMinutes.mod(MINUTES_PER_DAY)
        return if (normalizedEnd >= normalizedStart) {
            normalizedEnd - normalizedStart
        } else {
            normalizedEnd + MINUTES_PER_DAY - normalizedStart
        }
    }
}
