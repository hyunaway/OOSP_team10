// 경로: com/example/habittracker/domain/usecase/water/AddWaterLogUseCase.kt
package com.example.habittracker.domain.usecase.water

import com.example.habittracker.domain.repository.WaterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddWaterLogUseCase @Inject constructor(
    private val waterRepository: WaterRepository,
) {
    suspend operator fun invoke(
        amountMl: Int,
        source: String,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        waterRepository.addLog(amountMl, source, timestamp)
    }
}
