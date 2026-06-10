package com.example.habittracker

import com.example.habittracker.data.entity.WaterLogEntity
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.local.room.dao.WaterDao
import com.example.habittracker.domain.analysis.AnalysisWindow
import com.example.habittracker.domain.analysis.GateThresholds
import com.example.habittracker.domain.usecase.water.AnalyzeWaterPatternUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * 게이트 미통과/통과 파이프라인 통합 테스트.
 *
 * [픽스처 헬퍼]:
 *   makeWaterLogs(days, logsPerDay, hour, minute) — N일치 WaterLogEntity 생성
 *
 * [검증 시나리오]:
 *  (A) 게이트 미통과 → DataStore 갱신 함수 미호출(후퇴 없음)
 *  (B) 게이트 통과  → DataStore 갱신 함수 호출됨
 */
class PersonalizationPipelineTest {

    private lateinit var waterDao: WaterDao
    private lateinit var prefs: UserPreferenceManager
    private lateinit var useCase: AnalyzeWaterPatternUseCase

    @Before
    fun setup() {
        waterDao = mockk(relaxed = true)
        prefs    = mockk(relaxed = true)
        every { prefs.waterReminderIntervalMinutesFlow } returns flowOf(180)
        useCase  = AnalyzeWaterPatternUseCase(waterDao, prefs, mockk(relaxed = true))
    }

    // ─── (A) 게이트 미통과: 관찰 3일, 총 9건 (minDays=7·minVolume=20 미달) ──────
    @Test
    fun `gate miss - insufficient data - DataStore not updated`() = runBlocking {
        val logs = makeWaterLogs(days = 3, logsPerDay = 3, hour = 9, minute = 0)
        every { waterDao.getLogsBetween(any(), any()) } returns flowOf(logs)

        useCase()

        // DataStore 갱신 함수가 단 한 번도 호출되지 않아야 함
        coVerify(exactly = 0) { prefs.updateWaterPeakJson(any()) }
        coVerify(exactly = 0) { prefs.updateWaterPersonalizationReady(any()) }
    }

    // ─── (B-1) 게이트 통과: 관찰 10일, 총 30건, 최근 기록 있음 ─────────────────
    @Test
    fun `gate pass - sufficient data - DataStore updated`() = runBlocking {
        val logs = makeWaterLogs(days = 10, logsPerDay = 3, hour = 9, minute = 0)
        every { waterDao.getLogsBetween(any(), any()) } returns flowOf(logs)

        useCase()

        // 게이트 통과 → 갱신 함수 호출
        coVerify(atLeast = 1) { prefs.updateWaterPersonalizationReady(true) }
        coVerify(atLeast = 1) { prefs.updateWaterPeakJson(any()) }
    }

    // ─── (B-2) 게이트 경계값: 정확히 minDays=7, minVolume=20 ────────────────────
    @Test
    fun `gate boundary - exactly minDays and minVolume - passes`() = runBlocking {
        // 7일 × 3건 = 21건 (minVolume=20 이상), distinct days=7 (minDays=7 이상)
        val logs = makeWaterLogs(
            days       = GateThresholds.WATER.minDays,
            logsPerDay = 3,
            hour       = 8,
            minute     = 30,
        )
        every { waterDao.getLogsBetween(any(), any()) } returns flowOf(logs)

        useCase()

        coVerify(atLeast = 1) { prefs.updateWaterPersonalizationReady(true) }
    }

    // ─── (C) 기록 0건: DataStore 갱신 없음, 크래시 없음 ─────────────────────────
    @Test
    fun `zero records - no DataStore update and no crash`() = runBlocking {
        every { waterDao.getLogsBetween(any(), any()) } returns flowOf(emptyList())

        // 크래시 없이 완료
        useCase()

        coVerify(exactly = 0) { prefs.updateWaterPersonalizationReady(any()) }
    }

    // ─── (D) DAO 예외 발생 시에도 파이프라인이 전파하지 않음 ──────────────────
    @Test
    fun `dao exception propagated as expected`() = runBlocking {
        every { waterDao.getLogsBetween(any(), any()) } throws RuntimeException("DB error")

        var threw = false
        try {
            useCase()
        } catch (_: Exception) {
            threw = true
        }
        // UseCase는 예외를 그대로 전파 (Worker가 catch)
        assertTrue("DAO exception propagated", threw)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 픽스처 헬퍼 — N일치 WaterLogEntity 생성
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @param days      생성할 날짜 수 (오늘 ~ days-1일 전)
     * @param logsPerDay 하루당 로그 수
     * @param hour      로그 시각(시)
     * @param minute    로그 시각(분)
     */
    private fun makeWaterLogs(
        days      : Int,
        logsPerDay: Int,
        hour      : Int,
        minute    : Int,
    ): List<WaterLogEntity> {
        val result = mutableListOf<WaterLogEntity>()
        val base   = Calendar.getInstance()
        repeat(days) { dayOffset ->
            base.apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, -dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            repeat(logsPerDay) {
                result.add(WaterLogEntity(timestamp = base.timeInMillis, amountMl = 200, source = "test"))
            }
        }
        return result
    }
}
