package com.griffith.valuetracker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griffith.valuetracker.data.UserPrefsDataStore
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.domain.model.OnboardingAnswer
import com.griffith.valuetracker.domain.model.UserProfile
import com.griffith.valuetracker.ui.components.QuestionScaffold
import com.griffith.valuetracker.ui.components.SelectionRowCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val ctaText = if (uiState.currentStep == uiState.totalSteps - 1) "Get Started" else "Next"
    val title = when (uiState.currentStep) {
        0 -> "What's your primary goal?"
        1 -> "What's your current weight?"
        2 -> "What's your target weight?"
        3 -> "How active are you?"
        4 -> "Any dietary preferences?"
        else -> ""
    }

    QuestionScaffold(
        title = title,
        onCtaClick = if (uiState.currentStep == uiState.totalSteps - 1) {
            {
                viewModel.completeOnboarding()
                onComplete()
            }
        } else {
            viewModel::nextStep
        },
        ctaText = ctaText,
        ctaEnabled = uiState.canProceed,
    ) {
        LinearProgressIndicator(
            progress = { (uiState.currentStep + 1).toFloat() / uiState.totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (uiState.currentStep) {
                0 -> PrimaryGoalStep(
                    selectedGoal = uiState.answers.primaryGoal,
                    onGoalSelected = viewModel::updatePrimaryGoal,
                )

                1 -> CurrentWeightStep(
                    weight = uiState.answers.currentWeight,
                    onWeightChanged = viewModel::updateCurrentWeight,
                )

                2 -> TargetWeightStep(
                    weight = uiState.answers.targetWeight,
                    onWeightChanged = viewModel::updateTargetWeight,
                )

                3 -> ActivityLevelStep(
                    selectedLevel = uiState.answers.activityLevel,
                    onLevelSelected = viewModel::updateActivityLevel,
                )

                4 -> DietaryPreferencesStep(
                    selectedPreferences = uiState.answers.dietaryPreferences,
                    onPreferenceToggled = viewModel::toggleDietaryPreference,
                )
            }
        }
    }
}

@Composable
private fun PrimaryGoalStep(
    selectedGoal: String,
    onGoalSelected: (String) -> Unit,
) {
    val goals = listOf("Lose Weight", "Maintain Weight", "Gain Weight", "Build Muscle")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        goals.forEach { goal ->
            SelectionRowCard(
                title = goal,
                selected = selectedGoal == goal,
                onClick = { onGoalSelected(goal) },
            )
        }
    }
}

@Composable
private fun CurrentWeightStep(
    weight: String,
    onWeightChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = weight,
        onValueChange = onWeightChanged,
        label = { Text("Weight (kg)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun TargetWeightStep(
    weight: String,
    onWeightChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = weight,
        onValueChange = onWeightChanged,
        label = { Text("Weight (kg)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun ActivityLevelStep(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
) {
    val levels = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extremely Active")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        levels.forEach { level ->
            SelectionRowCard(
                title = level,
                selected = selectedLevel == level,
                onClick = { onLevelSelected(level) },
            )
        }
    }
}

@Composable
private fun DietaryPreferencesStep(
    selectedPreferences: List<String>,
    onPreferenceToggled: (String) -> Unit,
) {
    val preferences = listOf("Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free", "Keto", "Paleo", "None")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Select all that apply",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        preferences.forEach { preference ->
            FilterChip(
                selected = selectedPreferences.contains(preference),
                onClick = { onPreferenceToggled(preference) },
                label = { Text(preference) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

data class OnboardingUiState(
    val currentStep: Int = 0,
    val answers: OnboardingAnswer = OnboardingAnswer(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val totalSteps: Int = 5
    val canProceed: Boolean
        get() = when (currentStep) {
            0 -> answers.primaryGoal.isNotBlank()
            1 -> answers.currentWeight.isNotBlank()
            2 -> answers.targetWeight.isNotBlank()
            3 -> answers.activityLevel.isNotBlank()
            4 -> answers.dietaryPreferences.isNotEmpty()
            else -> false
        }
}

class OnboardingViewModel(
    private val userPrefsDataStore: UserPrefsDataStore,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updatePrimaryGoal(goal: String) {
        _uiState.update { state -> state.copy(answers = state.answers.copy(primaryGoal = goal)) }
    }

    fun updateCurrentWeight(weight: String) {
        _uiState.update { state -> state.copy(answers = state.answers.copy(currentWeight = weight)) }
    }

    fun updateTargetWeight(weight: String) {
        _uiState.update { state -> state.copy(answers = state.answers.copy(targetWeight = weight)) }
    }

    fun updateActivityLevel(level: String) {
        _uiState.update { state -> state.copy(answers = state.answers.copy(activityLevel = level)) }
    }

    fun toggleDietaryPreference(preference: String) {
        _uiState.update { state ->
            val currentPrefs = state.answers.dietaryPreferences
            val updatedPrefs = if (currentPrefs.contains(preference)) currentPrefs - preference else currentPrefs + preference
            state.copy(answers = state.answers.copy(dietaryPreferences = updatedPrefs))
        }
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < state.totalSteps - 1 && state.canProceed) state.copy(currentStep = state.currentStep + 1) else state
        }
    }

    fun previousStep() {
        _uiState.update { state -> if (state.currentStep > 0) state.copy(currentStep = state.currentStep - 1) else state }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Onboarding persists answers before the completion flag so the app never skips setup with an empty profile.
                userPrefsDataStore.saveOnboardingAnswers(_uiState.value.answers)
                profileRepository.saveUserProfile(
                    UserProfile(
                        goalType = _uiState.value.answers.primaryGoal,
                        weightKg = _uiState.value.answers.currentWeight.toFloatOrNull() ?: 0f,
                    ),
                )
                userPrefsDataStore.setOnboardingCompleted(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
