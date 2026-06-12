// 경로: com/example/habittracker/ui/reports/ReportsViewModel.kt
package com.example.habittracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.model.MonthlyReportState
import com.example.habittracker.domain.model.WeeklyReportState
import com.example.habittracker.domain.usecase.reports.GetMonthlyReportUseCase
import com.example.habittracker.domain.usecase.reports.GetWeeklyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
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
    private val userPreferenceManager: UserPreferenceManager,
) : ViewModel() {

    // 현재 선택된 주차의 월요일 기준 날짜
    private val _currentWeekStart = MutableStateFlow<LocalDate>(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart.asStateFlow()

    // 현재 선택된 월의 1일 기준 날짜
    private val _currentMonthStart = MutableStateFlow<LocalDate>(
        LocalDate.now().withDayOfMonth(1)
    )
    val currentMonthStart: StateFlow<LocalDate> = _currentMonthStart.asStateFlow()

    private val _weeklyState = MutableStateFlow<WeeklyReportState?>(null)
    val weeklyState: StateFlow<WeeklyReportState?> = _weeklyState.asStateFlow()

    private val _monthlyState = MutableStateFlow<MonthlyReportState?>(null)
    val monthlyState: StateFlow<MonthlyReportState?> = _monthlyState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var weeklyJob: Job? = null
    private var monthlyJob: Job? = null

    // ── 개인화 데이터 StateFlow 노출 ───────────────────────────────────────────
    val waterPersonalizationReady = userPreferenceManager.waterPersonalizationReadyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val waterGoalMl = kotlinx.coroutines.flow.combine(
        userPreferenceManager.waterPersonalizationReadyFlow,
        userPreferenceManager.userWeightKgFlow,
        userPreferenceManager.userBmiFlow
    ) { ready, weight, bmi ->
        if (ready && weight > 0f && bmi > 0f) {
            com.example.habittracker.domain.analysis.PersonalizationEngine.calcWaterTarget(weight, bmi)
        } else {
            2000
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)
    
    val mealPersonalizationReady = userPreferenceManager.mealPersonalizationReadyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val stretchPersonalizationReady = userPreferenceManager.stretchPersonalizationReadyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val digitalPersonalizationReady = userPreferenceManager.digitalPersonalizationReadyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val mealBreakfastPeak = userPreferenceManager.mealBreakfastPeakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
    val mealLunchPeak = userPreferenceManager.mealLunchPeakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
    val mealDinnerPeak = userPreferenceManager.mealDinnerPeakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
    val mealLateNightPeak = userPreferenceManager.mealLateNightPeakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val deliveryIntervalDays = userPreferenceManager.deliveryIntervalDaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stretchGoalCount = userPreferenceManager.stretchGoalCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val stretchPreferredTimeSlots = userPreferenceManager.stretchPreferredTimeSlotsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val perAppProfileJson = userPreferenceManager.perAppProfileJsonFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val waterPeakJson = userPreferenceManager.waterPeakJsonFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val preferredMessageTone = userPreferenceManager.preferredMessageToneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EMPATHY")

    val waterReminderIntervalMinutes = userPreferenceManager.waterReminderIntervalMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 180)

    val digitalInterventionThresholdMinutes = userPreferenceManager.digitalInterventionThresholdMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    // ── 기간 이동 기능 ────────────────────────────────────────────────────────

    fun navigateWeek(offset: Int) {
        val nextWeekStart = _currentWeekStart.value.plusWeeks(offset.toLong())
        // 오늘 날짜가 속한 주를 넘어서는 미래 주차는 선택 불가능하도록 방어 코드 추가 가능
        if (nextWeekStart.isAfter(LocalDate.now())) return
        _currentWeekStart.value = nextWeekStart
        loadWeekly()
    }

    fun navigateMonth(offset: Int) {
        val nextMonthStart = _currentMonthStart.value.plusMonths(offset.toLong())
        if (nextMonthStart.isAfter(LocalDate.now().withDayOfMonth(1))) return
        _currentMonthStart.value = nextMonthStart
        loadMonthly()
    }

    fun loadWeekly() {
        val start = _currentWeekStart.value
        val end = start.plusDays(6)
        
        // 데이터 정합성을 위해 오늘보다 미래인 날짜는 오늘로 제한하여 조회 가능
        val today = LocalDate.now()
        val formattedStart = start.format(DATE_FORMATTER)
        val formattedEnd = (if (end.isAfter(today)) today else end).format(DATE_FORMATTER)
        
        loadWeeklyRange(formattedStart, formattedEnd)
    }

    fun loadMonthly() {
        val start = _currentMonthStart.value
        val end = start.with(TemporalAdjusters.lastDayOfMonth())
        
        val today = LocalDate.now()
        val formattedStart = start.format(DATE_FORMATTER)
        val formattedEnd = (if (end.isAfter(today)) today else end).format(DATE_FORMATTER)
        
        loadMonthlyRange(formattedStart, formattedEnd)
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
