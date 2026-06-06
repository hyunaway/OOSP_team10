package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.domain.usecase.personalization.UpdatePersonalizationParamsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 주 1회 실행되는 개인화 분석 Worker.
 *
 * doWork():
 *  1. UpdatePersonalizationParamsUseCase 호출 (4개 카테고리 분석 + DataStore 갱신)
 *  2. 반환값이 true(= 이번 실행이 "첫 게이트 통과")이면
 *     즉시 1회 재실행 예약 → 신규 사용자의 개인화 반영을 빠르게 처리.
 */
@HiltWorker
class PersonalizationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val updatePersonalizationParamsUseCase: UpdatePersonalizationParamsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val firstGatePass = updatePersonalizationParamsUseCase()
            if (firstGatePass) {
                // 신규 사용자: 첫 게이트 통과 직후 1회 즉시 재분석 예약
                WorkScheduler.scheduleImmediatePersonalization(context)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
