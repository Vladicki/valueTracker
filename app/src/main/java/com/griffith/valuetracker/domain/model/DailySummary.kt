package com.griffith.valuetracker.domain.model

data class DailySummary(
    val consumedCalories: Int,
    val targetCalories: Int,
    val consumedProteinGrams: Int,
    val targetProteinGrams: Int,
    val consumedCarbsGrams: Int,
    val targetCarbsGrams: Int,
    val consumedFatGrams: Int,
    val targetFatGrams: Int,
    val proteinProgress: Float,
    val carbsProgress: Float,
    val fatProgress: Float,
    val waterMl: Int,
)
