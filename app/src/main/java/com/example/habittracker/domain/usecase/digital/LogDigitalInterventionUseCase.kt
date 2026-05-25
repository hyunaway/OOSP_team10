// 경로: com/example/habittracker/domain/usecase/digital/LogDigitalInterventionUseCase.kt
package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.domain.repository.DigitalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogDigitalInterventionUseCase @Inject constructor(
    private val digitalRepository: DigitalRepository,
) {
    suspend operator fun invoke(
        appPackage: String,
        triggerDuration: Int,
        messageTone: String,
        timestamp: Long = System.currentTimeMillis(),
    ): Long = digitalRepository.logIntervention(appPackage, triggerDuration, messageTone, timestamp)
}
