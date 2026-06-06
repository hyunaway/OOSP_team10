package com.example.habittracker.domain.analysis

import java.util.Calendar

/**
 * 분석 대상 시간 윈도우 헬퍼.
 * 오늘 자정 기준으로 최근 N일의 [startMillis, endMillis] 범위를 반환한다.
 */
object AnalysisWindow {

    const val DEFAULT_DAYS = 30
    const val STRETCH_DAYS = 7

    /**
     * 오늘 기준 최근 [days]일의 범위를 반환한다.
     * @return LongRange(startMillis, endMillis)
     *   - startMillis: N일 전 자정 00:00:00.000
     *   - endMillis  : 오늘 자정 직전 23:59:59.999 (= 현재 시각)
     */
    fun recent(days: Int = DEFAULT_DAYS): LongRange {
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        return startCal.timeInMillis..endCal.timeInMillis
    }
}
