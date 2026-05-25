// 경로: com/example/habittracker/ui/avatar/SharedAvatarViewModel.kt
package com.example.habittracker.ui.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedAvatarViewModel @Inject constructor(
    userPreferenceManager: UserPreferenceManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferenceManager.avatarGenderFlow,
                userPreferenceManager.userNameFlow,
            ) { genderStr, name ->
                val gender = AvatarGender.fromString(genderStr)
                val imageResId = AvatarImageMapper.resolve(gender, AvatarState.GOOD)
                AvatarUiState(
                    gender = gender,
                    userName = name.ifEmpty { "나" },
                    primaryState = AvatarState.GOOD,
                    activeStates = emptyList(),
                    bubbleMessage = AvatarState.GOOD.bubbleMessage,
                    imageResId = imageResId,
                )
            }
                .catch { /* 기본값 유지 */ }
                .collect { state -> _uiState.value = state }
        }
    }
}
