package com.griffith.valuetracker.di

import com.griffith.valuetracker.data.DatabaseFoodRepository
import com.griffith.valuetracker.data.DatabaseNutritionRepository
import com.griffith.valuetracker.data.DatabaseProfileRepository
import com.griffith.valuetracker.data.DatabaseStorage
import com.griffith.valuetracker.data.SeedDataInitializer
import com.griffith.valuetracker.data.UserPrefsDataStore
import com.griffith.valuetracker.data.repository.FoodRepository
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.presentation.AddFoodViewModel
import com.griffith.valuetracker.presentation.HomeViewModel
import com.griffith.valuetracker.presentation.OnboardingViewModel
import com.griffith.valuetracker.presentation.SettingsViewModel
import com.griffith.valuetracker.presentation.StatsViewModel
import com.griffith.valuetracker.presentation.food.FoodDetailsViewModel
import com.griffith.valuetracker.presentation.food.FoodViewModel
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { UserPrefsDataStore(androidContext()) }
    single { DatabaseStorage.build(androidContext()) }
    single { get<DatabaseStorage>().allFoodDao() }
    single { get<DatabaseStorage>().savedRecipeDao() }
    single { get<DatabaseStorage>().savedRecipeIngredientDao() }
    single { get<DatabaseStorage>().foodItemDao() }
    single { get<DatabaseStorage>().nutrientDao() }
    single { get<DatabaseStorage>().foodNutrientDao() }
    single { get<DatabaseStorage>().foodPortionDao() }
    single { get<DatabaseStorage>().foodSearchDao() }
    single { get<DatabaseStorage>().mealHistoryDao() }
    single { get<DatabaseStorage>().ingredientHistoryDao() }
    single { get<DatabaseStorage>().mealLogDao() }
    single { get<DatabaseStorage>().weightDao() }
    single { get<DatabaseStorage>().profileDao() }
    single { get<DatabaseStorage>().mealDetailDao() }
    single { get<DatabaseStorage>().portionPreferenceDao() }
    single { get<DatabaseStorage>().mealIngredientDao() }
    single<NutritionRepository> { DatabaseNutritionRepository(get(), get(), get(), get(), get()) }
    single<FoodRepository> { DatabaseFoodRepository(get(), get(), get(), get(), get(), get(), get()) }
    single<ProfileRepository> { DatabaseProfileRepository(get()) }
    single { SeedDataInitializer(androidContext(), get()) }

    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { StatsViewModel(get(), get()) }
    viewModel { AddFoodViewModel(get(), get()) }
    viewModel { FoodViewModel(get()) }
    viewModel { (mealId: Long, isEditingHistoryMeal: Boolean, initialMeal: com.griffith.valuetracker.domain.model.Meal?) -> FoodDetailsViewModel(mealId, isEditingHistoryMeal, initialMeal, get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { com.griffith.valuetracker.presentation.camera.CameraViewModel() }
}
