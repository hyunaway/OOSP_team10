// 경로: com/example/habittracker/ui/dashboard/DashboardViewModel.kt
package com.example.habittracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.domain.model.DashboardState
import com.example.habittracker.domain.usecase.dashboard.GetDashboardStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val dashboardState: DashboardState? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardStateUseCase: GetDashboardStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDashboardStateUseCase()
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { state ->
                    _uiState.update { it.copy(loading = false, dashboardState = state) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
