package com.griffith.valuetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val firstName: String = "",
    val goalType: String = "",
    val targetCalories: Int = 2000,
    val targetProtein: Int = 150,
    val targetCarbs: Int = 250,
    val targetFat: Int = 65,
    val heightCm: Int = 175,
    val weightKg: Float = 70f,
    val age: Int = 30,
)
