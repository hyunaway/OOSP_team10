// 경로: com/example/habittracker/ui/water/WaterViewModel.kt
package com.example.habittracker.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.domain.repository.WaterRepository
import com.example.habittracker.domain.usecase.water.AddWaterLogUseCase
import com.example.habittracker.domain.usecase.water.GetTodayWaterStatusUseCase
import com.example.habittracker.domain.usecase.water.GetWaterHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val getTodayWaterStatusUseCase: GetTodayWaterStatusUseCase,
    private val addWaterLogUseCase: AddWaterLogUseCase,
    private val getWaterHistoryUseCase: GetWaterHistoryUseCase,
    private val waterRepository: WaterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaterUiState())
    val uiState: StateFlow<WaterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTodayWaterStatusUseCase()
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { status ->
                    _uiState.update { it.copy(loading = false, todayStatus = status) }
                }
        }
    }

    fun onDrinkButtonClick(amountMl: Int) {
        viewModelScope.launch {
            try {
                addWaterLogUseCase(amountMl = amountMl, source = "manual")
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onDeleteLog(id: Long) {
        viewModelScope.launch {
            try {
                waterRepository.deleteLog(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun loadHistory(startDate: String, endDate: String) {
        viewModelScope.launch {
            getWaterHistoryUseCase(startDate, endDate)
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
