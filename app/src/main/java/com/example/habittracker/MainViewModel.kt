// 경로: com/example/habittracker/MainViewModel.kt
package com.example.habittracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferenceManager: UserPreferenceManager,
) : ViewModel() {

    // null = 아직 DataStore에서 값을 읽는 중
    val hasCompletedOnboarding: StateFlow<Boolean?> =
        userPreferenceManager.hasCompletedOnboardingFlow
            .map<Boolean, Boolean?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)
}
