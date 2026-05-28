// 경로: com/example/habittracker/domain/usecase/stretch/AddStretchLogUseCase.kt
package com.example.habittracker.domain.usecase.stretch

import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.domain.repository.StretchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddStretchLogUseCase @Inject constructor(
    private val stretchRepository: StretchRepository,
) {
    suspend operator fun invoke(
        bodyPart: BodyPartType,
        durationSeconds: Int,
        source: String,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        stretchRepository.addLog(bodyPart, durationSeconds, source, timestamp)
    }
}
