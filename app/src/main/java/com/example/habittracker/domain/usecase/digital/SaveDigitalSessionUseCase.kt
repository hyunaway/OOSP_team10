// 경로: com/example/habittracker/domain/usecase/digital/SaveDigitalSessionUseCase.kt
package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.domain.repository.DigitalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveDigitalSessionUseCase @Inject constructor(
    private val digitalRepository: DigitalRepository,
) {
    suspend operator fun invoke(
        appPackage: String,
        startTime: Long,
        endTime: Long,
        durationMinutes: Int,
    ) {
        digitalRepository.saveSession(appPackage, startTime, endTime, durationMinutes)
    }
}
