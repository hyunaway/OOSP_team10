// 경로: com/example/habittracker/ui/settings/SettingsViewModel.kt
package com.example.habittracker.ui.settings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.usage.UsageStatsHelper
import com.example.habittracker.ui.avatar.AvatarGender
import com.example.habittracker.widget.WidgetUpdateHelper
import com.example.habittracker.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.habittracker.data.DebugDataSeeder
import com.example.habittracker.domain.usecase.personalization.UpdatePersonalizationParamsUseCase
import com.example.habittracker.domain.analysis.PersonalizationResolver
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userPreferenceManager: UserPreferenceManager,
    private val usageStatsHelper: UsageStatsHelper,
    private val debugDataSeeder: DebugDataSeeder,
    private val updatePersonalizationParamsUseCase: UpdatePersonalizationParamsUseCase,
    private val personalizationResolver: PersonalizationResolver,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferenceManager.bedTimeFlow,
                userPreferenceManager.wakeTimeFlow,
                userPreferenceManager.waterReminderIntervalMinutesFlow,
                userPreferenceManager.preferredMessageToneFlow,
            ) { bedTime, wakeTime, interval, tone ->
                SettingsUiState(
                    loading = false,
                    bedTime = bedTime,
                    wakeTime = wakeTime,
                    waterReminderIntervalMinutes = interval,
                    preferredMessageTone = tone,
                )
            }
                .combine(userPreferenceManager.avatarGenderFlow) { state, gender ->
                    state.copy(avatarGender = AvatarGender.fromString(gender))
                }
                .combine(userPreferenceManager.userNameFlow) { state, name ->
                    state.copy(userName = name)
                }
                .combine(userPreferenceManager.selectedDigitalPackagesFlow) { state, packages ->
                    state.copy(selectedDigitalPackages = packages)
                }
                .combine(userPreferenceManager.digitalInterventionThresholdMinutesFlow) { state, threshold ->
                    state.copy(digitalInterventionThresholdMinutes = threshold)
                }
                .combine(userPreferenceManager.digitalInterventionCooldownMinutesFlow) { state, cooldown ->
                    state.copy(digitalInterventionCooldownMinutes = cooldown)
                }
                .combine(userPreferenceManager.categoryPriorityOrderFlow) { state, order ->
                    val parsed = order.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    state.copy(categoryPriorityOrder = parsed.ifEmpty { listOf("MEAL", "WATER", "DIGITAL", "STRETCH") })
                }
                .combine(userPreferenceManager.userHeightCmFlow) { state, height ->
                    state.copy(userHeightCm = height)
                }
                .combine(userPreferenceManager.userWeightKgFlow) { state, weight ->
                    state.copy(userWeightKg = weight)
                }
                .combine(userPreferenceManager.waterPersonalizationReadyFlow) { state, _ -> state }
                .combine(userPreferenceManager.mealPersonalizationReadyFlow) { state, _ -> state }
                .combine(userPreferenceManager.stretchPersonalizationReadyFlow) { state, _ -> state }
                .combine(userPreferenceManager.digitalPersonalizationReadyFlow) { state, _ -> state }
                .combine(userPreferenceManager.waterPeakJsonFlow) { state, _ -> state }
                .combine(userPreferenceManager.mealBreakfastPeakFlow) { state, _ -> state }
                .combine(userPreferenceManager.mealLunchPeakFlow) { state, _ -> state }
                .combine(userPreferenceManager.mealDinnerPeakFlow) { state, _ -> state }
                .combine(userPreferenceManager.mealLateNightPeakFlow) { state, _ -> state }
                .combine(userPreferenceManager.stretchGoalCountFlow) { state, _ -> state }
                .combine(userPreferenceManager.stretchPreferredTimeSlotsFlow) { state, _ -> state }
                .combine(userPreferenceManager.perAppProfileJsonFlow) { state, _ -> state }
                .catch { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } }
                .collect { state ->
                    val waterReady = userPreferenceManager.waterPersonalizationReadyFlow.first()
                    val mealReady = userPreferenceManager.mealPersonalizationReadyFlow.first()
                    val stretchReady = userPreferenceManager.stretchPersonalizationReadyFlow.first()
                    val digitalReady = userPreferenceManager.digitalPersonalizationReadyFlow.first()

                    val bPeak = personalizationResolver.resolveBreakfastPeakMinutes()
                    val lPeak = personalizationResolver.resolveLunchPeakMinutes()
                    val dPeak = personalizationResolver.resolveDinnerPeakMinutes()
                    val lnPeak = personalizationResolver.resolveLateNightPeakMinutes()
                    val sGoal = personalizationResolver.resolveStretchGoalCount()
                    val resolvedWaterGoal = personalizationResolver.resolveWaterGoalMl()
                    val resolvedTone = userPreferenceManager.preferredMessageToneFlow.first()

                    val waterInterval = userPreferenceManager.waterReminderIntervalMinutesFlow.first()
                    
                    val waterPeakObj = personalizationResolver.resolveWaterPeakWindow()
                    val waterPeakStr = waterPeakObj?.let {
                        val startH = it.rangeStart / 60
                        val startM = it.rangeStart % 60
                        val endH = it.rangeEnd / 60
                        val endM = it.rangeEnd % 60
                        String.format("%02d:%02d ~ %02d:%02d (신뢰도: %.2f)", startH, startM, endH, endM, it.concentration)
                    } ?: "패턴 없음"

                    val stretchPrefSlot = personalizationResolver.resolveStretchPreferredSlot() ?: "없음"

                    val youtubeThreshold = personalizationResolver.resolveDigitalThresholdMinutes("com.google.android.youtube")
                    var youtubeAvg = 0f
                    try {
                        val json = userPreferenceManager.perAppProfileJsonFlow.first()
                        if (json.isNotBlank()) {
                            com.example.habittracker.domain.model.AppProfile.listFromJson(json)
                                .find { it.packageName == "com.google.android.youtube" }
                                ?.let { youtubeAvg = it.avgSessionMinutes }
                        }
                    } catch (_: Exception) {}

                    fun formatMinutes(minutes: Int): String {
                        val h = minutes / 60
                        val m = minutes % 60
                        return String.format("%02d:%02d", h, m)
                    }

                    val current = _uiState.value
                    _uiState.value = state.copy(
                        notificationPermissionGranted = current.notificationPermissionGranted,
                        usageAccessGranted = current.usageAccessGranted,
                        isWaterReady = waterReady,
                        isMealReady = mealReady,
                        isStretchReady = stretchReady,
                        isDigitalReady = digitalReady,
                        resolvedBreakfastTime = formatMinutes(bPeak),
                        resolvedLunchTime = formatMinutes(lPeak),
                        resolvedDinnerTime = formatMinutes(dPeak),
                        resolvedLateNightTime = formatMinutes(lnPeak),
                        resolvedStretchGoal = sGoal,
                        resolvedWaterGoalMl = resolvedWaterGoal,
                        resolvedWaterInterval = waterInterval,
                        resolvedWaterPeak = waterPeakStr,
                        resolvedStretchPreferredSlot = stretchPrefSlot,
                        resolvedYoutubeThreshold = youtubeThreshold,
                        resolvedYoutubeAvgSession = youtubeAvg,
                        resolvedMessageTone = resolvedTone,
                        debugInfoText = current.debugInfoText
                    )
                    refreshPermissionStates()
                }
        }
        refreshPermissionStates()
    }

    fun updateBedTime(value: String) {
        _uiState.update { it.copy(bedTime = value, isSaved = false) }
    }

    fun updateWakeTime(value: String) {
        _uiState.update { it.copy(wakeTime = value, isSaved = false) }
    }

    fun updateWaterReminderInterval(minutes: Int) {
        _uiState.update { it.copy(waterReminderIntervalMinutes = minutes, isSaved = false) }
    }

    fun updatePreferredMessageTone(tone: String) {
        _uiState.update { it.copy(preferredMessageTone = tone, isSaved = false) }
    }

    fun updateSelectedDigitalPackages(packages: Set<String>) {
        _uiState.update { it.copy(selectedDigitalPackages = packages, isSaved = false) }
        viewModelScope.launch {
            userPreferenceManager.updateSelectedDigitalPackages(packages)
        }
    }

    fun toggleSelectedDigitalPackage(packageName: String) {
        var updatedPackages: Set<String> = emptySet()
        _uiState.update { state ->
            val updated = state.selectedDigitalPackages.toMutableSet().apply {
                if (!add(packageName)) remove(packageName)
            }
            updatedPackages = updated
            state.copy(selectedDigitalPackages = updated, isSaved = false)
        }
        viewModelScope.launch {
            userPreferenceManager.updateSelectedDigitalPackages(updatedPackages)
        }
    }

    fun updateDigitalInterventionThresholdMinutes(minutes: Int) {
        _uiState.update { it.copy(digitalInterventionThresholdMinutes = minutes, isSaved = false) }
    }

    fun updateDigitalInterventionCooldownMinutes(minutes: Int) {
        _uiState.update { it.copy(digitalInterventionCooldownMinutes = minutes, isSaved = false) }
    }

    fun updateAvatarGender(gender: AvatarGender) {
        _uiState.update { it.copy(avatarGender = gender, isSaved = false) }
    }

    fun updateUserName(name: String) {
        _uiState.update { it.copy(userName = name, isSaved = false) }
    }

    fun updateHeight(value: Float) {
        _uiState.update { it.copy(userHeightCm = value, isSaved = false) }
    }

    fun updateWeight(value: Float) {
        _uiState.update { it.copy(userWeightKg = value, isSaved = false) }
    }

    fun moveCategoryPriorityUp(index: Int) {
        if (index <= 0) return
        val order = _uiState.value.categoryPriorityOrder.toMutableList()
        val temp = order[index]; order[index] = order[index - 1]; order[index - 1] = temp
        val newOrder = order.joinToString(",")
        _uiState.update { it.copy(categoryPriorityOrder = order) }
        viewModelScope.launch(Dispatchers.IO) {
            userPreferenceManager.updateCategoryPriorityOrder(newOrder)
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun moveCategoryPriorityDown(index: Int) {
        val order = _uiState.value.categoryPriorityOrder.toMutableList()
        if (index >= order.lastIndex) return
        val temp = order[index]; order[index] = order[index + 1]; order[index + 1] = temp
        val newOrder = order.joinToString(",")
        _uiState.update { it.copy(categoryPriorityOrder = order) }
        viewModelScope.launch(Dispatchers.IO) {
            userPreferenceManager.updateCategoryPriorityOrder(newOrder)
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                userPreferenceManager.updateBedTime(state.bedTime)
                userPreferenceManager.updateWakeTime(state.wakeTime)
                userPreferenceManager.updateWaterReminderIntervalMinutes(state.waterReminderIntervalMinutes)
                userPreferenceManager.updatePreferredMessageTone(state.preferredMessageTone)
                userPreferenceManager.updateAvatarGender(state.avatarGender.name)
                userPreferenceManager.updateUserName(state.userName.trim().ifEmpty { "나" })
                userPreferenceManager.updateSelectedDigitalPackages(state.selectedDigitalPackages)
                userPreferenceManager.updateDigitalInterventionThresholdMinutes(state.digitalInterventionThresholdMinutes)
                userPreferenceManager.updateDigitalInterventionCooldownMinutes(state.digitalInterventionCooldownMinutes)
                if (state.userHeightCm > 0f && state.userWeightKg > 0f) {
                    userPreferenceManager.updateBodyInfo(state.userHeightCm, state.userWeightKg)
                }
                WorkScheduler.rescheduleAll(getApplication(), userPreferenceManager)
                WidgetUpdateHelper.updateAllWidgetsSync(getApplication())
                _uiState.update { it.copy(isSaved = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun refreshPermissionStates() {
        _uiState.update {
            it.copy(
                notificationPermissionGranted = hasNotificationPermission(),
                usageAccessGranted = usageStatsHelper.hasUsageAccess(),
            )
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(notificationPermissionGranted = granted || hasNotificationPermission())
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ── 디버그 기능 ──────────────────────────────────────────────────────────

    fun seedDebugData() {
        viewModelScope.launch {
            _uiState.update { it.copy(debugInfoText = "규칙 가상 데이터 주입 중...") }
            try {
                debugDataSeeder.seedPersonaData()
                // 유튜브 앱을 관리 대상 앱으로 추가
                val currentPackages = userPreferenceManager.selectedDigitalPackagesFlow.first().toMutableSet()
                currentPackages.add("com.google.android.youtube")
                userPreferenceManager.updateSelectedDigitalPackages(currentPackages)
                _uiState.update { it.copy(debugInfoText = "어제(6.4) 기한 규칙 데이터 주입 완료! (유튜브 관리 앱 등록됨)") }
            } catch (e: Exception) {
                _uiState.update { it.copy(debugInfoText = "데이터 주입 실패: ${e.message}") }
            }
        }
    }

    fun seedIrregularDebugData() {
        viewModelScope.launch {
            _uiState.update { it.copy(debugInfoText = "불규칙 가상 데이터 주입 중...") }
            try {
                debugDataSeeder.seedIrregularPersonaData()
                // 유튜브 앱을 관리 대상 앱으로 추가
                val currentPackages = userPreferenceManager.selectedDigitalPackagesFlow.first().toMutableSet()
                currentPackages.add("com.google.android.youtube")
                userPreferenceManager.updateSelectedDigitalPackages(currentPackages)
                _uiState.update { it.copy(debugInfoText = "어제(6.4) 기한 불규칙 데이터 주입 완료! (유튜브 관리 앱 등록됨)") }
            } catch (e: Exception) {
                _uiState.update { it.copy(debugInfoText = "데이터 주입 실패: ${e.message}") }
            }
        }
    }

    fun runPersonalizationAnalysis() {
        viewModelScope.launch {
            _uiState.update { it.copy(debugInfoText = "개인화 분석 실행 중...") }
            try {
                val firstPass = updatePersonalizationParamsUseCase()
                _uiState.update { it.copy(debugInfoText = "분석 완료! (첫 게이트 통과 여부: $firstPass)") }
            } catch (e: Exception) {
                _uiState.update { it.copy(debugInfoText = "분석 실행 실패: ${e.message}") }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(debugInfoText = "데이터 초기화 중...") }
            try {
                debugDataSeeder.clearAllData()
                userPreferenceManager.clearAllPersonalizationData()
                
                _uiState.update {
                    it.copy(
                        debugInfoText = "초기화 완료! (Room DB 비움 & 개인화 리셋)",
                        isWaterReady = false,
                        isMealReady = false,
                        isStretchReady = false,
                        isDigitalReady = false,
                        resolvedBreakfastTime = "00:00",
                        resolvedLunchTime = "00:00",
                        resolvedDinnerTime = "00:00",
                        resolvedLateNightTime = "00:00",
                        resolvedStretchGoal = 0,
                        resolvedWaterGoalMl = 0,
                        resolvedWaterInterval = 0,
                        resolvedWaterPeak = "패턴 없음",
                        resolvedStretchPreferredSlot = "없음",
                        resolvedYoutubeThreshold = 0,
                        resolvedYoutubeAvgSession = 0f,
                        resolvedMessageTone = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(debugInfoText = "초기화 실패: ${e.message}") }
            }
        }
    }
}
