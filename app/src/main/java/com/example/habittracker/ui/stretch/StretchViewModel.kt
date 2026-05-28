// 경로: com/example/habittracker/ui/stretch/StretchViewModel.kt
package com.example.habittracker.ui.stretch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.domain.usecase.stretch.AddStretchLogUseCase
import com.example.habittracker.domain.usecase.stretch.GetTodayStretchStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StretchViewModel @Inject constructor(
    private val getTodayStretchStatusUseCase: GetTodayStretchStatusUseCase,
    private val addStretchLogUseCase: AddStretchLogUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StretchUiState())
    val uiState: StateFlow<StretchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTodayStretchStatusUseCase()
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { status ->
                    _uiState.update { it.copy(loading = false, todayStatus = status) }
                }
        }
    }

    fun onStretchButtonClick(bodyPart: BodyPartType) {
        viewModelScope.launch {
            try {
                addStretchLogUseCase(
                    bodyPart = bodyPart,
                    durationSeconds = DEFAULT_DURATION_SECONDS,
                    source = "manual",
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val DEFAULT_DURATION_SECONDS = 300
    }
}
