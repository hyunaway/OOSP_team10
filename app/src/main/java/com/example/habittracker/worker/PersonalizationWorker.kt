// 경로: com/example/habittracker/worker/PersonalizationWorker.kt
package com.example.habittracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.domain.usecase.personalization.UpdatePersonalizationParamsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PersonalizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updatePersonalizationParamsUseCase: UpdatePersonalizationParamsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            updatePersonalizationParamsUseCase()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
