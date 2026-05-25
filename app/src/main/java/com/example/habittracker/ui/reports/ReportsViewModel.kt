// 경로: com/example/habittracker/ui/reports/ReportsViewModel.kt
package com.example.habittracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.domain.model.MonthlyReportState
import com.example.habittracker.domain.model.WeeklyReportState
import com.example.habittracker.domain.usecase.reports.GetMonthlyReportUseCase
import com.example.habittracker.domain.usecase.reports.GetWeeklyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getWeeklyReportUseCase: GetWeeklyReportUseCase,
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase,
) : ViewModel() {

    private val _weeklyState = MutableStateFlow<WeeklyReportState?>(null)
    val weeklyState: StateFlow<WeeklyReportState?> = _weeklyState.asStateFlow()

    private val _monthlyState = MutableStateFlow<MonthlyReportState?>(null)
    val monthlyState: StateFlow<MonthlyReportState?> = _monthlyState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var weeklyJob: Job? = null
    private var monthlyJob: Job? = null

    fun loadWeekly() {
        val today = LocalDate.now()
        val startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .format(DATE_FORMATTER)
        val endDate = today.format(DATE_FORMATTER)
        loadWeeklyRange(startDate, endDate)
    }

    fun loadMonthly() {
        val today = LocalDate.now()
        val startDate = today.withDayOfMonth(1).format(DATE_FORMATTER)
        val endDate = today.format(DATE_FORMATTER)
        loadMonthlyRange(startDate, endDate)
    }

    fun loadWeeklyRange(startDate: String, endDate: String) {
        weeklyJob?.cancel()
        weeklyJob = viewModelScope.launch {
            getWeeklyReportUseCase(startDate, endDate)
                .catch { e -> _errorMessage.value = e.message }
                .collect { state -> _weeklyState.value = state }
        }
    }

    fun loadMonthlyRange(startDate: String, endDate: String) {
        monthlyJob?.cancel()
        monthlyJob = viewModelScope.launch {
            getMonthlyReportUseCase(startDate, endDate)
                .catch { e -> _errorMessage.value = e.message }
                .collect { state -> _monthlyState.value = state }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
