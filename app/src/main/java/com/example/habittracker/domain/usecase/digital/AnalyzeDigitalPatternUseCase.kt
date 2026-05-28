// 경로: com/example/habittracker/domain/usecase/digital/AnalyzeDigitalPatternUseCase.kt
package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.domain.model.DigitalPatternResult
import com.example.habittracker.domain.repository.DigitalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzeDigitalPatternUseCase @Inject constructor(
    private val digitalRepository: DigitalRepository,
) {
    operator fun invoke(): Flow<DigitalPatternResult> = digitalRepository.getPatternAnalysis()
}
