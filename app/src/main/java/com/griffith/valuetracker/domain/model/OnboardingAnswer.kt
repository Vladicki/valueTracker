package com.griffith.valuetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingAnswer(
    val primaryGoal: String = "",
    val currentWeight: String = "",
    val targetWeight: String = "",
    val activityLevel: String = "",
    val dietaryPreferences: List<String> = emptyList()
)
