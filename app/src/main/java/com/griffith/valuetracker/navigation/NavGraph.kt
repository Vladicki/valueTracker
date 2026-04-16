package com.griffith.valuetracker.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestaurantMenu
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.griffith.valuetracker.data.UserPrefsDataStore
import com.griffith.valuetracker.presentation.HomeScreen
import com.griffith.valuetracker.presentation.OnboardingScreen
import com.griffith.valuetracker.presentation.SavedFoodsScreen
import com.griffith.valuetracker.presentation.SettingsScreen
import com.griffith.valuetracker.presentation.StatsScreen
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.presentation.camera.CameraScreen
import com.griffith.valuetracker.presentation.camera.ImagePreviewScreen
import com.griffith.valuetracker.presentation.food.FoodDetailsScreen
import com.griffith.valuetracker.presentation.food.FoodDetailsViewModel
import com.griffith.valuetracker.presentation.food.FoodScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.griffith.valuetracker.ui.components.BottomBarItem
import com.griffith.valuetracker.ui.components.BottomBarWithCenterAction
import com.griffith.valuetracker.ui.components.ShellTopBar
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import androidx.compose.material3.Scaffold

sealed class NavRoutes(val route: String) {
    object Onboarding : NavRoutes("onboarding")
    object Shell : NavRoutes("shell")
    object Home : NavRoutes("home")
    object Food : NavRoutes("food")
    object Stats : NavRoutes("stats")
    object Settings : NavRoutes("settings")
    object Camera : NavRoutes("camera")
    object SavedFoods : NavRoutes("saved_foods")
    object ImagePreview : NavRoutes("image_preview/{imageUri}") {
        fun createRoute(imageUri: String) = "image_preview/$imageUri"
    }
    object FoodDetails : NavRoutes("food_details/{mealId}") {
        fun createRoute(mealId: Long) = "food_details/$mealId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    userPrefsDataStore: UserPrefsDataStore = koinInject(),
) {
    val isOnboardingCompleted by userPrefsDataStore
        .isOnboardingCompleted()
        .map<Boolean, Boolean?> { it }
        .collectAsState(initial = null)

    if (isOnboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (isOnboardingCompleted == true) NavRoutes.Shell.route else NavRoutes.Onboarding.route,
    ) {
        composable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(NavRoutes.Shell.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.Shell.route) {
            ValueTrackerShell()
        }
    }
}

private fun copyImageToLocalStorage(context: Context, sourceUri: Uri): String? {
    val outputDirectory = File(context.filesDir, "meal_images").apply { if (!exists()) mkdirs() }
    val targetFile = File(outputDirectory, "meal-${System.currentTimeMillis()}.jpg")
    return runCatching {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
        Uri.fromFile(targetFile).toString()
    }.getOrNull()
}

private fun saveBitmapToLocalStorage(context: Context, bitmap: Bitmap): String? {
    val outputDirectory = File(context.filesDir, "meal_images").apply { if (!exists()) mkdirs() }
    val targetFile = File(outputDirectory, "meal-${System.currentTimeMillis()}.jpg")
    return runCatching {
        FileOutputStream(targetFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        Uri.fromFile(targetFile).toString()
    }.getOrNull()
}

@Composable
private fun ValueTrackerShell() {
    var mealToEdit by remember { mutableStateOf<Meal?>(null) }
    var selectedFoodMeal by remember { mutableStateOf<Meal?>(null) }
    val shellNavController = rememberNavController()
    val currentBackStackEntry by shellNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: NavRoutes.Home.route

    fun navigateTopLevel(route: String) {
        shellNavController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(shellNavController.graph.startDestinationId) {
                saveState = true
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            val encodedUri = URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString())
            shellNavController.navigate(NavRoutes.ImagePreview.createRoute(encodedUri)) {
                launchSingleTop = true
            }
        }
    }

    val items = listOf(
        BottomBarItem(icon = Icons.Default.Home, label = "Home"),
        BottomBarItem(icon = Icons.Default.RestaurantMenu, label = "Food"),
        BottomBarItem(icon = Icons.Default.RestaurantMenu, label = "Quick Actions"),
        BottomBarItem(icon = Icons.Default.BarChart, label = "Stats"),
    )

    val showShellChrome = currentRoute !in setOf(
        NavRoutes.Settings.route,
        NavRoutes.Camera.route,
        NavRoutes.ImagePreview.route,
        NavRoutes.FoodDetails.route,
    )

    Scaffold(
        topBar = {
            if (showShellChrome && currentRoute != NavRoutes.Food.route) {
                ShellTopBar(
                    title = if (currentRoute == NavRoutes.Stats.route) "Stats" else "ValueTracker",
                    streakCount = 0,
                    onProfileClick = {
                        shellNavController.navigate(NavRoutes.Settings.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(NavRoutes.Home.route) { saveState = true }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showShellChrome) {
                BottomBarWithCenterAction(
                    selectedIndex = when (currentRoute) {
                        NavRoutes.Home.route -> 0
                        NavRoutes.Food.route -> 1
                        NavRoutes.Stats.route -> 3
                        else -> -1
                    },
                    onItemClick = { index ->
                        val route = when (index) {
                            1 -> NavRoutes.Food.route
                            3 -> NavRoutes.Stats.route
                            else -> NavRoutes.Home.route
                        }
                        navigateTopLevel(route)
                    },
                    onScanFoodClick = {
                        shellNavController.navigate(NavRoutes.Camera.route) {
                            launchSingleTop = true
                        }
                    },
                    onGalleryClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onFoodDatabaseClick = {
                        navigateTopLevel(NavRoutes.Food.route)
                    },
                    items = items,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = shellNavController,
            startDestination = NavRoutes.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(NavRoutes.Home.route) {
                HomeScreen(
                    onEditMeal = { meal ->
                        mealToEdit = meal
                        selectedFoodMeal = meal
                        shellNavController.navigate(NavRoutes.FoodDetails.createRoute(meal.id)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(NavRoutes.Stats.route) {
                StatsScreen()
            }
            composable(NavRoutes.Food.route) {
                FoodScreen(
                    onAddFoodClick = { meal ->
                        selectedFoodMeal = meal
                        shellNavController.navigate(NavRoutes.FoodDetails.createRoute(meal.id)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(NavRoutes.Settings.route) {
                SettingsScreen()
            }
            composable(NavRoutes.Camera.route) {
                CameraScreen(
                    onNavigateToSavedFoods = {
                        shellNavController.navigate(NavRoutes.SavedFoods.route)
                    },
                    onNavigateToPreview = { encodedUri ->
                        shellNavController.navigate(NavRoutes.ImagePreview.createRoute(encodedUri))
                    }
                )
            }
            composable(NavRoutes.SavedFoods.route) {
                SavedFoodsScreen(
                    onOpenRecipe = { recipeId ->
                        selectedFoodMeal = null
                        shellNavController.navigate(NavRoutes.FoodDetails.createRoute(kotlin.math.abs(recipeId))) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = NavRoutes.ImagePreview.route,
                arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                val imageUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
                ImagePreviewScreen(
                    imageUri = imageUri,
                    onBack = { shellNavController.popBackStack() },
                    onConfirm = {
                        shellNavController.popBackStack(NavRoutes.Home.route, inclusive = false)
                    }
                )
            }
            composable(
                route = NavRoutes.FoodDetails.route,
                arguments = listOf(navArgument("mealId") { type = NavType.LongType })
            ) { backStackEntry ->
                val mealId = backStackEntry.arguments?.getLong("mealId") ?: -1L
                val editingHistoryMeal = mealToEdit?.id == mealId
                val initialMeal = selectedFoodMeal?.takeIf { it.id == mealId }
                val viewModel: FoodDetailsViewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(mealId, editingHistoryMeal, initialMeal) })
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val context = androidx.compose.ui.platform.LocalContext.current
                val detailsGalleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
                ) { uri ->
                    uri?.let {
                        copyImageToLocalStorage(context, it)?.let(viewModel::setImageUrl)
                    }
                }
                val detailsCameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicturePreview(),
                ) { bitmap ->
                    bitmap?.let {
                        saveBitmapToLocalStorage(context, it)?.let(viewModel::setImageUrl)
                    }
                }
                if (state.isDeleted) {
                    shellNavController.popBackStack()
                }
                if (state.isLogged) {
                    shellNavController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                FoodDetailsScreen(
                    state = state,
                    onBackClick = { shellNavController.popBackStack() },
                    onBookmarkClick = viewModel::toggleBookmark,
                    onEditClick = {},
                    onDeleteClick = viewModel::deleteMeal,
                    onServingsModeClick = viewModel::selectPortionMode,
                    onServingsStepChange = viewModel::stepPortionSize,
                    onAddedOilChange = viewModel::updateAddedOil,
                    onPickImageCamera = { detailsCameraLauncher.launch(null) },
                    onPickImageGallery = {
                        detailsGalleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPictureMenuToggle = viewModel::setPictureMenuExpanded,
                    onSaveRecipe = viewModel::saveRecipe,
                    onConfirmOverrideRecipe = viewModel::confirmOverrideRecipe,
                    onDismissOverrideDialog = viewModel::dismissOverrideRecipe,
                    onConfirmPortionPreference = viewModel::confirmPortionPreference,
                    onDismissPortionPreference = viewModel::dismissPortionPreference,
                    onCancelPortionPreference = viewModel::cancelPortionPreferenceDialog,
                    onDoneClick = viewModel::logMeal,
                )
            }
        }
    }
}

