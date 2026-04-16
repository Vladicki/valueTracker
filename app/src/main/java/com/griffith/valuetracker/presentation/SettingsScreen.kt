package com.griffith.valuetracker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.domain.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onFirstNameChange = viewModel::updateFirstName,
        onGoalTypeChange = viewModel::updateGoalType,
        onTargetCaloriesChange = viewModel::updateTargetCalories,
        onTargetProteinChange = viewModel::updateTargetProtein,
        onTargetCarbsChange = viewModel::updateTargetCarbs,
        onTargetFatChange = viewModel::updateTargetFat,
        onHeightCmChange = viewModel::updateHeightCm,
        onWeightKgChange = viewModel::updateWeightKg,
        onAgeChange = viewModel::updateAge,
        onSaveClick = viewModel::saveProfile,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onFirstNameChange: (String) -> Unit,
    onGoalTypeChange: (String) -> Unit,
    onTargetCaloriesChange: (String) -> Unit,
    onTargetProteinChange: (String) -> Unit,
    onTargetCarbsChange: (String) -> Unit,
    onTargetFatChange: (String) -> Unit,
    onHeightCmChange: (String) -> Unit,
    onWeightKgChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = if (state.firstName.isBlank()) "Profile" else "${state.firstName}'s profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item { OutlinedTextField(value = state.firstName, onValueChange = onFirstNameChange, label = { Text("First name") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = state.goalType, onValueChange = onGoalTypeChange, label = { Text("Goal type") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = state.weightKg, onValueChange = onWeightKgChange, label = { Text("Current weight") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = state.heightCm, onValueChange = onHeightCmChange, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = state.age, onValueChange = onAgeChange, label = { Text("Age") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = state.targetCalories, onValueChange = onTargetCaloriesChange, label = { Text("Target calories") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = state.targetProtein, onValueChange = onTargetProteinChange, label = { Text("Target protein") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = state.targetCarbs, onValueChange = onTargetCarbsChange, label = { Text("Target carbs") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = state.targetFat, onValueChange = onTargetFatChange, label = { Text("Target fat") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth()) {
                Text("Save profile")
            }
        }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    val goalType: String = "",
    val targetCalories: String = "",
    val targetProtein: String = "",
    val targetCarbs: String = "",
    val targetFat: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val age: String = "",
    val isSaving: Boolean = false,
)

class SettingsViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        profileRepository.observeUserProfile()
            .map { profile ->
                SettingsUiState(
                    isLoading = false,
                    firstName = profile.firstName,
                    goalType = profile.goalType,
                    targetCalories = profile.targetCalories.toString(),
                    targetProtein = profile.targetProtein.toString(),
                    targetCarbs = profile.targetCarbs.toString(),
                    targetFat = profile.targetFat.toString(),
                    heightCm = profile.heightCm.toString(),
                    weightKg = profile.weightKg.toString(),
                    age = profile.age.toString(),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SettingsUiState(isLoading = true),
            ).also { flow ->
                viewModelScope.launch {
                    flow.collect { _uiState.value = it }
                }
            }
    }

    fun updateFirstName(value: String) { _uiState.update { it.copy(firstName = value) } }
    fun updateGoalType(value: String) { _uiState.update { it.copy(goalType = value) } }
    fun updateTargetCalories(value: String) { _uiState.update { it.copy(targetCalories = value) } }
    fun updateTargetProtein(value: String) { _uiState.update { it.copy(targetProtein = value) } }
    fun updateTargetCarbs(value: String) { _uiState.update { it.copy(targetCarbs = value) } }
    fun updateTargetFat(value: String) { _uiState.update { it.copy(targetFat = value) } }
    fun updateHeightCm(value: String) { _uiState.update { it.copy(heightCm = value) } }
    fun updateWeightKg(value: String) { _uiState.update { it.copy(weightKg = value) } }
    fun updateAge(value: String) { _uiState.update { it.copy(age = value) } }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            profileRepository.saveUserProfile(
                UserProfile(
                    firstName = state.firstName,
                    goalType = state.goalType,
                    targetCalories = state.targetCalories.toIntOrNull() ?: 0,
                    targetProtein = state.targetProtein.toIntOrNull() ?: 0,
                    targetCarbs = state.targetCarbs.toIntOrNull() ?: 0,
                    targetFat = state.targetFat.toIntOrNull() ?: 0,
                    heightCm = state.heightCm.toIntOrNull() ?: 0,
                    weightKg = state.weightKg.toFloatOrNull() ?: 0f,
                    age = state.age.toIntOrNull() ?: 0,
                )
            )
        }
    }
}
