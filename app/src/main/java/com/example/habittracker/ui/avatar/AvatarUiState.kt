// 경로: com/example/habittracker/ui/avatar/AvatarUiState.kt
package com.example.habittracker.ui.avatar

import androidx.annotation.DrawableRes

data class AvatarUiState(
    val gender: AvatarGender = AvatarGender.MALE,
    val userName: String = "",
    val primaryState: AvatarState = AvatarState.GOOD,
    val activeStates: List<AvatarState> = emptyList(),
    val bubbleMessage: String = AvatarState.GOOD.bubbleMessage,
    @DrawableRes val imageResId: Int = 0,
) {
    val hasMultipleIssues: Boolean get() = activeStates.size > 1
}
