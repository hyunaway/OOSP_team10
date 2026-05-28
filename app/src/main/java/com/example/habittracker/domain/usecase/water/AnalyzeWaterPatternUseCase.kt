// 경로: com/example/habittracker/domain/usecase/water/AnalyzeWaterPatternUseCase.kt
package com.example.habittracker.domain.usecase.water

import com.example.habittracker.domain.model.WaterPatternResult
import com.example.habittracker.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzeWaterPatternUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
) {
    operator fun invoke(): Flow<WaterPatternResult> = waterRepository.getPatternAnalysis()
}
