// 경로: com/example/habittracker/domain/usecase/digital/UpdateInterventionReactionUseCase.kt
package com.example.habittracker.domain.usecase.digital

import com.example.habittracker.domain.repository.DigitalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateInterventionReactionUseCase @Inject constructor(
    private val digitalRepository: DigitalRepository,
) {
    suspend operator fun invoke(id: Long, reacted: Boolean, actionType: String) {
        digitalRepository.updateInterventionReaction(id, reacted, actionType)
    }
}
