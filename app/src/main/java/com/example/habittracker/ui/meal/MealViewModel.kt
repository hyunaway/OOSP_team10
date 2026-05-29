// 경로: com/example/habittracker/ui/meal/MealViewModel.kt
package com.example.habittracker.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.repository.MealRepository
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
    private val mealRepository: MealRepository,
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
        viewModelScope.launch {
            mealRepository.getTodayLogs()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { logs ->
                    _uiState.update { it.copy(todayLogs = logs) }
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
                    mealDate = java.time.LocalDate.now().toString(),
                    recordedTime = java.time.LocalTime.now().toString(),
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onCancelMealClick(id: Long) {
        viewModelScope.launch {
            try {
                mealRepository.deleteLog(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onLateNightClick(
        inputMethod: String = "manual",
        triggerType: String = "direct"
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val targetMealDate = resolveLateNightMealDate(now)
                addMealLogUseCase(
                    type = MealType.LATE_NIGHT,
                    isLateNight = true,
                    viaDeliveryApp = inputMethod == "delivery_app",
                    source = "manual",
                    timestamp = now,
                    mealDate = targetMealDate,
                    recordedTime = java.time.LocalTime.now().toString(),
                    inputMethod = inputMethod,
                    triggerType = triggerType,
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun resolveLateNightMealDate(timestamp: Long): String {
        val zonedDateTime = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault())
        val localDateTime = zonedDateTime.toLocalDateTime()
        val hour = localDateTime.hour
        val dayOfWeek = localDateTime.dayOfWeek

        return if (hour in 0..1) { // 00:00 ~ 01:59 (새벽 2시 이전)
            if (dayOfWeek == java.time.DayOfWeek.FRIDAY) {
                // 금요일 자정 ~ 새벽 2시 사이의 야식은 금요일 야식으로 처리
                localDateTime.toLocalDate().toString()
            } else {
                // 다른 요일은 전날로 귀속
                localDateTime.toLocalDate().minusDays(1).toString()
            }
        } else {
            localDateTime.toLocalDate().toString()
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
