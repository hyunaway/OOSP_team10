package com.example.habittracker.domain.model

enum class RecommendedInterventionActionType {
    WATER,
    STRETCH,
    MEAL,
    DIGITAL_BREAK,
}

data class RecommendedInterventionAction(
    val type: RecommendedInterventionActionType,
    val message: String,
)
