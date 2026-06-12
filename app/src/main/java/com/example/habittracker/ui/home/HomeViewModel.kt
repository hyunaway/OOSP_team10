// 경로: com/example/habittracker/ui/home/HomeViewModel.kt
package com.example.habittracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.analysis.PersonalizationResolver
import com.example.habittracker.domain.model.DashboardState
import com.example.habittracker.domain.model.StretchInterventionStatus
import com.example.habittracker.domain.model.WaterInterventionStatus
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.usecase.dashboard.GetDashboardStateUseCase
import com.example.habittracker.domain.usecase.meal.GetCurrentMealInterventionStatusUseCase
import com.example.habittracker.domain.usecase.water.CheckWaterInterventionNeededUseCase
import com.example.habittracker.domain.usecase.stretch.CheckStretchInterventionNeededUseCase
import com.example.habittracker.ui.avatar.AvatarGender
import com.example.habittracker.ui.avatar.AvatarImageMapper
import com.example.habittracker.ui.avatar.AvatarStateResolver
import com.example.habittracker.ui.avatar.AvatarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val dashboardState: DashboardState? = null,
    val waterInterventionStatus: WaterInterventionStatus? = null,
    val stretchInterventionStatus: StretchInterventionStatus? = null,
    val avatarUiState: AvatarUiState = AvatarUiState(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getDashboardStateUseCase: GetDashboardStateUseCase,
    userPreferenceManager: UserPreferenceManager,
    private val mealRepository: MealRepository,
    private val getCurrentMealInterventionStatusUseCase: GetCurrentMealInterventionStatusUseCase,
    private val checkWaterInterventionNeededUseCase: CheckWaterInterventionNeededUseCase,
    private val checkStretchInterventionNeededUseCase: CheckStretchInterventionNeededUseCase,
    private val personalizationResolver: PersonalizationResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val mealHistoryFlow = mealRepository.getTodayStatus().flatMapLatest {
                val today = LocalDate.now()
                val threeDaysAgo = today.minusDays(2)
                mealRepository.getLogsBetween(threeDaysAgo.toString(), today.toString())
            }.map { summaries ->
                val dateList = listOf(
                    LocalDate.now(),
                    LocalDate.now().minusDays(1),
                    LocalDate.now().minusDays(2)
                ).map { it.toString() }

                val summariesByDate = summaries.associateBy {
                    Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                }

                val breakfastList = dateList.map { summariesByDate[it]?.mealMap?.get("BREAKFAST") ?: false }
                val lunchList = dateList.map { summariesByDate[it]?.mealMap?.get("LUNCH") ?: false }
                val dinnerList = dateList.map { summariesByDate[it]?.mealMap?.get("DINNER") ?: false }

                val skipBreakfast3Days = breakfastList.all { !it }
                val skipLunch3Days = lunchList.all { !it }
                val skipDinner3Days = dinnerList.all { !it }

                val isThreeDaySkip = skipBreakfast3Days || skipLunch3Days || skipDinner3Days
                val allMeals3Days = breakfastList.all { it } && lunchList.all { it } && dinnerList.all { it }

                Pair(isThreeDaySkip, allMeals3Days)
            }

            combine(
                getDashboardStateUseCase(),
                userPreferenceManager.avatarGenderFlow,
                userPreferenceManager.userNameFlow,
                userPreferenceManager.categoryPriorityOrderFlow,
                mealHistoryFlow,
                userPreferenceManager.digitalInterventionBaseDurationFlow
            ) { flowsArray ->
                val dashState = flowsArray[0] as DashboardState
                val genderStr = flowsArray[1] as String
                val name = flowsArray[2] as String
                val priorityOrderStr = flowsArray[3] as String
                @Suppress("UNCHECKED_CAST")
                val mealHistory = flowsArray[4] as Pair<Boolean, Boolean>
                val digitalLimit = flowsArray[5] as Int

                val gender = AvatarGender.fromString(genderStr)
                val mealInterventionStatus = getCurrentMealInterventionStatusUseCase()
                val waterInterventionStatus = checkWaterInterventionNeededUseCase()
                val stretchInterventionStatus = checkStretchInterventionNeededUseCase()

                val priorityOrder = priorityOrderStr.split(",")
                    .map { it.trim().uppercase() }
                    .filter { it.isNotBlank() }
                val (isThreeDaySkip, allMeals3Days) = mealHistory

                val resolveResult = AvatarStateResolver.resolve(
                    mealIntervention = mealInterventionStatus,
                    waterIntervention = waterInterventionStatus,
                    digitalStatus = dashState.digitalStatus,
                    stretchIntervention = stretchInterventionStatus,
                    priorityOrder = priorityOrder,
                    digitalLimitMinutes = digitalLimit,
                    isThreeDaySkip = isThreeDaySkip,
                    allMeals3Days = allMeals3Days
                )
                val imageResId = AvatarImageMapper.resolve(gender, resolveResult.primaryState)
                HomeUiState(
                    loading = false,
                    dashboardState = dashState,
                    waterInterventionStatus = waterInterventionStatus,
                    stretchInterventionStatus = stretchInterventionStatus,
                    avatarUiState = AvatarUiState(
                        gender = gender,
                        userName = name.ifEmpty { "나" },
                        primaryState = resolveResult.primaryState,
                        activeStates = resolveResult.activeStates,
                        bubbleMessage = AvatarStateResolver.bubbleMessageFor(
                            primaryState = resolveResult.primaryState,
                            mealInterventionStatus = mealInterventionStatus,
                        ),
                        imageResId = imageResId,
                    ),
                )
            }
                .flowOn(Dispatchers.Default)
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { state -> _uiState.value = state }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
