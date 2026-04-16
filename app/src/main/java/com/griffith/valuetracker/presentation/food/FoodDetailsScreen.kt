package com.griffith.valuetracker.presentation.food

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griffith.valuetracker.data.repository.FoodRepository
import com.griffith.valuetracker.data.repository.LoggedIngredientInput
import com.griffith.valuetracker.data.repository.LoggedMealInput
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.SavedRecipeIngredientInput
import com.griffith.valuetracker.domain.model.CalculatedIngredient
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.MealDetails
import com.griffith.valuetracker.domain.model.SecondaryNutrient
import com.griffith.valuetracker.ui.components.FadeInScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun FoodDetailsScreen(
    state: FoodDetailsUiState,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onServingsModeClick: (PortionMode) -> Unit,
    onServingsStepChange: (Boolean) -> Unit,
    onAddedOilChange: (Int) -> Unit,
    onPickImageCamera: () -> Unit,
    onPickImageGallery: () -> Unit,
    onPictureMenuToggle: (Boolean) -> Unit,
    onSaveRecipe: () -> Unit,
    onConfirmOverrideRecipe: () -> Unit,
    onDismissOverrideDialog: () -> Unit,
    onConfirmPortionPreference: () -> Unit,
    onDismissPortionPreference: () -> Unit,
    onCancelPortionPreference: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    state.error?.let { message ->
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("food_details_error"),
            )
        }
        return
    }

    if (state.showOverrideRecipeDialog) {
        AlertDialog(
            onDismissRequest = onDismissOverrideDialog,
            confirmButton = {
                TextButton(onClick = onConfirmOverrideRecipe) {
                    Text("Override")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissOverrideDialog) {
                    Text("Cancel")
                }
            },
            title = { Text("Recipe already exists") },
            text = { Text("Override existing recipe for this food?") },
        )
    }

    if (state.showPortionPreferenceDialog) {
        AnchoredActionPopup(
            title = "Save default portion?",
            message = "Use this portion as the default for this food later?",
            alignment = Alignment.BottomCenter,
            offset = androidx.compose.ui.unit.IntOffset(0, 0),
            onDismiss = onCancelPortionPreference,
            actions = listOf(
                PopupAction("Yes", onClick = onConfirmPortionPreference),
                PopupAction("No", onClick = onDismissPortionPreference),
            ),
        )
    }

    FadeInScreen(testTag = "food_details_screen_fade") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            TopBar(
                isBookmarked = state.mealDetails?.isBookmarked ?: false,
                onBackClick = onBackClick,
                onBookmarkClick = onBookmarkClick,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
            )

            Spacer(modifier = Modifier.height(24.dp))
            ImageSection(
                imageUrl = state.mealDetails?.imageUrl,
                pictureMenuExpanded = state.pictureMenuExpanded,
                onTogglePictureMenu = { onPictureMenuToggle(!state.pictureMenuExpanded) },
                onDismissPictureMenu = { onPictureMenuToggle(false) },
                onPickImageCamera = {
                    onPictureMenuToggle(false)
                    onPickImageCamera()
                },
                onPickImageGallery = {
                    onPictureMenuToggle(false)
                    onPickImageGallery()
                },
            )
            Spacer(modifier = Modifier.height(24.dp))

            state.mealDetails?.let { meal ->
                Text(
                    text = meal.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(16.dp))

                EnergySection(
                    calories = meal.calculatedCalories,
                    servings = meal.servings,
                    portionMode = state.portionMode,
                    onPortionModeToggle = {
                        val next = if (state.portionMode == PortionMode.Portions) PortionMode.Grams else PortionMode.Portions
                        onServingsModeClick(next)
                    },
                    onPortionScroll = onServingsStepChange,
                )

                Spacer(modifier = Modifier.height(24.dp))

                NutrientBlocks(
                    carbs = meal.calculatedCarbsGrams,
                    fats = meal.calculatedFatGrams,
                    proteins = meal.calculatedProteinGrams,
                )

                if (meal.calculatedSecondaryNutrients.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryNutrientGrid(meal.calculatedSecondaryNutrients)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (meal.ingredients.isNotEmpty()) {
                    IngredientsList(meal.calculatedIngredients)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (meal.hasOil) {
                    AddedOilSlider(
                        addedOilGrams = meal.addedOilGrams,
                        onAddedOilChange = onAddedOilChange,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }

        BottomActions(
            onSaveRecipe = onSaveRecipe,
            onDoneClick = onDoneClick,
            isEditingHistoryMeal = state.isEditingHistoryMeal,
            onRemoveClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 36.dp)
                .testTag("food_details_bottom_actions"),
        )
    }
}

@Composable
private fun TopBar(
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.semantics { contentDescription = "Back" },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        Row {
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier.semantics { contentDescription = "Bookmark" },
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.semantics { contentDescription = "More options" },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )                
                }

                if (showMenu) {
                    AnchoredActionPopup(
                        title = "Meal Options",
                        width = 150.dp,
                        alignment = Alignment.TopEnd,
                        offset = androidx.compose.ui.unit.IntOffset(-16, 72),
                        onDismiss = { showMenu = false },
                        actions = listOf(
                            PopupAction("Edit", onClick = {
                                showMenu = false
                                onEditClick()
                            }),
                            PopupAction("Delete", destructive = true, onClick = {
                                showMenu = false
                                onDeleteClick()
                            }),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageSection(
    imageUrl: String?,
    pictureMenuExpanded: Boolean,
    onTogglePictureMenu: () -> Unit,
    onDismissPictureMenu: () -> Unit,
    onPickImageCamera: () -> Unit,
    onPickImageGallery: () -> Unit,
) {
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .clickable(onClick = onTogglePictureMenu)
                .testTag("food_details_image_placeholder"),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Meal picture",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "Add picture to the Recipe",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (pictureMenuExpanded) {
            AnchoredActionPopup(
                title = "Add picture",
                alignment = Alignment.Center,
                width = 200.dp,
                offset = androidx.compose.ui.unit.IntOffset(0, 450),
                onDismiss = onDismissPictureMenu,
                actions = listOf(
                    PopupAction("Camera", onClick = onPickImageCamera),
                    PopupAction("Gallery", onClick = onPickImageGallery),
                ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnergySection(
    calories: Int,
    servings: Float,
    portionMode: PortionMode,
    onPortionModeToggle: () -> Unit,
    onPortionScroll: (Boolean) -> Unit,
) {
    var dragAccumulator by remember { mutableStateOf(0f) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NutrientBlock(
            label = "kcal",
            value = calories.toString(),
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable(onClick = onPortionModeToggle)
                .pointerInput(portionMode) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            dragAccumulator += dragAmount
                            val threshold = 36f
                            if (dragAccumulator <= -threshold) {
                                dragAccumulator = 0f
                                onPortionScroll(true)
                            } else if (dragAccumulator >= threshold) {
                                dragAccumulator = 0f
                                onPortionScroll(false)
                            }
                        },
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("food_details_portion_control"),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "󰹺",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (portionMode == PortionMode.Grams) {
                        "${(servings * 100).toInt()} g"
                    } else {
                        String.format("%.1f portion", servings)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun NutrientBlocks(
    carbs: Int,
    fats: Int,
    proteins: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NutrientBlock(label = "Proteins", value = "${proteins}g", modifier = Modifier.weight(1f))
        NutrientBlock(label = "Fats", value = "${fats}g", modifier = Modifier.weight(1f))
        NutrientBlock(label = "Carbs", value = "${carbs}g", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SecondaryNutrientGrid(nutrients: List<SecondaryNutrient>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "More nutrients",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        nutrients.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { nutrient ->
                    NutrientBlock(
                        label = nutrient.label,
                        value = nutrient.value,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NutrientBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun IngredientsList(
    ingredients: List<CalculatedIngredient>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Ingredients",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ingredients.forEach { ingredient ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${ingredient.grams}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddedOilSlider(
    addedOilGrams: Int,
    onAddedOilChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Added Oil",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${addedOilGrams}g",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Slider(
            value = addedOilGrams.toFloat(),
            onValueChange = { onAddedOilChange(it.toInt()) },
            valueRange = 1f..50f,
            modifier = Modifier.testTag("food_details_oil_slider"),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
private fun BottomActions(
    onSaveRecipe: () -> Unit,
    onDoneClick: () -> Unit,
    isEditingHistoryMeal: Boolean,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isEditingHistoryMeal) {
            Button(
                onClick = onRemoveClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Remove", fontSize = 16.sp)
            }
            Button(
                onClick = onDoneClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Save", fontSize = 16.sp)
            }
        } else {
            Button(
                onClick = onSaveRecipe,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Save", fontSize = 16.sp)
            }
            Button(
                onClick = onDoneClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Log it", fontSize = 16.sp)
            }
        }
    }
}

data class FoodDetailsUiState(
    val isLoading: Boolean = true,
    val mealDetails: MealDetails? = null,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val isLogged: Boolean = false,
    val isEditingHistoryMeal: Boolean = false,
    val portionMode: PortionMode = PortionMode.Grams,
    val pictureMenuExpanded: Boolean = false,
    val showOverrideRecipeDialog: Boolean = false,
    val showPortionPreferenceDialog: Boolean = false,
)

enum class PortionMode {
    Grams,
    Portions,
}

class FoodDetailsViewModel(
    private val mealId: Long,
    private val isEditingHistoryMeal: Boolean,
    private val initialMeal: Meal?,
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {
    private companion object {
        const val GRAM_STEP = 10f
        const val PORTION_STEP = 0.2f
        const val DEFAULT_PORTION_GRAMS = 100f
    }

    private var hasSavedPortionPreference = false
    private val _uiState = MutableStateFlow(FoodDetailsUiState())
    val uiState: StateFlow<FoodDetailsUiState> = _uiState.asStateFlow()

    init {
        loadMealDetails()
    }

    private fun loadMealDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                if (isEditingHistoryMeal) {
                    nutritionRepository.getHistoryMealDetails(mealId)
                } else {
                    foodRepository.getMealDetails(mealId)
                }
            }.onSuccess { details ->
                val mergedDetails = if (initialMeal != null && !isEditingHistoryMeal) {
                    details.copy(
                        title = initialMeal.title,
                        baseCalories = initialMeal.calories,
                        baseProteinGrams = initialMeal.proteinGrams,
                        baseCarbsGrams = initialMeal.carbsGrams,
                        baseFatGrams = initialMeal.fatGrams,
                        mealType = initialMeal.mealType,
                    )
                } else {
                    details
                }
                val portionPreference = if (!isEditingHistoryMeal) foodRepository.getPortionPreference(mealId) else null
                hasSavedPortionPreference = portionPreference != null
                val basePortion = mergedDetails.mealType.removeSuffix(" g").toFloatOrNull() ?: DEFAULT_PORTION_GRAMS
                val detailsWithPreference = if (portionPreference != null && basePortion > 0f) {
                    mergedDetails.copy(servings = (portionPreference / basePortion).coerceAtLeast(1f))
                } else {
                    mergedDetails
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        mealDetails = detailsWithPreference,
                        error = null,
                        isEditingHistoryMeal = isEditingHistoryMeal,
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun selectPortionMode(mode: PortionMode) {
        _uiState.update { it.copy(portionMode = mode) }
    }

    fun stepPortionSize(increase: Boolean) {
        _uiState.update { state ->
            val meal = state.mealDetails ?: return@update state
            val step = if (state.portionMode == PortionMode.Grams) GRAM_STEP / 100f else PORTION_STEP
            val next = if (increase) meal.servings + step else (meal.servings - step).coerceAtLeast(1f)
            state.copy(mealDetails = meal.copy(servings = next))
        }
    }

    fun updateServings(servings: Float) {
        _uiState.update { state ->
            state.copy(
                mealDetails = state.mealDetails?.copy(servings = servings.coerceAtLeast(1f)),
            )
        }
    }

    fun updateAddedOil(grams: Int) {
        _uiState.update { state ->
            state.copy(
                mealDetails = state.mealDetails?.copy(addedOilGrams = grams),
            )
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val currentDetails = _uiState.value.mealDetails ?: return@launch
            val newBookmarkState = !currentDetails.isBookmarked

            _uiState.update { state ->
                state.copy(
                    mealDetails = currentDetails.copy(isBookmarked = newBookmarkState),
                )
            }

            foodRepository.updateMealBookmark(mealId, newBookmarkState)
        }
    }

    fun setPictureMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(pictureMenuExpanded = expanded) }
    }

    fun setImageUrl(imageUrl: String) {
        _uiState.update { state ->
            state.copy(mealDetails = state.mealDetails?.copy(imageUrl = imageUrl))
        }
    }

    fun saveRecipe() {
        viewModelScope.launch {
            val details = _uiState.value.mealDetails ?: return@launch
            if (foodRepository.getSavedRecipesForBaseFood(mealId).isNotEmpty()) {
                _uiState.update { it.copy(showOverrideRecipeDialog = true) }
                return@launch
            }
            persistRecipe(details, overrideExisting = false)
        }
    }

    fun confirmOverrideRecipe() {
        viewModelScope.launch {
            val details = _uiState.value.mealDetails ?: return@launch
            persistRecipe(details, overrideExisting = true)
            _uiState.update { it.copy(showOverrideRecipeDialog = false) }
        }
    }

    fun dismissOverrideRecipe() {
        _uiState.update { it.copy(showOverrideRecipeDialog = false) }
    }

    private suspend fun persistRecipe(details: MealDetails, overrideExisting: Boolean) {
        foodRepository.saveRecipe(
            baseFood = Meal(
                id = details.id,
                title = details.title,
                calories = details.baseCalories,
                proteinGrams = details.baseProteinGrams,
                carbsGrams = details.baseCarbsGrams,
                fatGrams = details.baseFatGrams,
                mealType = details.mealType,
                loggedAtEpochMillis = 0,
            ),
            ingredients = details.ingredients.map {
                SavedRecipeIngredientInput(
                    ingredientName = it.name,
                    grams = it.baseGrams.toFloat(),
                )
            },
            overrideExisting = overrideExisting,
        )
    }

    fun logMeal() {
        viewModelScope.launch {
            val details = _uiState.value.mealDetails ?: return@launch
            val portionGrams = currentPortionGrams(details)
            if (!isEditingHistoryMeal && !hasSavedPortionPreference && portionGrams != DEFAULT_PORTION_GRAMS) {
                _uiState.update { it.copy(showPortionPreferenceDialog = true) }
                return@launch
            }
            if (!isEditingHistoryMeal && !hasSavedPortionPreference) {
                foodRepository.savePortionPreference(details.id, DEFAULT_PORTION_GRAMS)
                hasSavedPortionPreference = true
            }
            commitMealLog(details)
        }
    }

    fun confirmPortionPreference() {
        viewModelScope.launch {
            val details = _uiState.value.mealDetails ?: return@launch
            val portionGrams = currentPortionGrams(details)
            foodRepository.savePortionPreference(details.id, portionGrams)
            hasSavedPortionPreference = true
            _uiState.update { it.copy(showPortionPreferenceDialog = false) }
            commitMealLog(details)
        }
    }

    fun dismissPortionPreference() {
        viewModelScope.launch {
            val details = _uiState.value.mealDetails ?: return@launch
            foodRepository.savePortionPreference(details.id, DEFAULT_PORTION_GRAMS)
            hasSavedPortionPreference = true
            _uiState.update { it.copy(showPortionPreferenceDialog = false) }
            commitMealLog(details)
        }
    }

    fun cancelPortionPreferenceDialog() {
        _uiState.update { it.copy(showPortionPreferenceDialog = false) }
    }

    private suspend fun commitMealLog(details: MealDetails) {
        val meal = Meal(
            id = details.id,
            title = details.title,
            calories = details.calculatedCalories,
            proteinGrams = details.calculatedProteinGrams,
            carbsGrams = details.calculatedCarbsGrams,
            fatGrams = details.calculatedFatGrams,
            mealType = details.mealType,
            loggedAtEpochMillis = 0,
            imageUrl = details.imageUrl,
        )
        val ingredients = details.ingredients.map { ingredient ->
            LoggedIngredientInput(
                ingredientTitle = ingredient.name,
                ingredientFdcId = null,
                normalizedWeightGrams = ingredient.baseGrams * details.servings,
                calories = (ingredient.calories * details.servings).toInt(),
                proteinGrams = (ingredient.proteinGrams * details.servings).toInt(),
                carbsGrams = (ingredient.carbsGrams * details.servings).toInt(),
                fatGrams = (ingredient.fatGrams * details.servings).toInt(),
            )
        }
        runCatching {
            if (_uiState.value.isEditingHistoryMeal) {
                nutritionRepository.updateHistoryMeal(meal, ingredients, currentPortionGrams(details))
            } else {
                nutritionRepository.logHistory(
                    LoggedMealInput(
                        sourceFdcId = details.id,
                        sourceRecipeId = null,
                        displayName = details.title,
                        portionGrams = currentPortionGrams(details),
                        calories = details.calculatedCalories,
                        proteinGrams = details.calculatedProteinGrams,
                        carbsGrams = details.calculatedCarbsGrams,
                        fatGrams = details.calculatedFatGrams,
                        mealTypeLabel = details.mealType,
                        imageUrl = details.imageUrl,
                        ingredients = ingredients,
                    )
                )
            }
        }.onSuccess {
            _uiState.update { it.copy(isLogged = true) }
        }.onFailure { error ->
            _uiState.update { it.copy(error = error.message) }
        }
    }

    private fun currentPortionGrams(details: MealDetails): Float =
        details.mealType.removeSuffix(" g").toFloatOrNull()?.times(details.servings) ?: (DEFAULT_PORTION_GRAMS * details.servings)

    fun deleteMeal() {
        viewModelScope.launch {
            runCatching {
                if (_uiState.value.isEditingHistoryMeal) {
                    nutritionRepository.deleteHistoryMeal(mealId)
                } else {
                    foodRepository.deleteMeal(mealId)
                }
            }.onSuccess {
                _uiState.update { it.copy(isDeleted = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
}

data class PopupAction(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun AnchoredActionPopup(
    title: String,
    width: androidx.compose.ui.unit.Dp = 220.dp,
    message: String? = null,
    alignment: Alignment,
    offset: IntOffset,
    onDismiss: () -> Unit,
    actions: List<PopupAction>,
) {
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(width)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
            }
            actions.forEach { action ->
                TextButton(
                    onClick = action.onClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = action.label,
                        color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
