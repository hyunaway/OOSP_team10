package com.example.habittracker

import com.example.habittracker.domain.analysis.AnalysisWindow
import com.example.habittracker.domain.analysis.GateEvaluator
import com.example.habittracker.domain.analysis.GateInput
import com.example.habittracker.domain.analysis.GateThreshold
import com.example.habittracker.domain.analysis.PersonalizationEngine
import com.example.habittracker.domain.model.PeakWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PersonalizationEngineTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 보조 함수
    // ─────────────────────────────────────────────────────────────────────────

    /** 오늘 기준 특정 시각의 타임스탬프 생성 */
    private fun todayAt(hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** 지정 시각을 N번 반복하는 타임스탬프 목록 생성 */
    private fun repeat(n: Int, hour: Int, minute: Int = 0): List<Long> =
        List(n) { todayAt(hour, minute) }

    // ─────────────────────────────────────────────────────────────────────────
    // calcPeakWindow 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun calcPeakWindow_emptyInput_returnsEmptyPeakWindow() {
        val result = PersonalizationEngine.calcPeakWindow(emptyList())
        assertTrue("빈 입력에 EMPTY 반환", result.isEmpty())
        assertEquals(-1, result.centerMinutes)
        assertEquals(0f, result.concentration)
    }

    @Test
    fun calcPeakWindow_singleTimestamp_returnsCenterWithZeroConcentration() {
        val ts = listOf(todayAt(8, 0))
        val result = PersonalizationEngine.calcPeakWindow(ts, binMinutes = 30)
        // 08:00 → bin 16 (index = 8*60/30 = 16), center = 16*30+15 = 495분
        assertEquals(495, result.centerMinutes)
        assertEquals(1.0f, result.concentration, 0.01f)
    }

    @Test
    fun calcPeakWindow_clearPeakAt8am_detectsCorrectly() {
        // 08:00~08:29 구간에 10개, 12:00에 2개 → peak = 08시대
        val timestamps = repeat(10, 8, 15) + repeat(2, 12, 0)
        val result = PersonalizationEngine.calcPeakWindow(timestamps, binMinutes = 30)
        // 08:15 → bin 16, center = 495분 (= 08:15)
        assertTrue(
            "peak는 08시대여야 함. actual center=${result.centerMinutes}",
            result.centerMinutes in 480..509,
        )
        assertTrue("concentration > 0.5", result.concentration > 0.5f)
    }

    @Test
    fun calcPeakWindow_uniformDistribution_lowConcentration() {
        // 하루 전체에 고르게 분포 → concentration 낮음
        val timestamps = (0..23).map { h -> todayAt(h, 0) }
        val result = PersonalizationEngine.calcPeakWindow(timestamps, binMinutes = 60)
        assertTrue(
            "균등 분포 시 concentration < 0.2, actual=${result.concentration}",
            result.concentration < 0.2f,
        )
    }

    @Test
    fun calcPeakWindow_outlierTimestamps_ignoredByFrequency() {
        // 08시에 20개, 이상치 23시에 1개 → 이상치는 빈도 낮아 무시됨
        val timestamps = repeat(20, 8, 0) + repeat(1, 23, 0)
        val result = PersonalizationEngine.calcPeakWindow(timestamps, binMinutes = 30)
        assertTrue(
            "이상치(23시) 무시, peak가 08시대여야 함. actual=${result.centerMinutes}",
            result.centerMinutes in 480..509,
        )
    }

    @Test
    fun calcPeakWindow_rangeIsWithinValidBounds() {
        val timestamps = repeat(10, 8, 0)
        val result = PersonalizationEngine.calcPeakWindow(timestamps)
        assertTrue("rangeStart >= 0", result.rangeStart >= 0)
        assertTrue("rangeEnd <= 1439", result.rangeEnd <= 1439)
        assertTrue("rangeStart <= rangeEnd", result.rangeStart <= result.rangeEnd)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calcAverageTimeOfDay 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun calcAverageTimeOfDay_emptyInput_returnsZeroStat() {
        val result = PersonalizationEngine.calcAverageTimeOfDay(emptyList())
        assertEquals(0f, result.meanMinutes)
        assertEquals(0f, result.stdDevMinutes)
        assertEquals(0, result.sampleSize)
    }

    @Test
    fun calcAverageTimeOfDay_identicalTimes_zeroStdDev() {
        val timestamps = repeat(5, 9, 0)
        val result = PersonalizationEngine.calcAverageTimeOfDay(timestamps)
        assertEquals(540f, result.meanMinutes, 1f) // 09:00 = 540분
        assertEquals(0f, result.stdDevMinutes, 0.01f)
        assertEquals(5, result.sampleSize)
    }

    @Test
    fun calcAverageTimeOfDay_twoExtremeTimes_correctMean() {
        // 00:00(0분), 23:00(1380분) → 평균 690분 = 11:30
        val timestamps = listOf(todayAt(0, 0), todayAt(23, 0))
        val result = PersonalizationEngine.calcAverageTimeOfDay(timestamps)
        assertEquals(690f, result.meanMinutes, 1f)
        assertTrue("stdDev > 0", result.stdDevMinutes > 0f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calcRecurrenceInterval 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun calcRecurrenceInterval_emptyInput_returnsNull() {
        assertNull(PersonalizationEngine.calcRecurrenceInterval(emptyList()))
    }

    @Test
    fun calcRecurrenceInterval_singleDate_returnsNull() {
        assertNull(PersonalizationEngine.calcRecurrenceInterval(listOf("2024-01-01")))
    }

    @Test
    fun calcRecurrenceInterval_sevenDayInterval_returns7() {
        val dates = listOf("2024-01-01", "2024-01-08", "2024-01-15")
        val result = PersonalizationEngine.calcRecurrenceInterval(dates)
        assertNotNull(result)
        assertEquals(7f, result!!, 0.01f)
    }

    @Test
    fun calcRecurrenceInterval_duplicateDates_deduplicated() {
        // 중복 날짜가 있어도 distinct 처리 후 간격 계산
        val dates = listOf("2024-01-01", "2024-01-01", "2024-01-04")
        val result = PersonalizationEngine.calcRecurrenceInterval(dates)
        assertNotNull(result)
        assertEquals(3f, result!!, 0.01f)
    }

    @Test
    fun calcRecurrenceInterval_unsortedDates_sortedBeforeCalc() {
        val dates = listOf("2024-01-08", "2024-01-01", "2024-01-15")
        val result = PersonalizationEngine.calcRecurrenceInterval(dates)
        assertNotNull(result)
        assertEquals(7f, result!!, 0.01f)
    }

    @Test
    fun calcRecurrenceInterval_invalidDateStrings_safelyIgnored() {
        // 유효하지 않은 날짜 문자열은 무시됨
        val dates = listOf("2024-01-01", "not-a-date", "2024-01-08")
        val result = PersonalizationEngine.calcRecurrenceInterval(dates)
        assertNotNull(result)
        assertEquals(7f, result!!, 0.01f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calcMostFrequentSlot 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun calcMostFrequentSlot_emptyInput_returnsNull() {
        assertNull(PersonalizationEngine.calcMostFrequentSlot(emptyList()))
    }

    @Test
    fun calcMostFrequentSlot_clearWinner_returnsIt() {
        val slots = listOf("아침", "아침", "점심", "아침", "저녁")
        assertEquals("아침", PersonalizationEngine.calcMostFrequentSlot(slots))
    }

    @Test
    fun calcMostFrequentSlot_singleSlot_returnsThat() {
        assertEquals("저녁", PersonalizationEngine.calcMostFrequentSlot(listOf("저녁")))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calcWaterTarget 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun calcWaterTarget_normalWeight_uses30Multiplier() {
        // 60kg, BMI=22 → 60*30 = 1800ml
        assertEquals(1800, PersonalizationEngine.calcWaterTarget(60f, 22f))
    }

    @Test
    fun calcWaterTarget_overweight_uses35Multiplier() {
        // 70kg, BMI=26 → 70*35 = 2450ml
        assertEquals(2450, PersonalizationEngine.calcWaterTarget(70f, 26f))
    }

    @Test
    fun calcWaterTarget_zeroWeight_returnsMinimum() {
        assertEquals(1500, PersonalizationEngine.calcWaterTarget(0f, 22f))
    }

    @Test
    fun calcWaterTarget_negativeWeight_returnsMinimum() {
        assertEquals(1500, PersonalizationEngine.calcWaterTarget(-10f, 22f))
    }

    @Test
    fun calcWaterTarget_veryLightPerson_clampedToMin() {
        // 40kg, BMI=18 → 40*30 = 1200 → coerce → 1500
        assertEquals(1500, PersonalizationEngine.calcWaterTarget(40f, 18f))
    }

    @Test
    fun calcWaterTarget_veryHeavyPerson_clampedToMax() {
        // 150kg, BMI=45 → 150*35 = 5250 → coerce → 4000
        assertEquals(4000, PersonalizationEngine.calcWaterTarget(150f, 45f))
    }

    @Test
    fun calcWaterTarget_exactlyBmi25_usesLowerMultiplier() {
        // BMI=25는 과체중 기준 미만 → *30
        assertEquals(1800, PersonalizationEngine.calcWaterTarget(60f, 25f))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GateEvaluator 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun evaluateGate_allConditionsMet_returnsTrue() {
        val input = GateInput(daysObserved = 10, volume = 30, hasRecentRecord = true)
        val threshold = GateThreshold(minDays = 7, minVolume = 20)
        assertTrue(GateEvaluator.evaluateGate(input, threshold))
    }

    @Test
    fun evaluateGate_insufficientDays_returnsFalse() {
        val input = GateInput(daysObserved = 5, volume = 30, hasRecentRecord = true)
        val threshold = GateThreshold(minDays = 7, minVolume = 20)
        assertFalse(GateEvaluator.evaluateGate(input, threshold))
    }

    @Test
    fun evaluateGate_insufficientVolume_returnsFalse() {
        val input = GateInput(daysObserved = 10, volume = 10, hasRecentRecord = true)
        val threshold = GateThreshold(minDays = 7, minVolume = 20)
        assertFalse(GateEvaluator.evaluateGate(input, threshold))
    }

    @Test
    fun evaluateGate_noRecentRecord_returnsFalse() {
        val input = GateInput(daysObserved = 10, volume = 30, hasRecentRecord = false)
        val threshold = GateThreshold(minDays = 7, minVolume = 20)
        assertFalse(GateEvaluator.evaluateGate(input, threshold))
    }

    @Test
    fun evaluateGate_exactlyAtThreshold_returnsTrue() {
        // 경계값: 정확히 임계값과 동일한 경우도 통과
        val input = GateInput(daysObserved = 7, volume = 20, hasRecentRecord = true)
        val threshold = GateThreshold(minDays = 7, minVolume = 20)
        assertTrue(GateEvaluator.evaluateGate(input, threshold))
    }

    @Test
    fun evaluateGate_waterThreshold_passesWithSufficientData() {
        val input = GateInput(daysObserved = 8, volume = 25, hasRecentRecord = true)
        assertTrue(GateEvaluator.evaluateGate(input, com.example.habittracker.domain.analysis.GateThresholds.WATER))
    }

    @Test
    fun evaluateGate_mealThreshold_failsWithLowVolume() {
        val input = GateInput(daysObserved = 8, volume = 3, hasRecentRecord = true)
        assertFalse(GateEvaluator.evaluateGate(input, com.example.habittracker.domain.analysis.GateThresholds.MEAL))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AnalysisWindow 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun analysisWindow_recent30_spanIs30Days() {
        val range = AnalysisWindow.recent(30)
        val diffMs = range.last - range.first
        val diffDays = diffMs / (1000L * 60 * 60 * 24)
        // 29일 차이 (오늘 포함 30일)
        assertTrue("30일 윈도우 범위가 29~30일 차이여야 함. actual=$diffDays", diffDays in 29..30)
    }

    @Test
    fun analysisWindow_startIsBeforeEnd() {
        val range = AnalysisWindow.recent()
        assertTrue(range.first < range.last)
    }

    @Test
    fun analysisWindow_endIsToday() {
        val range = AnalysisWindow.recent()
        // recent()의 end는 오늘 23:59:59.999로 설정됨
        // → 오늘 자정(00:00:00) 이상이고 내일 자정 미만이어야 함
        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrowMidnight = todayMidnight + 24L * 60 * 60 * 1000
        assertTrue("end는 오늘 자정 이상", range.last >= todayMidnight)
        assertTrue("end는 내일 자정 미만", range.last < tomorrowMidnight)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PeakWindow 직렬화 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun peakWindow_toJsonFromJson_roundtrip() {
        val original = PeakWindow(centerMinutes = 480, rangeStart = 420, rangeEnd = 540, concentration = 0.75f)
        val json = original.toJson()
        val restored = PeakWindow.fromJson(json)
        assertNotNull(restored)
        assertEquals(original.centerMinutes, restored!!.centerMinutes)
        assertEquals(original.rangeStart, restored.rangeStart)
        assertEquals(original.rangeEnd, restored.rangeEnd)
        assertEquals(original.concentration, restored.concentration, 0.001f)
    }

    @Test
    fun peakWindow_fromJson_emptyString_returnsNull() {
        assertNull(PeakWindow.fromJson(""))
    }

    @Test
    fun peakWindow_fromJson_malformed_returnsNull() {
        assertNull(PeakWindow.fromJson("invalid-json"))
    }

    @Test
    fun peakWindow_empty_sentinel() {
        assertTrue(PeakWindow.EMPTY.isEmpty())
    }
}
