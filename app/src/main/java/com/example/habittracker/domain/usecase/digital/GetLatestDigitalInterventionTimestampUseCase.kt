package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.domain.repository.DigitalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLatestDigitalInterventionTimestampUseCase @Inject constructor(
    private val digitalRepository: DigitalRepository,
) {
    suspend operator fun invoke(appPackage: String): Long? =
        digitalRepository.getLatestInterventionTimestamp(appPackage)
}
