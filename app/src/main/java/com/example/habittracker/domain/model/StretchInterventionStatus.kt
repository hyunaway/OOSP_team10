package com.example.habittracker.domain.model

data class StretchInterventionStatus(
    val isNeedStretch: Boolean,
    val message: String,
    val personalizedGoalCount: Int,
    val todayCount: Int,
    val minutesUntilNextRecommended: Int?,
)
