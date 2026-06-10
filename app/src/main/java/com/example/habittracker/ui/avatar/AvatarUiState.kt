// 경로: com/example/habittracker/ui/avatar/AvatarUiState.kt
package com.example.habittracker.ui.avatar

import androidx.annotation.DrawableRes

data class AvatarUiState(
    val gender: AvatarGender = AvatarGender.MALE,
    val userName: String = "",
    val primaryState: AvatarState = AvatarState.GOOD,
    val activeStates: List<AvatarState> = emptyList(),
    val bubbleMessage: String = AvatarState.GOOD.bubbleMessage,
    @get:DrawableRes val imageResId: Int = 0,
) {
    val hasMultipleIssues: Boolean get() = activeStates.size > 1
}

fun AvatarUiState.forCategory(categoryState: AvatarState): AvatarUiState {
    val isCategoryRisk = when (categoryState) {
        AvatarState.MEAL_LACK ->
            AvatarState.MEAL_LACK in activeStates || primaryState == AvatarState.WARNING
        else -> categoryState in activeStates
    }
    val resolvedState = if (isCategoryRisk) categoryState else AvatarState.GOOD
    return copy(
        primaryState = resolvedState,
        imageResId = AvatarImageMapper.resolve(gender, resolvedState),
        bubbleMessage = resolvedState.bubbleMessage,
    )
}
