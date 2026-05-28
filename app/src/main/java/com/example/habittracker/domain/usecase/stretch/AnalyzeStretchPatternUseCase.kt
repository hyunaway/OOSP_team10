// 경로: com/example/habittracker/domain/usecase/stretch/AnalyzeStretchPatternUseCase.kt
package com.example.habittracker.domain.usecase.stretch

import com.example.habittracker.domain.model.StretchPatternResult
import com.example.habittracker.domain.repository.StretchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzeStretchPatternUseCase @Inject constructor(
    private val stretchRepository: StretchRepository,
) {
    operator fun invoke(): Flow<StretchPatternResult> = stretchRepository.getPatternAnalysis()
}
