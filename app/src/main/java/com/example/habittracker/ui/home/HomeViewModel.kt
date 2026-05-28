// 경로: com/example/habittracker/ui/home/HomeViewModel.kt
package com.example.habittracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.model.DashboardState
import com.example.habittracker.domain.usecase.dashboard.GetDashboardStateUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val dashboardState: DashboardState? = null,
    val avatarUiState: AvatarUiState = AvatarUiState(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getDashboardStateUseCase: GetDashboardStateUseCase,
    userPreferenceManager: UserPreferenceManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getDashboardStateUseCase(),
                userPreferenceManager.avatarGenderFlow,
                userPreferenceManager.userNameFlow,
            ) { dashState, genderStr, name ->
                val gender = AvatarGender.fromString(genderStr)
                val resolveResult = AvatarStateResolver.resolve(
                    mealStatus = dashState.mealStatus,
                    waterStatus = dashState.waterStatus,
                    digitalStatus = dashState.digitalStatus,
                    stretchStatus = dashState.stretchStatus,
                )
                val imageResId = AvatarImageMapper.resolve(gender, resolveResult.primaryState)
                HomeUiState(
                    loading = false,
                    dashboardState = dashState,
                    avatarUiState = AvatarUiState(
                        gender = gender,
                        userName = name.ifEmpty { "나" },
                        primaryState = resolveResult.primaryState,
                        activeStates = resolveResult.activeStates,
                        bubbleMessage = resolveResult.primaryState.bubbleMessage,
                        imageResId = imageResId,
                    ),
                )
            }
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { state -> _uiState.value = state }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
