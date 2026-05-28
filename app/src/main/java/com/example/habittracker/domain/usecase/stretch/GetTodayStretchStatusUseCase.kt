// 경로: com/example/habittracker/domain/usecase/stretch/GetTodayStretchStatusUseCase.kt
package com.example.habittracker.domain.usecase.stretch

import com.example.habittracker.domain.model.StretchTodayStatus
import com.example.habittracker.domain.repository.StretchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTodayStretchStatusUseCase @Inject constructor(
    private val stretchRepository: StretchRepository,
) {
    operator fun invoke(): Flow<StretchTodayStatus> = stretchRepository.getTodayStatus()
}
