// 경로: com/example/habittracker/ui/theme/HabitCategory.kt
package com.example.habittracker.ui.theme

import androidx.compose.ui.graphics.Color

enum class HabitCategoryStyle {
    MEAL, WATER, DIGITAL, STRETCH;

    val primaryColor: Color get() = when (this) {
        MEAL    -> MealPrimary
        WATER   -> WaterPrimary
        DIGITAL -> DigitalPrimary
        STRETCH -> StretchPrimary
    }

    val backgroundColor: Color get() = when (this) {
        MEAL    -> MealBackground
        WATER   -> WaterBackground
        DIGITAL -> DigitalBackground
        STRETCH -> StretchBackground
    }

    val surfaceColor: Color get() = when (this) {
        MEAL    -> MealSurface
        WATER   -> WaterSurface
        DIGITAL -> DigitalSurface
        STRETCH -> StretchSurface
    }

    val containerColor: Color get() = when (this) {
        MEAL    -> MealContainer
        WATER   -> WaterContainer
        DIGITAL -> DigitalContainer
        STRETCH -> StretchContainer
    }

    val onPrimaryColor: Color get() = when (this) {
        MEAL    -> MealOnPrimary
        WATER   -> WaterOnPrimary
        DIGITAL -> DigitalOnPrimary
        STRETCH -> StretchOnPrimary
    }

    val label: String get() = when (this) {
        MEAL    -> "식사"
        WATER   -> "물"
        DIGITAL -> "디지털"
        STRETCH -> "스트레칭"
    }

    val emoji: String get() = when (this) {
        MEAL    -> "🍽️"
        WATER   -> "💧"
        DIGITAL -> "📱"
        STRETCH -> "🧘"
    }
}
