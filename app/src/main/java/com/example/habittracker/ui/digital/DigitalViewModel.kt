// 경로: com/example/habittracker/ui/digital/DigitalViewModel.kt
package com.example.habittracker.ui.digital

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.domain.usecase.digital.GetTodayDigitalStatusUseCase
import com.example.habittracker.domain.usecase.digital.UpdateInterventionReactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DigitalViewModel @Inject constructor(
    private val getTodayDigitalStatusUseCase: GetTodayDigitalStatusUseCase,
    private val updateInterventionReactionUseCase: UpdateInterventionReactionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalUiState())
    val uiState: StateFlow<DigitalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTodayDigitalStatusUseCase()
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { status ->
                    _uiState.update { it.copy(loading = false, todayStatus = status) }
                }
        }
    }

    fun onInterventionAction(interventionId: Long, actionType: String) {
        viewModelScope.launch {
            try {
                updateInterventionReactionUseCase(
                    id = interventionId,
                    reacted = true,
                    actionType = actionType,
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
