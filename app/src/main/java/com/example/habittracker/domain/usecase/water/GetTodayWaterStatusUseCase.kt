// 경로: com/example/habittracker/domain/usecase/water/GetTodayWaterStatusUseCase.kt
package com.example.habittracker.domain.usecase.water

import com.example.habittracker.domain.model.WaterTodayStatus
import com.example.habittracker.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTodayWaterStatusUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
) {
    operator fun invoke(): Flow<WaterTodayStatus> = waterRepository.getTodayStatus()
}
