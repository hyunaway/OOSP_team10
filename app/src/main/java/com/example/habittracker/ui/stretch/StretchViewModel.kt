// 경로: com/example/habittracker/ui/stretch/StretchViewModel.kt
package com.example.habittracker.ui.stretch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.entity.StretchingRecord
import com.example.habittracker.data.model.BodyPartType
import com.example.habittracker.domain.usecase.stretch.GetTodayStretchStatusUseCase
import com.example.habittracker.util.NotificationHelper
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.data.local.UserPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StretchViewModel @Inject constructor(
    private val getTodayStretchStatusUseCase: GetTodayStretchStatusUseCase,
    private val stretchRepository: StretchRepository,
    val userPreferenceManager: UserPreferenceManager,
    private val notificationHelper: NotificationHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StretchUiState())
    val uiState: StateFlow<StretchUiState> = _uiState.asStateFlow()

    // 팝업 및 토스트 상태 변수
    private val _showCancelConfirmPopup = MutableStateFlow<StretchingRecord?>(null)
    val showCancelConfirmPopup: StateFlow<StretchingRecord?> = _showCancelConfirmPopup.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

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
                
                // 오늘 횟수 및 연속 스트릭 계산
                val count = stretchRepository.getTodayStretchCount(date)
                val streakVal = stretchRepository.calculateStreak(date)
                
                // 각 시간대별 버튼 상태 계산 (수정 4 구현)
                val states = mutableMapOf<String, StretchButtonState>()
                val slots = listOf("아침", "점심", "저녁", "기타")
                
                slots.forEach { slot ->
                    val record = stretchRepository.getRecordByTimeSlot(date, slot)
                    val state = when {
                        count >= 4 -> StretchButtonState.DISABLED_COMPLETED
                        record != null -> StretchButtonState.EDITABLE
                        else -> StretchButtonState.INPUTTABLE
                    }
                    states[slot] = state
                }
                
                // 50% 달성 여부 플래그 계산
                val amEnabled = userPreferenceManager.stretchSlotAmEnabledFlow.first()
                val pmEnabled = userPreferenceManager.stretchSlotPmEnabledFlow.first()
                val eveEnabled = userPreferenceManager.stretchSlotEveEnabledFlow.first()
                val nightEnabled = userPreferenceManager.stretchSlotNightEnabledFlow.first()
                val activeSlotsCount = listOf(amEnabled, pmEnabled, eveEnabled, nightEnabled).count { it }
                val isHalfGoalAchievedVal = if (activeSlotsCount > 0) {
                    count >= (activeSlotsCount / 2.0)
                } else {
                    false
                }
                
                _uiState.update { it.copy(
                    loading = false,
                    streak = streakVal,
                    buttonStates = states,
                    todayCount = count,
                    isHalfGoalAchieved = isHalfGoalAchievedVal
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, errorMessage = e.message) }
            }
        }
    }

    // 시간대 버튼 클릭 분기 처리 (원터치 기록 및 4회 제한 정책 적용)
    fun handleTimeSlotTap(timeSlot: String) {
        viewModelScope.launch {
            try {
                val date = java.time.LocalDate.now().toString()
                val record = stretchRepository.getRecordByTimeSlot(date, timeSlot)
                val todayCount = stretchRepository.getTodayStretchCount(date)
                
                if (record != null) {
                    // 이미 완료된 기록 존재 -> 롤백 취소 여부를 묻는 팝업창 활성화
                    _showCancelConfirmPopup.value = record
                } else {
                    if (todayCount < 4) {
                        // 기록 없고 오늘 횟수 < 4 -> 기본 부위("전신")로 즉시 추가 (원터치 기록)
                        addStretchRecord(timeSlot, listOf("전신"))
                    } else {
                        // 기록 없고 오늘 횟수 >= 4 -> 토스트 출력 및 입력 제한
                        _toastMessage.value = "오늘 목표를 달성했어요!"
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // DB 연동 기록 및 삭제
    fun addStretchRecord(timeSlot: String, bodyParts: List<String>) {
        viewModelScope.launch {
            try {
                val date = java.time.LocalDate.now().toString()
                val bodyPartsJson = toJsonBodyParts(bodyParts)
                stretchRepository.insertStretchRecord(date, timeSlot, bodyPartsJson)
                refreshData()

                // 50% 이상 달성 축하 알림 체크
                try {
                    val amEnabled = userPreferenceManager.stretchSlotAmEnabledFlow.first()
                    val pmEnabled = userPreferenceManager.stretchSlotPmEnabledFlow.first()
                    val eveEnabled = userPreferenceManager.stretchSlotEveEnabledFlow.first()
                    val nightEnabled = userPreferenceManager.stretchSlotNightEnabledFlow.first()

                    val activeSlotsCount = listOf(amEnabled, pmEnabled, eveEnabled, nightEnabled).count { it }
                    if (activeSlotsCount > 0) {
                        val status = stretchRepository.getTodayStatus().first()
                        val completedCount = status.slotsLogged.size

                        if (completedCount >= (activeSlotsCount / 2.0)) {
                            notificationHelper.sendStretchReminder(
                                message = "오늘 스트레칭 목표를 달성하셨어요! 몸이 한결 가벼워졌을 거예요 ✨",
                                trigger = "congrats"
                            )
                        }
                    }
                } catch (_: Exception) {
                    // 무시
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
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
                    // 삭제 성공 시 리프레시를 통해 UI 업데이트 및 재계산 유도
                    refreshData()
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

    private fun toJsonBodyParts(parts: List<String>): String {
        return parts.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }
}
