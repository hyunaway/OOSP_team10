// 경로: com/example/habittracker/domain/usecase/water/GetWaterHistoryUseCase.kt
package com.example.habittracker.domain.usecase.water

import com.example.habittracker.domain.model.DailyWaterSummary
import com.example.habittracker.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetWaterHistoryUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
) {
    operator fun invoke(startDate: String, endDate: String): Flow<List<DailyWaterSummary>> =
        waterRepository.getLogsBetween(startDate, endDate)
}
