// 경로: com/example/habittracker/ui/onboarding/OnboardingViewModel.kt
package com.example.habittracker.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.ui.avatar.AvatarGender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
) : ViewModel() {

    var userName by mutableStateOf("")
        private set

    var selectedGender by mutableStateOf(AvatarGender.MALE)
        private set

    fun updateUserName(name: String) {
        userName = name
    }

    fun selectGender(gender: AvatarGender) {
        selectedGender = gender
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            val name = userName.trim().ifEmpty { "나" }
            userPreferenceManager.updateUserName(name)
            userPreferenceManager.updateAvatarGender(selectedGender.name)
            userPreferenceManager.updateHasCompletedOnboarding(true)
            onComplete()
        }
    }
}
