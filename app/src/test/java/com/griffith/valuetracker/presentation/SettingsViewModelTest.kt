package com.griffith.valuetracker.presentation

import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun settingsViewModel_startsLoadingBeforeProfileArrives() = runTest {
        val viewModel = SettingsViewModel(SettingsFakeProfileRepository(UserProfile(firstName = "Vlad")))

        val initialState = viewModel.uiState.value

        assertEquals(true, initialState.isLoading)
        assertEquals("", initialState.firstName)
    }

    @Test
    fun settingsViewModel_updatesEditableFields_andSavesProfile() = runTest {
        val repository = SettingsFakeProfileRepository(
            UserProfile(
                firstName = "Vlad",
                goalType = "Lose Weight",
                targetCalories = 2200,
                targetProtein = 160,
                targetCarbs = 210,
                targetFat = 70,
                heightCm = 180,
                weightKg = 78f,
                age = 30,
            )
        )
        val viewModel = SettingsViewModel(repository)

        advanceUntilIdle()
        viewModel.updateFirstName("Alex")
        viewModel.updateWeightKg("76.5")
        viewModel.updateTargetCalories("2100")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals("Alex", repository.savedProfile?.firstName)
        assertEquals(76.5f, repository.savedProfile?.weightKg)
        assertEquals(2100, repository.savedProfile?.targetCalories)
    }

    @Test
    fun settingsViewModel_exposesProfileTargets() = runTest {
        val profile = UserProfile(
            firstName = "Vlad",
            goalType = "Lose Weight",
            targetCalories = 2200,
            targetProtein = 160,
            targetCarbs = 210,
            targetFat = 70,
        )
        val viewModel = SettingsViewModel(SettingsFakeProfileRepository(profile))

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Vlad", state.firstName)
        assertEquals("Lose Weight", state.goalType)
        assertEquals("2200", state.targetCalories)
    }
}

private class SettingsFakeProfileRepository(
    private val profile: UserProfile,
) : ProfileRepository {
    var savedProfile: UserProfile? = null

    override fun observeUserProfile(): Flow<UserProfile> = flowOf(profile)
    override suspend fun saveUserProfile(profile: UserProfile) {
        savedProfile = profile
    }
}
