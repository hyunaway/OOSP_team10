package com.example.habittracker.domain.analysis

import com.example.habittracker.domain.model.PeakWindow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// 보조 데이터 클래스
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 시간대 분포의 평균·표준편차·표본 크기.
 * calcPeakWindow 내부에서 range/concentration 산출에 활용 (별도 저장 X).
 */
data class TimeStat(
    val meanMinutes: Float,
    val stdDevMinutes: Float,
    val sampleSize: Int,
)

// ─────────────────────────────────────────────────────────────────────────────
// 공용 분석 엔진 (순수 함수 집합)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 4개 카테고리가 공유하는 결정론적 통계 엔진.
 *
 * 모든 메서드는 순수 함수:
 *  - 같은 입력 → 같은 출력
 *  - 외부 상태(DB, 시계) 접근 없음
 *  - 빈 입력 / 이상치에도 크래시 없이 안전값 반환
 */
object PersonalizationEngine {

    // ─── 1. calcPeakWindow ───────────────────────────────────────────────────

    /**
     * 타임스탬프 목록에서 "하루 중 가장 자주 일어나는 시간대"를 구한다.
     *
     * 알고리즘:
     *  1. 각 타임스탬프를 하루 기준 분(0~1439)으로 변환.
     *  2. 하루를 [binMinutes] 단위로 나눠 빈도 히스토그램 구성.
     *     (예: binMinutes=30 → 48개 bin)
     *  3. 최빈 bin을 peak로 선정. center = bin 중앙값(분).
     *  4. calcAverageTimeOfDay()로 mean·stdDev 계산:
     *     - rangeStart = center - stdDev (최소 0)
     *     - rangeEnd   = center + stdDev (최대 1439)
     *  5. concentration = 최빈 bin 빈도 / 전체 표본 수.
     *     → 집중도가 높을수록(값이 1에 가까울수록) 패턴이 규칙적.
     *  6. 이상치 자연 배제: 이상치가 존재해도 빈도가 낮아 최빈 bin에서 제외됨.
     *
     * @param timestamps Unix 밀리초 타임스탬프 목록 (빈 리스트 허용)
     * @param binMinutes bin 크기 (기본 30분)
     * @return 빈 입력 시 [PeakWindow.EMPTY] 반환
     */
    fun calcPeakWindow(
        timestamps: List<Long>,
        binMinutes: Int = 30,
    ): PeakWindow {
        if (timestamps.isEmpty()) return PeakWindow.EMPTY
 
        // Calendar 인스턴스를 루프 밖에서 한 번만 생성하여 메모리 낭비를 방지합니다.
        val cal = Calendar.getInstance()
        // 1. 타임스탬프 → 하루 기준 분(minuteOfDay)
        val minutesOfDay = timestamps.map { ts ->
            cal.timeInMillis = ts
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }
 
        // 2. 빈도 히스토그램: 하루를 binMinutes 크기로 나누어 구간별 카운팅을 진행합니다.
        val totalBins = (24 * 60 + binMinutes - 1) / binMinutes
        val histogram = IntArray(totalBins)
        for (m in minutesOfDay) {
            histogram[m / binMinutes]++
        }
 
        // 3. 가장 빈도가 높은 구간(최빈값)을 찾아 중심 시간(center)으로 설정합니다.
        val peakBinIndex = histogram.indices.maxByOrNull { histogram[it] } ?: 0
        val peakBinFreq = histogram[peakBinIndex]
        val centerMinutes = peakBinIndex * binMinutes + binMinutes / 2
 
        // 4. 고정 1시간(60분) 범위 계산 (Center 기준 앞뒤 30분씩)
        val rangeStart = (centerMinutes - 30).coerceAtLeast(0)
        val rangeEnd   = (centerMinutes + 30).coerceAtMost(23 * 60 + 59)
 
        // 5. 집중도(concentration) = 최빈 구간의 빈도 / 전체 샘플 수
        // 이 값이 클수록 일관되고 규칙적인 패턴임을 나타냅니다.
        val concentration = peakBinFreq.toFloat() / timestamps.size.toFloat()
 
        return PeakWindow(
            centerMinutes = centerMinutes,
            rangeStart    = rangeStart.coerceAtLeast(0),
            rangeEnd      = rangeEnd.coerceAtMost(23 * 60 + 59),
            concentration = concentration.coerceIn(0f, 1f),
        )
    }
 
    // ─── 2. calcAverageTimeOfDay ─────────────────────────────────────────────
 
    /**
     * 타임스탬프 목록의 하루 기준 분(minuteOfDay) 평균·표준편차를 계산한다.
     *
     * @return 빈 입력 시 TimeStat(0f, 0f, 0)
     */
    fun calcAverageTimeOfDay(timestamps: List<Long>): TimeStat {
        if (timestamps.isEmpty()) return TimeStat(0f, 0f, 0)
 
        val cal = Calendar.getInstance()
        val minutesOfDay = timestamps.map { ts ->
            cal.timeInMillis = ts
            (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).toFloat()
        }
 
        val mean = minutesOfDay.average().toFloat()
        val variance = minutesOfDay
            .map { (it - mean) * (it - mean) }
            .average()
            .toFloat()
        val stdDev = sqrt(variance)
 
        return TimeStat(
            meanMinutes   = mean,
            stdDevMinutes = stdDev,
            sampleSize    = timestamps.size,
        )
    }

    // ─── 3. calcRecurrenceInterval ───────────────────────────────────────────

    /**
     * yyyy-MM-dd 날짜 목록에서 연속 발생 간의 평균 간격(일)을 계산한다.
     * 배달 앱 주문 주기 등 "N일마다 한 번" 패턴 파악에 사용.
     *
     * @param dates "yyyy-MM-dd" 형식 날짜 목록 (중복 허용, 정렬 불필요)
     * @return 2개 미만이면 null (간격 계산 불가)
     */
    fun calcRecurrenceInterval(dates: List<String>): Float? {
        if (dates.size < 2) return null

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val sorted = dates
            .mapNotNull { runCatching { LocalDate.parse(it, formatter) }.getOrNull() }
            .distinct()
            .sorted()

        if (sorted.size < 2) return null

        val gaps = sorted.zipWithNext { a, b ->
            (b.toEpochDay() - a.toEpochDay()).toFloat()
        }
        return gaps.average().toFloat()
    }

    // ─── 4. calcMostFrequentSlot ─────────────────────────────────────────────

    /**
     * 스트레칭 timeSlot 목록 중 가장 자주 등장하는 슬롯을 반환한다.
     * (예: "아침", "점심", "저녁", "기타")
     *
     * @param slots timeSlot 문자열 목록 (빈 리스트 허용)
     * @return null = 입력 없음 또는 빈도 동률(첫 번째 반환)
     */
    fun calcMostFrequentSlot(slots: List<String>): String? {
        if (slots.isEmpty()) return null
        return slots
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    // ─── 5. calcWaterTarget ──────────────────────────────────────────────────

    /**
     * 체중·BMI 기반 일일 물 섭취 목표량(ml)을 계산한다.
     *
     * 규칙:
     *  - 기본: weightKg × 30 ml
     *  - BMI > 25(과체중 이상): weightKg × 35 ml
     *  - 결과를 [1500, 4000] ml 범위로 강제 (극단값 보정)
     *
     * @param weightKg 체중 (kg), 0 이하이면 최솟값 1500 반환
     * @param bmi      체질량지수, 0 이하이면 기본 배율 사용
     */
    fun calcWaterTarget(weightKg: Float, bmi: Float): Int {
        if (weightKg <= 0f) return 1500
        val multiplier = if (bmi > 25f) 35f else 30f
        return (weightKg * multiplier).toInt().coerceIn(1500, 4000)
    }
}
