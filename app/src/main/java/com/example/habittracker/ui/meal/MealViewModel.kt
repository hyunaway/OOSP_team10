package com.example.habittracker.ui.meal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.entity.MealLogEntity
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.model.MealType
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.usecase.meal.AddMealLogUseCase
import com.example.habittracker.domain.usecase.meal.GetMealHistoryUseCase
import com.example.habittracker.domain.usecase.meal.GetTodayMealStatusUseCase
import com.example.habittracker.domain.usecase.meal.MealClassifier
import com.example.habittracker.domain.usecase.meal.MealDailyStatus
import com.example.habittracker.domain.usecase.meal.MealDailyStatusCalculator
import com.example.habittracker.widget.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTodayMealStatusUseCase: GetTodayMealStatusUseCase,
    private val addMealLogUseCase: AddMealLogUseCase,
    private val getMealHistoryUseCase: GetMealHistoryUseCase,
    private val mealRepository: MealRepository,
    private val userPreferenceManager: UserPreferenceManager,
    private val mealClassifier: MealClassifier,
    private val mealDailyStatusCalculator: MealDailyStatusCalculator,
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
            val today = LocalDate.now().toString()
            val previousDate = LocalDate.now().minusDays(1).toString()
            combine(
                mealRepository.observeLogsByMealDate(today),
                mealRepository.observeLogsForMealScreen(today, previousDate),
                userPreferenceManager.wakeTimeFlow,
                userPreferenceManager.bedTimeFlow,
            ) { logs, displayLogs, wakeTime, bedTime ->
                val wakeMinutes = parseTimeToMinutes(wakeTime, DEFAULT_WAKE_TIME_MINUTES)
                val bedMinutes = parseTimeToMinutes(
                    value = bedTime,
                    fallback = DEFAULT_SLEEP_TIME_MINUTES,
                    midnightAsEndOfDay = true,
                )
                val dailyStatus = mealDailyStatusCalculator.calculate(
                    logs = logs,
                    nowMillis = System.currentTimeMillis(),
                    wakeTimeMinutes = wakeMinutes,
                    bedTimeMinutes = bedMinutes,
                )
                MealScreenLogState(logs, displayLogs, dailyStatus, dailyStatus.message)
            }
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { state ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            todayLogs = state.todayLogs,
                            displayLogs = state.displayLogs,
                            dailyMealStatus = state.dailyStatus,
                            dailyStatusMessage = state.message,
                        )
                    }
                }
        }
    }

    fun onAutoMealRecordClick(
        timestamp: Long = System.currentTimeMillis(),
    ) {
        recordMeal(
            requestedMealType = null,
            timestamp = timestamp,
            inputMethod = "",
            triggerType = "",
            viaDeliveryApp = false,
            successPrefix = null,
        )
    }

    @Deprecated("Use onAutoMealRecordClick() for the current automatic meal classification flow.")
    fun onMealButtonClick(
        type: MealType,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        recordMeal(
            requestedMealType = type,
            timestamp = timestamp,
            inputMethod = "",
            triggerType = "",
            viaDeliveryApp = false,
            successPrefix = null,
        )
    }

    fun onLateNightClick(
        inputMethod: String = "manual",
        triggerType: String = "direct",
    ) {
        recordMeal(
            requestedMealType = MealType.LATE_NIGHT,
            timestamp = System.currentTimeMillis(),
            inputMethod = inputMethod,
            triggerType = triggerType,
            viaDeliveryApp = inputMethod == "delivery_app",
            successPrefix = null,
        )
    }

    fun toggleMealLogsExpanded() {
        _uiState.update { it.copy(mealLogsExpanded = !it.mealLogsExpanded) }
    }

    fun onMissedMealTimeSelected(timeText: String) {
        val selectedTime = parseLocalTime(timeText)
        if (selectedTime == null) {
            _uiState.update { it.copy(transientMessage = "시간을 HH:mm 형식으로 입력해주세요.", classificationMessage = null) }
            return
        }

        val selectedDateTime = LocalDateTime.of(LocalDate.now(), selectedTime)
        val timestamp = selectedDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (timestamp > System.currentTimeMillis()) {
            _uiState.update { it.copy(transientMessage = "미래 시간은 기록할 수 없어요.", classificationMessage = null) }
            return
        }

        recordMeal(
            requestedMealType = null,
            timestamp = timestamp,
            inputMethod = "missed_time",
            triggerType = "manual_missed",
            viaDeliveryApp = false,
            successPrefix = "${selectedTime.format(TIME_FORMATTER)} 기록을 추가했어요.",
        )
    }

    fun cancelLatestMealLog() {
        val latestLog = _uiState.value.displayLogs.firstOrNull()
        if (latestLog == null) {
            _uiState.update { it.copy(transientMessage = "취소할 식사 기록이 없어요.", classificationMessage = null) }
            return
        }
        viewModelScope.launch {
            try {
                mealRepository.deleteLog(latestLog.id)
                WidgetUpdateHelper.updateAllWidgetsSync(context)
                _uiState.update {
                    it.copy(
                        transientMessage = "${formatMealTime(latestLog)} ${mealLabel(latestLog)} 기록을 취소했어요.",
                        classificationMessage = null,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onCancelMealClick(id: Long) {
        viewModelScope.launch {
            try {
                mealRepository.deleteLog(id)
                WidgetUpdateHelper.updateAllWidgetsSync(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun recordMeal(
        requestedMealType: MealType?,
        timestamp: Long,
        inputMethod: String,
        triggerType: String,
        viaDeliveryApp: Boolean,
        successPrefix: String?,
    ) {
        viewModelScope.launch {
            try {
                val mealDate = mealClassifier.resolveMealDate(timestamp, requestedMealType)
                val existingLogs = mealRepository.getLogsByMealDate(mealDate)
                val wakeTimeMinutes = parseTimeToMinutes(
                    value = userPreferenceManager.wakeTimeFlow.first(),
                    fallback = DEFAULT_WAKE_TIME_MINUTES,
                )
                val sleepTimeMinutes = parseTimeToMinutes(
                    value = userPreferenceManager.bedTimeFlow.first(),
                    fallback = DEFAULT_SLEEP_TIME_MINUTES,
                    midnightAsEndOfDay = true,
                )
                val classification = mealClassifier.classify(
                    requestedMealType = requestedMealType,
                    timestamp = timestamp,
                    existingLogs = existingLogs,
                    wakeTimeMinutes = wakeTimeMinutes,
                    sleepTimeMinutes = sleepTimeMinutes,
                )
                val localDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()

                addMealLogUseCase(
                    type = classification.mealType,
                    isLateNight = classification.isLateNight,
                    viaDeliveryApp = viaDeliveryApp,
                    source = "manual",
                    timestamp = timestamp,
                    mealDate = classification.mealDate,
                    recordedTime = localDateTime.toLocalTime().toString(),
                    inputMethod = inputMethod,
                    triggerType = triggerType,
                )
                WidgetUpdateHelper.updateAllWidgetsSync(context)

                val message = listOfNotNull(successPrefix, classification.message)
                    .joinToString(" ")
                    .ifBlank { classification.message }
                _uiState.update {
                    it.copy(
                        transientMessage = message,
                        classificationMessage = classification.message,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun parseTimeToMinutes(
        value: String,
        fallback: Int,
        midnightAsEndOfDay: Boolean = false,
    ): Int {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..24 || minute !in 0..59) {
            return fallback
        }
        if (hour == 24 && minute != 0) return fallback
        if (midnightAsEndOfDay && hour == 0 && minute == 0) return MINUTES_PER_DAY
        return hour * 60 + minute
    }

    private fun parseLocalTime(value: String): LocalTime? =
        try {
            LocalTime.parse(value.trim(), TIME_FORMATTER)
        } catch (_: Exception) {
            null
        }

    fun formatMealTime(log: MealLogEntity): String =
        Instant.ofEpochMilli(log.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(TIME_FORMATTER)

    fun mealLabel(log: MealLogEntity): String =
        when {
            (log.isLateNight || log.type == MealType.LATE_NIGHT) && log.mealDate != LocalDate.now().toString() -> "야식 (전날)"
            log.isLateNight || log.type == MealType.LATE_NIGHT -> "야식"
            log.type == MealType.BREAKFAST -> "아침"
            log.type == MealType.LUNCH -> "점심"
            log.type == MealType.DINNER -> "저녁"
            else -> "식사"
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

    fun clearClassificationMessage() {
        _uiState.update { it.copy(classificationMessage = null, transientMessage = null) }
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
        private const val DEFAULT_WAKE_TIME_MINUTES = 8 * 60
        private const val DEFAULT_SLEEP_TIME_MINUTES = 24 * 60
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

private data class MealScreenLogState(
    val todayLogs: List<MealLogEntity>,
    val displayLogs: List<MealLogEntity>,
    val dailyStatus: MealDailyStatus,
    val message: String,
)
