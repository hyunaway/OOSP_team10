// 경로: com/example/habittracker/domain/usecase/digital/GetTodayDigitalStatusUseCase.kt
package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.domain.model.DigitalTodayStatus
import com.example.habittracker.domain.repository.DigitalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTodayDigitalStatusUseCase @Inject constructor(
    private val digitalRepository: DigitalRepository,
) {
    operator fun invoke(): Flow<DigitalTodayStatus> = digitalRepository.getTodayStatus()
}
