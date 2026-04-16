package com.griffith.valuetracker.presentation

import com.griffith.valuetracker.data.UserPrefsDataStore
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.domain.model.OnboardingAnswer
import com.griffith.valuetracker.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var userPrefsDataStore: UserPrefsDataStore
    private lateinit var profileRepository: ProfileRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userPrefsDataStore = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)
        viewModel = OnboardingViewModel(userPrefsDataStore, profileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has step 0 and empty answers`() {
        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
        assertEquals(OnboardingAnswer(), state.answers)
        assertFalse(state.canProceed)
    }

    @Test
    fun `updatePrimaryGoal updates answers and enables proceed`() {
        viewModel.updatePrimaryGoal("Lose Weight")

        val state = viewModel.uiState.value
        assertEquals("Lose Weight", state.answers.primaryGoal)
        assertTrue(state.canProceed)
    }

    @Test
    fun `updateCurrentWeight updates answers`() {
        viewModel.updateCurrentWeight("75")

        val state = viewModel.uiState.value
        assertEquals("75", state.answers.currentWeight)
    }

    @Test
    fun `updateTargetWeight updates answers`() {
        viewModel.updateTargetWeight("70")

        val state = viewModel.uiState.value
        assertEquals("70", state.answers.targetWeight)
    }

    @Test
    fun `updateActivityLevel updates answers`() {
        viewModel.updateActivityLevel("Moderately Active")

        val state = viewModel.uiState.value
        assertEquals("Moderately Active", state.answers.activityLevel)
    }

    @Test
    fun `toggleDietaryPreference adds preference when not present`() {
        viewModel.toggleDietaryPreference("Vegetarian")

        val state = viewModel.uiState.value
        assertTrue(state.answers.dietaryPreferences.contains("Vegetarian"))
    }

    @Test
    fun `toggleDietaryPreference removes preference when present`() {
        viewModel.toggleDietaryPreference("Vegetarian")
        viewModel.toggleDietaryPreference("Vegetarian")

        val state = viewModel.uiState.value
        assertFalse(state.answers.dietaryPreferences.contains("Vegetarian"))
    }

    @Test
    fun `nextStep advances to next step when can proceed`() {
        viewModel.updatePrimaryGoal("Lose Weight")
        viewModel.nextStep()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentStep)
    }

    @Test
    fun `nextStep does not advance when cannot proceed`() {
        viewModel.nextStep()

        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
    }

    @Test
    fun `previousStep goes back to previous step`() {
        viewModel.updatePrimaryGoal("Lose Weight")
        viewModel.nextStep()
        viewModel.previousStep()

        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
    }

    @Test
    fun `previousStep does not go below 0`() {
        viewModel.previousStep()

        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
    }

    @Test
    fun `completeOnboarding saves answers marks completed and persists sqlite profile`() = runTest {
        coEvery { userPrefsDataStore.saveOnboardingAnswers(any()) } returns Unit
        coEvery { userPrefsDataStore.setOnboardingCompleted(true) } returns Unit
        coEvery { profileRepository.saveUserProfile(any()) } returns Unit

        viewModel.updatePrimaryGoal("Lose Weight")
        viewModel.updateCurrentWeight("75")
        viewModel.completeOnboarding()
        advanceUntilIdle()

        coVerify { userPrefsDataStore.saveOnboardingAnswers(any()) }
        coVerify { userPrefsDataStore.setOnboardingCompleted(true) }
        coVerify {
            profileRepository.saveUserProfile(
                withArg<UserProfile> {
                    assertEquals("Lose Weight", it.goalType)
                    assertEquals(75f, it.weightKg)
                },
            )
        }
    }

    @Test
    fun `canProceed is true for step 0 when goal selected`() {
        viewModel.updatePrimaryGoal("Lose Weight")
        assertTrue(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `canProceed is true for step 1 when weight entered`() {
        viewModel.updatePrimaryGoal("Lose Weight")
        viewModel.nextStep()
        viewModel.updateCurrentWeight("75")
        assertTrue(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `canProceed is true for step 4 when preferences selected`() {
        // Navigate to step 4
        viewModel.updatePrimaryGoal("Lose Weight")
        viewModel.nextStep()
        viewModel.updateCurrentWeight("75")
        viewModel.nextStep()
        viewModel.updateTargetWeight("70")
        viewModel.nextStep()
        viewModel.updateActivityLevel("Moderately Active")
        viewModel.nextStep()

        viewModel.toggleDietaryPreference("Vegetarian")
        assertTrue(viewModel.uiState.value.canProceed)
    }
}
