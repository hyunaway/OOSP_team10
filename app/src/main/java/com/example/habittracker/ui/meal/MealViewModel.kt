// 경로: com/example/habittracker/ui/meal/MealViewModel.kt
package com.example.habittracker.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.usecase.meal.AddMealLogUseCase
import com.example.habittracker.domain.usecase.meal.GetMealHistoryUseCase
import com.example.habittracker.domain.usecase.meal.GetTodayMealStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    private val getTodayMealStatusUseCase: GetTodayMealStatusUseCase,
    private val addMealLogUseCase: AddMealLogUseCase,
    private val getMealHistoryUseCase: GetMealHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTodayMealStatusUseCase()
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { status ->
                    _uiState.update { it.copy(loading = false, todayStatus = status) }
                }
        }
    }

    fun onMealButtonClick(
        type: MealType,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        viewModelScope.launch {
            try {
                addMealLogUseCase(
                    type = type,
                    isLateNight = false,
                    viaDeliveryApp = false,
                    source = "manual",
                    timestamp = timestamp,
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onLateNightButtonClick(ate: Boolean) {
        if (!ate) return
        viewModelScope.launch {
            try {
                addMealLogUseCase(
                    type = MealType.SNACK,
                    isLateNight = true,
                    viaDeliveryApp = false,
                    source = "manual",
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun loadHistory(startDate: String, endDate: String) {
        viewModelScope.launch {
            getMealHistoryUseCase(startDate, endDate)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(history = list) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
