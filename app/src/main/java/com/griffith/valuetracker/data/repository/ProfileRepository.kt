package com.griffith.valuetracker.data.repository

import com.griffith.valuetracker.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeUserProfile(): Flow<UserProfile>
    suspend fun saveUserProfile(profile: UserProfile)
}
