package com.griffith.valuetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Meal(
    val id: Long = 0,
    val title: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val mealType: String,
    val loggedAtEpochMillis: Long,
    val imageUrl: String? = null,
)
