// 경로: com/example/habittracker/domain/usecase/stretch/GetStretchHistoryUseCase.kt
package com.example.habittracker.domain.usecase.stretch

import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.domain.model.DailyStretchSummary
import com.example.habittracker.domain.repository.StretchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetStretchHistoryUseCase @Inject constructor(
    private val stretchRepository: StretchRepository,
) {
    operator fun invoke(
        startDate: String,
        endDate: String,
        bodyPart: BodyPartType? = null,
    ): Flow<List<DailyStretchSummary>> =
        stretchRepository.getLogsBetween(startDate, endDate, bodyPart)
}
