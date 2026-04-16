package com.griffith.valuetracker.domain.model

data class WeightEntry(
    val id: Long = 0,
    val weightKg: Float,
    val recordedAtEpochMillis: Long,
)
