// 경로: com/example/habittracker/ui/avatar/AvatarImageMapper.kt
package com.example.habittracker.ui.avatar

import androidx.annotation.DrawableRes
import com.example.habittracker.R

object AvatarImageMapper {

    @DrawableRes
    fun resolve(gender: AvatarGender, state: AvatarState): Int = when (gender) {
        AvatarGender.MALE   -> resolveMale(state)
        AvatarGender.FEMALE -> resolveFemale(state)
    }

    @DrawableRes
    private fun resolveMale(state: AvatarState): Int = when (state) {
        AvatarState.GOOD            -> R.drawable.avatar_male_good
        AvatarState.MEAL_LACK       -> R.drawable.avatar_male_meal_lack
        AvatarState.WATER_LACK      -> R.drawable.avatar_male_water_lack
        AvatarState.DIGITAL_OVERUSE -> R.drawable.avatar_male_digital_overuse
        AvatarState.STRETCH_LACK    -> R.drawable.avatar_male_stretch_lack
    }

    @DrawableRes
    private fun resolveFemale(state: AvatarState): Int = when (state) {
        AvatarState.GOOD            -> R.drawable.avatar_female_good
        AvatarState.MEAL_LACK       -> R.drawable.avatar_female_meal_lack
        AvatarState.WATER_LACK      -> R.drawable.avatar_female_water_lack
        AvatarState.DIGITAL_OVERUSE -> R.drawable.avatar_female_digital_overuse
        AvatarState.STRETCH_LACK    -> R.drawable.avatar_female_stretch_lack
    }
}
