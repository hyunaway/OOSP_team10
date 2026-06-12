// 경로: com/example/habittracker/ui/stretch/StretchViewModel.kt
package com.example.habittracker.ui.stretch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.entity.StretchingRecord
import com.example.habittracker.domain.usecase.stretch.GetTodayStretchStatusUseCase
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.domain.usecase.activity.MarkUserActiveUseCase
import com.example.habittracker.domain.usecase.stretch.CalculatePersonalizedStretchGoalUseCase
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.widget.WidgetUpdateHelper
import com.example.habittracker.widget.StretchTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StretchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTodayStretchStatusUseCase: GetTodayStretchStatusUseCase,
    private val stretchRepository: StretchRepository,
    val userPreferenceManager: UserPreferenceManager,
    private val notificationHelper: NotificationHelper,
    private val markUserActiveUseCase: MarkUserActiveUseCase,
    private val calculatePersonalizedStretchGoalUseCase: CalculatePersonalizedStretchGoalUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StretchUiState())
    val uiState: StateFlow<StretchUiState> = _uiState.asStateFlow()

    // 팝업 및 토스트 상태 변수
    private val _showCancelConfirmPopup = MutableStateFlow<StretchingRecord?>(null)
    val showCancelConfirmPopup: StateFlow<StretchingRecord?> = _showCancelConfirmPopup.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var countdownJob: Job? = null

    init {
        // 기존 상태 관찰 유지
        viewModelScope.launch {
            getTodayStretchStatusUseCase()
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { status ->
                    _uiState.update { it.copy(loading = false, todayStatus = status) }
                }
        }
        // 앱 시작 시 상태 갱신
        refreshData()
    }

    // 7대 DB 함수 및 비즈니스 요건 결합 갱신 메소드
    fun refreshData() {
        viewModelScope.launch {
            try {
                val date = java.time.LocalDate.now().toString()
                
                // 1. 먼저 오늘의 개인화 스트레칭 목표치를 계산합니다.
                val todayActiveStartedAt = userPreferenceManager.todayActiveStartedAtFlow.first()
                val personalizedGoalCount = todayActiveStartedAt
                    ?.let { activeStartedAt ->
                        calculatePersonalizedStretchGoalUseCase(
                            activeStartedAtMillis = activeStartedAt,
                            bedTime = userPreferenceManager.bedTimeFlow.first(),
                        )
                    }
                    ?: 4
                
                // 2. 동적 목표치를 넘겨서 오늘 수행 횟수와 스트릭을 구합니다.
                val count = stretchRepository.getTodayStretchCount(date)
                val streakVal = stretchRepository.calculateStreak(date, personalizedGoalCount)
                
                // 3. 각 시간대별 버튼 상태 계산 (하드코딩 4 -> 동적 목표치 적용)
                val states = mutableMapOf<String, StretchButtonState>()
                val slots = listOf("아침", "점심", "저녁", "기타")
                
                slots.forEach { slot ->
                    val record = stretchRepository.getRecordByTimeSlot(date, slot)
                    val state = when {
                        count >= personalizedGoalCount -> StretchButtonState.DISABLED_COMPLETED
                        record != null -> StretchButtonState.EDITABLE
                        else -> StretchButtonState.INPUTTABLE
                    }
                    states[slot] = state
                }
                
                // 4. 50% 달성 여부 플래그 계산
                val isHalfGoalAchievedVal = count >= (personalizedGoalCount / 2.0)
                
                _uiState.update { it.copy(
                    loading = false,
                    streak = streakVal,
                    buttonStates = states,
                    todayCount = count,
                    personalizedGoalCount = personalizedGoalCount,
                    hasTodayActiveStarted = todayActiveStartedAt != null,
                    isHalfGoalAchieved = isHalfGoalAchievedVal
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, errorMessage = e.message) }
            }
        }
    }

    // 시간대 버튼 클릭 분기 처리 (원터치 기록 및 목표 도달 제한 정책 적용)
    fun handleTimeSlotTap(timeSlot: String) {
        viewModelScope.launch {
            try {
                val date = java.time.LocalDate.now().toString()
                val record = stretchRepository.getRecordByTimeSlot(date, timeSlot)
                val todayCount = stretchRepository.getTodayStretchCount(date)
                val goal = _uiState.value.personalizedGoalCount
                
                if (record != null) {
                    // 이미 완료된 기록 존재 -> 롤백 취소 여부를 묻는 팝업창 활성화
                    _showCancelConfirmPopup.value = record
                } else {
                    if (todayCount < goal) {
                        // 기록 없고 오늘 횟수 < goal -> 즉시 추가 (원터치 기록)
                        addStretchRecord(timeSlot)
                    } else {
                        // 기록 없고 오늘 횟수 >= goal -> 토스트 출력 및 입력 제한
                        _toastMessage.value = "오늘 목표를 달성했어요!"
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // DB 연동 기록 및 삭제
    fun addStretchRecord(timeSlot: String) {
        viewModelScope.launch {
            try {
                saveStretchRecord(timeSlot)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun startStretchCountdown() {
        if (_uiState.value.isStretching) return
        if (isSharedStretchTimerRunning()) {
            _toastMessage.value = "이미 스트레칭 타이머가 진행 중이에요."
            return
        }
        countdownJob?.cancel()
        markSharedStretchTimerStarted()
        _uiState.update {
            it.copy(
                isStretching = true,
                countdownSeconds = STRETCH_COUNTDOWN_SECONDS,
                completionMessage = null,
            )
        }
        countdownJob = viewModelScope.launch {
            for (remaining in STRETCH_COUNTDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(countdownSeconds = remaining) }
                delay(1_000L)
            }
            _uiState.update { it.copy(countdownSeconds = 0) }
            try {
                val saved = saveStretchRecord(resolveCurrentTimeSlot())
                markSharedStretchTimerCompleted()
                _uiState.update {
                    it.copy(
                        isStretching = false,
                        countdownSeconds = STRETCH_COUNTDOWN_SECONDS,
                        completionMessage = if (saved) {
                            "잘했어요! 방금 스트레칭 1회를 완료했어요. 다음 스트레칭은 약 90분 뒤에 추천할게요."
                        } else {
                            null
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStretching = false,
                        countdownSeconds = STRETCH_COUNTDOWN_SECONDS,
                        errorMessage = e.message,
                    )
                }
                clearSharedStretchTimer()
            }
        }
    }

    fun cancelStretchCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        clearSharedStretchTimer()
        _uiState.update {
            it.copy(
                isStretching = false,
                countdownSeconds = STRETCH_COUNTDOWN_SECONDS,
            )
        }
    }

    fun clearCompletionMessage() {
        _uiState.update { it.copy(completionMessage = null) }
    }

    fun deleteStretchRecordBySlot(timeSlot: String) {
        viewModelScope.launch {
            try {
                val date = java.time.LocalDate.now().toString()
                
                // 이미 삭제된 기록을 다시 삭제하려는 경우 방지 로직
                val record = stretchRepository.getRecordByTimeSlot(date, timeSlot)
                if (record == null) {
                    _toastMessage.value = "이미 삭제된 기록입니다."
                    return@launch
                }

                // Room DB에서 물리적 삭제 시도
                val deletedRows = stretchRepository.deleteLogBySlot(date, timeSlot)
                if (deletedRows > 0) {
                    refreshData()
                    WidgetUpdateHelper.updateAllWidgetsSync(context)
                } else {
                    // 삭제 실패 시 에러 핸들링
                    _toastMessage.value = "기록 삭제에 실패했습니다."
                }
            } catch (e: Exception) {
                // DB 삭제 예외 발생 시 에러 핸들링
                _toastMessage.value = "기록 취소 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    fun setShowCancelConfirmPopup(record: StretchingRecord?) {
        _showCancelConfirmPopup.value = record
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun saveStretchRecord(timeSlot: String): Boolean {
        val date = java.time.LocalDate.now().toString()
        val existingRecord = stretchRepository.getRecordByTimeSlot(date, timeSlot)
        if (existingRecord != null) {
            _toastMessage.value = "이미 이 시간대 스트레칭을 기록했어요."
            refreshData()
            WidgetUpdateHelper.updateAllWidgetsSync(context)
            return false
        }
        stretchRepository.insertStretchRecord(date, timeSlot)
        markUserActiveUseCase(MarkUserActiveUseCase.SOURCE_STRETCH_LOG)
        refreshData()
        WidgetUpdateHelper.updateAllWidgetsSync(context)

        // 50% 이상 달성 축하 알림 체크
        try {
            val goal = _uiState.value.personalizedGoalCount
            val status = stretchRepository.getTodayStatus().first()
            val completedCount = status.totalCount

            if (completedCount >= (goal / 2.0)) {
                notificationHelper.sendStretchReminder(
                    message = "오늘 스트레칭 목표를 달성하셨어요! 몸이 한결 가벼워졌을 거예요 ✨",
                    trigger = "congrats"
                )
            }
        } catch (_: Exception) {
            // 무시
        }
        return true
    }

    private fun resolveCurrentTimeSlot(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "아침"
            in 12..16 -> "점심"
            in 17..21 -> "저녁"
            else -> "기타"
        }
    }

    private fun isSharedStretchTimerRunning(): Boolean {
        val prefs = context.getSharedPreferences(StretchTimerService.PREFS_STRETCH_TIMER, Context.MODE_PRIVATE)
        val startedAt = prefs.getLong(StretchTimerService.KEY_TIMER_STARTED_AT, 0L)
        return startedAt > 0L &&
            (System.currentTimeMillis() - startedAt) < StretchTimerService.STRETCH_DURATION_MS
    }

    private fun markSharedStretchTimerStarted() {
        context.getSharedPreferences(StretchTimerService.PREFS_STRETCH_TIMER, Context.MODE_PRIVATE).edit()
            .putLong(StretchTimerService.KEY_TIMER_STARTED_AT, System.currentTimeMillis())
            .remove(StretchTimerService.KEY_TIMER_COMPLETED_AT)
            .apply()
        WidgetUpdateHelper.updateAllWidgetsSync(context)
    }

    private fun markSharedStretchTimerCompleted() {
        context.getSharedPreferences(StretchTimerService.PREFS_STRETCH_TIMER, Context.MODE_PRIVATE).edit()
            .remove(StretchTimerService.KEY_TIMER_STARTED_AT)
            .putLong(StretchTimerService.KEY_TIMER_COMPLETED_AT, System.currentTimeMillis())
            .apply()
    }

    private fun clearSharedStretchTimer() {
        context.getSharedPreferences(StretchTimerService.PREFS_STRETCH_TIMER, Context.MODE_PRIVATE).edit()
            .remove(StretchTimerService.KEY_TIMER_STARTED_AT)
            .apply()
        WidgetUpdateHelper.updateAllWidgetsSync(context)
    }



    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val STRETCH_COUNTDOWN_SECONDS = 60
    }
}
