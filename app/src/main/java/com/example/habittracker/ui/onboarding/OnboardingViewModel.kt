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

    // 신체 정보
    var heightText by mutableStateOf("")
        private set

    var weightText by mutableStateOf("")
        private set

    val isHeightValid: Boolean
        get() = heightText.toFloatOrNull()?.let { it in HEIGHT_MIN_CM..HEIGHT_MAX_CM } ?: false

    val isWeightValid: Boolean
        get() = weightText.toFloatOrNull()?.let { it in WEIGHT_MIN_KG..WEIGHT_MAX_KG } ?: false

    val isBodyInfoValid: Boolean
        get() = isHeightValid && isWeightValid

    fun updateUserName(name: String) {
        userName = name
    }

    fun selectGender(gender: AvatarGender) {
        selectedGender = gender
    }

    fun updateHeight(value: String) {
        heightText = value
    }

    fun updateWeight(value: String) {
        weightText = value
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            val name = userName.trim().ifEmpty { "나" }
            val height = heightText.toFloatOrNull() ?: 0f
            val weight = weightText.toFloatOrNull() ?: 0f
            userPreferenceManager.saveOnboardingData(
                name = name,
                gender = selectedGender.name,
                heightCm = height,
                weightKg = weight
            )
            onComplete()
        }
    }

    companion object {
        const val HEIGHT_MIN_CM = 120f
        const val HEIGHT_MAX_CM = 250f
        const val WEIGHT_MIN_KG = 30f
        const val WEIGHT_MAX_KG = 250f
    }
}
