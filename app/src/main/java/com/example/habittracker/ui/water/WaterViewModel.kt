// 경로: com/example/habittracker/ui/water/WaterViewModel.kt
package com.example.habittracker.ui.water

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.domain.repository.WaterRepository
import com.example.habittracker.domain.usecase.activity.MarkUserActiveUseCase
import com.example.habittracker.domain.usecase.water.AddWaterLogUseCase
import com.example.habittracker.domain.usecase.water.CheckWaterInterventionNeededUseCase
import com.example.habittracker.domain.usecase.water.GetTodayWaterStatusUseCase
import com.example.habittracker.domain.usecase.water.GetWaterHistoryUseCase
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTodayWaterStatusUseCase: GetTodayWaterStatusUseCase,
    private val checkWaterInterventionNeededUseCase: CheckWaterInterventionNeededUseCase,
    private val addWaterLogUseCase: AddWaterLogUseCase,
    private val getWaterHistoryUseCase: GetWaterHistoryUseCase,
    private val waterRepository: WaterRepository,
    private val markUserActiveUseCase: MarkUserActiveUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaterUiState())
    val uiState: StateFlow<WaterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTodayWaterStatusUseCase()
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { status ->
                    val interventionMessage = runCatching {
                        checkWaterInterventionNeededUseCase().message
                    }.getOrNull()
                    _uiState.update {
                        it.copy(
                            loading = false,
                            todayStatus = status,
                            interventionMessage = interventionMessage,
                        )
                    }
                }
        }
    }

    fun onDrinkButtonClick(amountMl: Int) {
        viewModelScope.launch {
            try {
                addWaterLogUseCase(amountMl = amountMl, source = "manual")
                markUserActiveUseCase(MarkUserActiveUseCase.SOURCE_WATER_LOG)
                WidgetUpdateHelper.updateAllWidgetsSync(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onDeleteLog(id: Long) {
        viewModelScope.launch {
            try {
                waterRepository.deleteLog(id)
                WidgetUpdateHelper.updateAllWidgetsSync(context)
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
