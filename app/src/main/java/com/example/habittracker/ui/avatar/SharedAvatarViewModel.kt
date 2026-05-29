// 경로: com/example/habittracker/ui/avatar/SharedAvatarViewModel.kt
package com.example.habittracker.ui.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.domain.repository.DigitalRepository
import com.example.habittracker.domain.repository.MealRepository
import com.example.habittracker.domain.repository.StretchRepository
import com.example.habittracker.domain.repository.WaterRepository
import com.example.habittracker.ui.avatar.LateNightAvatarState
import com.example.habittracker.ui.avatar.StretchAvatarState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SharedAvatarViewModel @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
    private val mealRepository: MealRepository,
    private val waterRepository: WaterRepository,
    private val digitalRepository: DigitalRepository,
    private val stretchRepository: StretchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    init {
        // 오늘 실시간 상태 플로우 결합 및 아바타 최종 상태 결정
        viewModelScope.launch {
            val todayHabitsFlow = combine(
                mealRepository.getTodayStatus(),
                waterRepository.getTodayStatus(),
                digitalRepository.getTodayStatus(),
                stretchRepository.getTodayStatus(),
                userPreferenceManager.digitalInterventionBaseDurationFlow
            ) { meal, water, digital, stretch, digitalLimit ->
                AvatarStateResolver.resolve(
                    mealStatus = meal,
                    waterStatus = water,
                    digitalStatus = digital,
                    stretchStatus = stretch,
                    digitalLimitMinutes = digitalLimit
                )
            }

            // 최근 3일 식사 이력을 플로우로 계속 관찰하여 결식/규칙성 여부 판별
            val mealHistoryFlow = mealRepository.getTodayStatus().flatMapLatest {
                val today = LocalDate.now()
                val threeDaysAgo = today.minusDays(2)
                mealRepository.getLogsBetween(threeDaysAgo.toString(), today.toString())
            }.map { summaries ->
                val dateList = listOf(
                    LocalDate.now(),
                    LocalDate.now().minusDays(1),
                    LocalDate.now().minusDays(2)
                ).map { it.toString() }

                val summariesByDate = summaries.associateBy {
                    Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                }

                val breakfastList = dateList.map { summariesByDate[it]?.mealMap?.get("BREAKFAST") ?: false }
                val lunchList = dateList.map { summariesByDate[it]?.mealMap?.get("LUNCH") ?: false }
                val dinnerList = dateList.map { summariesByDate[it]?.mealMap?.get("DINNER") ?: false }

                val skipBreakfast3Days = breakfastList.all { !it }
                val skipLunch3Days = lunchList.all { !it }
                val skipDinner3Days = dinnerList.all { !it }

                val isThreeDaySkip = skipBreakfast3Days || skipLunch3Days || skipDinner3Days
                val allMeals3Days = breakfastList.all { it } && lunchList.all { it } && dinnerList.all { it }

                Pair(isThreeDaySkip, allMeals3Days)
            }

            combine(
                userPreferenceManager.avatarGenderFlow,
                userPreferenceManager.userNameFlow,
                todayHabitsFlow,
                mealHistoryFlow
            ) { genderStr, name, resolved, mealHistory ->
                val gender = AvatarGender.fromString(genderStr)
                val (isThreeDaySkip, allMeals3Days) = mealHistory

                val finalState = when {
                    allMeals3Days -> AvatarState.GOOD
                    isThreeDaySkip -> AvatarState.WARNING
                    else -> resolved.primaryState
                }

                val imageResId = AvatarImageMapper.resolve(gender, finalState)

                AvatarUiState(
                    gender = gender,
                    userName = name.ifEmpty { "나" },
                    primaryState = finalState,
                    activeStates = resolved.activeStates,
                    bubbleMessage = finalState.bubbleMessage,
                    imageResId = imageResId
                )
            }
                .catch { /* 기본값 유지 */ }
                .collect { state -> _uiState.value = state }
        }

        // 야식 아바타 상태 재계산 트리거 (기록 추가/취소 시 자동 호출)
        viewModelScope.launch {
            mealRepository.getTodayStatus().collect {
                val lateNightState = calculateLateNightAvatarState()
                android.util.Log.d("AvatarViewModel", "LateNightAvatarState recalculated: $lateNightState")
            }
        }

        // 스트레칭 아바타 상태 재계산 트리거 (기록 추가/취소 시 자동 호출)
        viewModelScope.launch {
            stretchRepository.getTodayStatus().collect {
                val stretchState = calculateStretchAvatarState()
                android.util.Log.d("AvatarViewModel", "StretchAvatarState recalculated: $stretchState")
                // TODO: 계산된 스트레칭 아바타 상태(stretchState)를 바탕으로 추후 프론트 기획 확정 시 아바타 감정 및 UI 이미지 연결 구현 필요 (1회 -> 0회 감소 시 STIFF 상태 반영)
            }
        }
    }

    // 야식 아바타 상태 계산 함수 (기본 틀만, UI 연결은 TODO 처리)
    private suspend fun calculateLateNightAvatarState(): LateNightAvatarState {
        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(6)

        val summaries = mealRepository.getLogsBetween(sevenDaysAgo.toString(), today.toString()).first()

        val summariesByDate = summaries.associateBy {
            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }

        val todayStr = today.toString()
        val todayLateNight = summariesByDate[todayStr]?.let { it.lateNightCount > 0 } ?: false
        val weeklyLateNightCount = summaries.sumOf { it.lateNightCount }

        // 최근 3일 연속 야식 섭취 여부
        val consecutive3Days = listOf(
            todayStr,
            today.minusDays(1).toString(),
            today.minusDays(2).toString()
        ).all { date ->
            summariesByDate[date]?.let { it.lateNightCount > 0 } ?: false
        }

        val state = when {
            consecutive3Days -> LateNightAvatarState.WARNING_RED
            weeklyLateNightCount >= 3 -> LateNightAvatarState.WARNING_ORANGE
            todayLateNight -> LateNightAvatarState.WARNING_YELLOW
            else -> LateNightAvatarState.NORMAL
        }

        // TODO: 계산된 야식 아바타 상태(state)를 바탕으로 추후 프론트 기획 확정 시 아바타 감정 및 UI 이미지 연결 구현 필요
        return state
    }

    // 스트레칭 아바타 상태 계산 함수 (기본 틀만, UI 연결은 TODO 처리)
    suspend fun calculateStretchAvatarState(): StretchAvatarState {
        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(6)

        val summaries = stretchRepository.getLogsBetween(sevenDaysAgo.toString(), today.toString(), null).first()

        val summariesByDate = summaries.associateBy {
            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }

        val todayStr = today.toString()
        val todayCount = summariesByDate[todayStr]?.count ?: 0
        val activeDaysCount = summaries.count { it.count > 0 }

        // 최근 3일 연속 스트레칭 미실시 여부
        val consecutive3DaysNoStretch = listOf(
            todayStr,
            today.minusDays(1).toString(),
            today.minusDays(2).toString()
        ).all { date ->
            (summariesByDate[date]?.count ?: 0) == 0
        }

        val state = when {
            consecutive3DaysNoStretch -> StretchAvatarState.TENSION
            activeDaysCount >= 5 -> StretchAvatarState.ACTIVE
            todayCount >= 1 -> StretchAvatarState.NORMAL
            else -> StretchAvatarState.STIFF
        }

        // TODO: 계산된 스트레칭 아바타 상태(state)를 바탕으로 추후 프론트 기획 확정 시 아바타 감정 및 UI 이미지 연결 구현 필요
        return state
    }
}
