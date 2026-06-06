package com.example.habittracker

import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.analysis.DefaultValues
import com.example.habittracker.domain.analysis.PersonalizationResolver
import com.example.habittracker.domain.model.AppProfile
import com.example.habittracker.domain.model.PeakWindow
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PersonalizationResolver 4단계 fallback 단위 테스트.
 *
 * 검증 시나리오:
 *  ① 초기 상태 (ready=false, wakeTime 정상)       → 온보딩 기반 Default
 *  ② 게이트 미통과 (ready=false, wakeTime 오류)    → 순수 상수 Default
 *  ③ peak JSON 깨짐 (ready=true, flow 예외)        → 온보딩 기반 Default (크래시 없음)
 *  ④ concentration < 0.3 (불규칙 패턴)            → 온보딩 기반 Default
 *  ⑤ 정상 통과 (ready=true, concentration≥0.3)   → peak.centerMinutes
 */
class PersonalizationResolverTest {

    private lateinit var prefs: UserPreferenceManager
    private lateinit var resolver: PersonalizationResolver

    @Before
    fun setup() {
        prefs    = mockk(relaxed = true)
        resolver = PersonalizationResolver(prefs)
    }

    // ─── 아침 ①: ready=false, wakeTime=06:00 → onboarding: 6*60+60=420 ────────
    @Test
    fun `resolveBreakfastPeakMinutes - scenario1 - notReady returns onboarding wakeTime+60`() = runBlocking {
        every { prefs.mealPersonalizationReadyFlow } returns flowOf(false)
        every { prefs.getWakeTimeAsMinutes() } returns flowOf(6 * 60)  // 06:00

        val result = resolver.resolveBreakfastPeakMinutes()

        assertEquals(6 * 60 + DefaultValues.BREAKFAST_OFFSET_FROM_WAKE_MINUTES, result) // 420
    }

    // ─── 아침 ②: ready=false, getWakeTimeAsMinutes 예외 → 순수 상수 07:30 ──────
    @Test
    fun `resolveBreakfastPeakMinutes - scenario2 - notReady wakeTime error returns constant`() = runBlocking {
        every { prefs.mealPersonalizationReadyFlow } returns flowOf(false)
        every { prefs.getWakeTimeAsMinutes() } throws RuntimeException("DataStore error")

        val result = resolver.resolveBreakfastPeakMinutes()

        assertEquals(DefaultValues.BREAKFAST_PEAK_MINUTES, result) // 7*60+30 = 450
    }

    // ─── 아침 ③: ready=true, peak flow 수집 시 예외 → 온보딩 기반 (크래시 없음) ──
    @Test
    fun `resolveBreakfastPeakMinutes - scenario3 - jsonBroken noCrash returns onboarding`() = runBlocking {
        every { prefs.mealPersonalizationReadyFlow } returns flowOf(true)
        // flow 수집(first()) 시점에 예외 발생 → safeMealPeak 내부 try-catch가 흡수
        every { prefs.mealBreakfastPeakFlow } returns flow { throw RuntimeException("JSON parse failed") }
        every { prefs.getWakeTimeAsMinutes() } returns flowOf(7 * 60)  // 07:00

        val result = resolver.resolveBreakfastPeakMinutes()

        // 크래시 없이 온보딩 기반 반환: 7*60 + 60 = 480
        assertEquals(7 * 60 + DefaultValues.BREAKFAST_OFFSET_FROM_WAKE_MINUTES, result)
    }

    // ─── 아침 ④: ready=true, peak 있음, concentration=0.1 → 온보딩 fallback ──
    @Test
    fun `resolveBreakfastPeakMinutes - scenario4 - lowConcentration returns onboarding`() = runBlocking {
        every { prefs.mealPersonalizationReadyFlow } returns flowOf(true)
        every { prefs.mealBreakfastPeakFlow } returns flowOf(
            PeakWindow(centerMinutes = 300, rangeStart = 0, rangeEnd = 1439, concentration = 0.1f)
        )
        every { prefs.getWakeTimeAsMinutes() } returns flowOf(6 * 60)

        val result = resolver.resolveBreakfastPeakMinutes()

        // concentration(0.1) < MIN(0.3) → onboarding: 6*60+60 = 420
        assertEquals(6 * 60 + DefaultValues.BREAKFAST_OFFSET_FROM_WAKE_MINUTES, result)
    }

    // ─── 아침 ⑤: 정상 — peak center 반환 ───────────────────────────────────────
    @Test
    fun `resolveBreakfastPeakMinutes - scenario5 - highConcentration returns peakCenter`() = runBlocking {
        every { prefs.mealPersonalizationReadyFlow } returns flowOf(true)
        every { prefs.mealBreakfastPeakFlow } returns flowOf(
            PeakWindow(centerMinutes = 480, rangeStart = 450, rangeEnd = 510, concentration = 0.8f)
        )

        val result = resolver.resolveBreakfastPeakMinutes()

        assertEquals(480, result)
    }

    // ─── 점심 ①: ready=false → 순수 상수 12:00 ──────────────────────────────────
    @Test
    fun `resolveLunchPeakMinutes - notReady returns constant 720`() = runBlocking {
        every { prefs.mealPersonalizationReadyFlow } returns flowOf(false)
        // 점심은 onboarding = constant로 동일하므로
        val result = resolver.resolveLunchPeakMinutes()
        assertEquals(DefaultValues.LUNCH_PEAK_MINUTES, result) // 720
    }

    // ─── 저녁: peak null (데이터 0건) → onboarding bedTime-180 fallback ──────────
    @Test
    fun `resolveDinnerPeakMinutes - peakNull returns bedTimeBased fallback`() = runBlocking {
        every { prefs.mealPersonalizationReadyFlow } returns flowOf(true)
        every { prefs.mealDinnerPeakFlow } returns flowOf(null)
        every { prefs.getBedTimeAsMinutes() } returns flowOf(23 * 60)  // 23:00

        val result = resolver.resolveDinnerPeakMinutes()

        // onboarding: 23*60 - 180 = 1200 = 20:00, ≥ DefaultValues.DINNER_PEAK_MINUTES(18:00)
        assertEquals((23 * 60 - DefaultValues.DINNER_OFFSET_BEFORE_BED_MINUTES), result)
    }

    // ─── 스트레칭 목표: ready=false → DefaultValues.STRETCH_GOAL_COUNT ────────────
    @Test
    fun `resolveStretchGoalCount - notReady returns default 4`() = runBlocking {
        every { prefs.stretchPersonalizationReadyFlow } returns flowOf(false)

        val result = resolver.resolveStretchGoalCount()

        assertEquals(DefaultValues.STRETCH_GOAL_COUNT, result)
    }

    // ─── 스트레칭 목표: ready=true, stored=3 → 3 ──────────────────────────────────
    @Test
    fun `resolveStretchGoalCount - ready stored value 3 returned`() = runBlocking {
        every { prefs.stretchPersonalizationReadyFlow } returns flowOf(true)
        every { prefs.stretchGoalCountFlow } returns flowOf(3)

        val result = resolver.resolveStretchGoalCount()

        assertEquals(3, result)
    }

    // ─── 스트레칭 목표: ready=true, stored=0 (범위 밖) → default ─────────────────
    @Test
    fun `resolveStretchGoalCount - ready stored 0 out of range returns default`() = runBlocking {
        every { prefs.stretchPersonalizationReadyFlow } returns flowOf(true)
        every { prefs.stretchGoalCountFlow } returns flowOf(0)

        val result = resolver.resolveStretchGoalCount()

        assertEquals(DefaultValues.STRETCH_GOAL_COUNT, result)
    }

    // ─── 디지털 임계: ready=false → 30분 default ─────────────────────────────────
    @Test
    fun `resolveDigitalThresholdMinutes - notReady returns default 30`() = runBlocking {
        every { prefs.digitalPersonalizationReadyFlow } returns flowOf(false)

        val result = resolver.resolveDigitalThresholdMinutes("com.foo.bar")

        assertEquals(DefaultValues.DIGITAL_THRESHOLD_MINUTES, result)
    }

    // ─── 디지털 임계: ready=true, 앱 프로필 존재 → 개인화 임계값 ──────────────────
    @Test
    fun `resolveDigitalThresholdMinutes - ready profile found returns personalizedThreshold`() = runBlocking {
        val profiles = listOf(
            AppProfile("com.example.youtube", 24, 30f),
            AppProfile("com.example.tiktok", 12, 15f),
        )
        every { prefs.digitalPersonalizationReadyFlow } returns flowOf(true)
        every { prefs.perAppProfileJsonFlow } returns flowOf(AppProfile.listToJson(profiles))

        val result = resolver.resolveDigitalThresholdMinutes("com.example.youtube")

        assertEquals(24, result)
    }

    // ─── 디지털 임계: ready=true, 해당 앱 없음 → default ─────────────────────────
    @Test
    fun `resolveDigitalThresholdMinutes - ready no matching package returns default`() = runBlocking {
        val profiles = listOf(AppProfile("com.example.youtube", 24, 30f))
        every { prefs.digitalPersonalizationReadyFlow } returns flowOf(true)
        every { prefs.perAppProfileJsonFlow } returns flowOf(AppProfile.listToJson(profiles))

        val result = resolver.resolveDigitalThresholdMinutes("com.unknown.app")

        assertEquals(DefaultValues.DIGITAL_THRESHOLD_MINUTES, result)
    }

    // ─── 기록 0건 안전성: 모든 Flow가 기본값/null → 크래시 없이 Default 반환 ────
    @Test
    fun `allMethods - zeroRecords allFlowsDefault noCrash`() = runBlocking {
        // 모든 ready=false, 모든 flow 기본값
        every { prefs.mealPersonalizationReadyFlow } returns flowOf(false)
        every { prefs.stretchPersonalizationReadyFlow } returns flowOf(false)
        every { prefs.digitalPersonalizationReadyFlow } returns flowOf(false)
        every { prefs.getWakeTimeAsMinutes() } returns flowOf(8 * 60)  // 08:00 wake
        every { prefs.getBedTimeAsMinutes() } returns flowOf(23 * 60) // 23:00 bed

        // 모두 크래시 없이 DefaultValues 범위 안의 값 반환
        val breakfast = resolver.resolveBreakfastPeakMinutes()
        val lunch     = resolver.resolveLunchPeakMinutes()
        val dinner    = resolver.resolveDinnerPeakMinutes()
        val stretch   = resolver.resolveStretchGoalCount()
        val digital   = resolver.resolveDigitalThresholdMinutes("com.any.app")

        assertTrue("breakfast in range",  breakfast in 0..23 * 60 + 59)
        assertTrue("lunch in range",      lunch     in 0..23 * 60 + 59)
        assertTrue("dinner in range",     dinner    in 0..23 * 60 + 59)
        assertTrue("stretch in 1..6",     stretch   in 1..6)
        assertTrue("digital >= 1",        digital   >= 1)
    }
}
