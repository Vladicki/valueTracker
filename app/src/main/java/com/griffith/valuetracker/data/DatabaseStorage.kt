package com.griffith.valuetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.griffith.valuetracker.data.repository.FoodRepository
import com.griffith.valuetracker.data.repository.LoggedIngredientInput
import com.griffith.valuetracker.data.repository.NutritionRepository
import com.griffith.valuetracker.data.repository.ProfileRepository
import com.griffith.valuetracker.data.repository.SavedRecipeIngredientInput
import com.griffith.valuetracker.domain.model.DailySummary
import com.griffith.valuetracker.domain.model.Ingredient
import com.griffith.valuetracker.domain.model.Meal
import com.griffith.valuetracker.domain.model.MealDetails
import com.griffith.valuetracker.domain.model.OnboardingAnswer
import com.griffith.valuetracker.domain.model.SecondaryNutrient
import com.griffith.valuetracker.domain.model.UserProfile
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class FoodSearchRow(
    val fdcId: Long,
    val name: String,
    val category: String?,
    val gramWeight: Float?,
    val calories: Float,
    val nutrientsSummary: String,
)

@Entity(tableName = "all_food")
data class AllFoodEntity(
    @PrimaryKey val fdcId: Long,
    val foodName: String,
    val defaultPortionGrams: Float?,
    val caloriesKcal: Float,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val fiberGrams: Float,
    val totalSugarsGrams: Float,
    val starchGrams: Float,
    val calciumMg: Float,
    val ironMg: Float,
    val magnesiumMg: Float,
    val phosphorusMg: Float,
    val potassiumMg: Float,
    val sodiumMg: Float,
    val zincMg: Float,
    val seleniumUg: Float,
    val caffeineMg: Float,
    val alcoholGrams: Float,
    val vitaminAiu: Float,
    val vitaminCmg: Float,
    val vitaminDug: Float,
    val vitaminEmg: Float,
    val vitaminKug: Float,
    val thiaminMg: Float,
    val riboflavinMg: Float,
    val niacinMg: Float,
    val vitaminB6Mg: Float,
    val folateUg: Float,
    val vitaminB12Ug: Float,
    val pantothenicAcidMg: Float,
    val cholineMg: Float,
    val omega3Grams: Float,
    val omega6Grams: Float,
    val monounsaturatedFatGrams: Float,
    val polyunsaturatedFatGrams: Float,
    val saturatedFatGrams: Float,
    val saved: Boolean,
)

@Entity(tableName = "saved_recipes")
data class SavedRecipeEntity(
    @PrimaryKey(autoGenerate = true) val recipeId: Long = 0,
    val baseFdcId: Long,
    val recipeName: String,
    val basePortionGrams: Float?,
    val saved: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "saved_recipe_ingredients", primaryKeys = ["recipeId", "position"])
data class SavedRecipeIngredientEntity(
    val recipeId: Long,
    val position: Int,
    val ingredientName: String,
    val grams: Float,
    val ingredientFdcId: Long? = null,
)

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val fdcId: Long,
    val name: String,
    val category: String?,
    val dataType: String,
)

@Entity(tableName = "nutrients")
data class NutrientEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val unitName: String,
    val nutrientNumber: String,
)

@Entity(tableName = "food_nutrients", primaryKeys = ["fdcId", "nutrientId"])
data class FoodNutrientEntity(
    val fdcId: Long,
    val nutrientId: Long,
    val amount: Float,
)

@Entity(tableName = "food_portions")
data class FoodPortionEntity(
    @PrimaryKey val id: Long,
    val fdcId: Long,
    val gramWeight: Float,
)

// Meal history stores immutable snapshots so later recipe edits do not rewrite past nutrition logs.
@Entity(tableName = "meal_history")
data class MealHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    val sourceFdcId: Long?,
    val sourceRecipeId: Long?,
    val displayName: String,
    val loggedAtEpochMillis: Long,
    val portionGrams: Float,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val mealTypeLabel: String,
    val imageUrl: String? = null,
)

@Entity(tableName = "ingredient_history", primaryKeys = ["historyId", "position"])
data class IngredientHistoryEntity(
    val historyId: Long,
    val position: Int,
    val ingredientTitle: String,
    val ingredientFdcId: Long?,
    val normalizedWeightGrams: Float,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
)

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val mealType: String,
    val loggedAtEpochMillis: Long,
)

@Entity(tableName = "weight_entries")
data class WeightRowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Float,
    val recordedAtEpochMillis: Long,
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val firstName: String,
    val goalType: String,
    val targetCalories: Int,
    val targetProtein: Int,
    val targetCarbs: Int,
    val targetFat: Int,
    val heightCm: Int,
    val weightKg: Float,
    val age: Int,
)

@Entity(tableName = "meal_details")
data class MealDetailEntity(
    @PrimaryKey val mealId: Long,
    val title: String,
    val baseCalories: Int,
    val baseProteinGrams: Int,
    val baseCarbsGrams: Int,
    val baseFatGrams: Int,
    val mealType: String,
    val servings: Float,
    val hasOil: Boolean,
    val addedOilGrams: Int,
    val isBookmarked: Boolean,
    val listBucket: String,
)

@Entity(tableName = "portion_preference")
data class PortionPreferenceEntity(
    @PrimaryKey val foodId: Long,
    val portionGrams: Float,
)

@Entity(tableName = "meal_ingredients", primaryKeys = ["mealId", "position"])
data class MealIngredientEntity(
    val mealId: Long,
    val position: Int,
    val name: String,
    val baseGrams: Int,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
)

@Dao
interface AllFoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AllFoodEntity>)

    @Query("SELECT * FROM all_food WHERE foodName LIKE '%' || :query || '%' ORDER BY foodName ASC LIMIT 100")
    suspend fun searchByName(query: String): List<AllFoodEntity>

    @Query("SELECT * FROM all_food WHERE fdcId = :fdcId LIMIT 1")
    suspend fun findById(fdcId: Long): AllFoodEntity?

    @Query("SELECT * FROM all_food WHERE saved = 1 ORDER BY foodName ASC")
    fun observeSavedFoods(): Flow<List<AllFoodEntity>>

    @Query("UPDATE all_food SET saved = :saved WHERE fdcId = :fdcId")
    suspend fun setSaved(fdcId: Long, saved: Boolean)

    @Query("SELECT COUNT(*) FROM all_food")
    suspend fun countRows(): Int
}

@Dao
interface SavedRecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: SavedRecipeEntity): Long

    @Query("SELECT * FROM saved_recipes WHERE recipeId = :recipeId LIMIT 1")
    suspend fun findById(recipeId: Long): SavedRecipeEntity?

    @Query("SELECT * FROM saved_recipes WHERE baseFdcId = :fdcId ORDER BY updatedAtEpochMillis DESC")
    suspend fun findByBaseFdcId(fdcId: Long): List<SavedRecipeEntity>

    @Query("SELECT * FROM saved_recipes WHERE saved = 1 ORDER BY updatedAtEpochMillis DESC")
    fun observeSavedRecipes(): Flow<List<SavedRecipeEntity>>

    @Query("UPDATE saved_recipes SET saved = :saved WHERE recipeId = :recipeId")
    suspend fun setSaved(recipeId: Long, saved: Boolean)
}

@Dao
interface SavedRecipeIngredientDao {
    @Query("SELECT * FROM saved_recipe_ingredients WHERE recipeId = :recipeId ORDER BY position ASC")
    suspend fun findByRecipeId(recipeId: Long): List<SavedRecipeIngredientEntity>

    @Query("DELETE FROM saved_recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavedRecipeIngredientEntity>)

    @Transaction
    suspend fun replaceForRecipe(recipeId: Long, ingredients: List<SavedRecipeIngredientEntity>) {
        deleteByRecipeId(recipeId)
        insertAll(ingredients)
    }
}

@Dao
interface FoodItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FoodItemEntity>)

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun countRows(): Int
}

@Dao
interface NutrientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NutrientEntity>)
}

@Dao
interface FoodNutrientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FoodNutrientEntity>)
}

@Dao
interface FoodPortionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FoodPortionEntity>)
}

@Dao
interface FoodSearchDao {
    @Query(
        """
        SELECT
            fi.fdcId AS fdcId,
            fi.name AS name,
            fi.category AS category,
            fp.gramWeight AS gramWeight,
            COALESCE(MAX(CASE WHEN n.name = 'Energy' THEN fn.amount END), 0) AS calories,
            COALESCE(
                GROUP_CONCAT(
                    CASE
                        WHEN n.name IS NOT NULL THEN n.name || ': ' || fn.amount || ' ' || n.unitName
                    END,
                    ' | '
                ),
                ''
            ) AS nutrientsSummary
        FROM food_items fi
        LEFT JOIN food_nutrients fn ON fn.fdcId = fi.fdcId
        LEFT JOIN nutrients n ON n.id = fn.nutrientId
        LEFT JOIN food_portions fp ON fp.fdcId = fi.fdcId
        WHERE fi.name LIKE '%' || :query || '%'
        GROUP BY fi.fdcId, fi.name, fi.category, fp.gramWeight
        ORDER BY fi.name ASC
        LIMIT 100
        """
    )
    suspend fun searchFoods(query: String): List<FoodSearchRow>

    @Query(
        """
        SELECT
            fi.fdcId AS fdcId,
            fi.name AS name,
            fi.category AS category,
            fp.gramWeight AS gramWeight,
            COALESCE(MAX(CASE WHEN n.name = 'Energy' THEN fn.amount END), 0) AS calories,
            COALESCE(
                GROUP_CONCAT(
                    CASE
                        WHEN n.name IS NOT NULL THEN n.name || ': ' || fn.amount || ' ' || n.unitName
                    END,
                    ' | '
                ),
                ''
            ) AS nutrientsSummary
        FROM food_items fi
        LEFT JOIN food_nutrients fn ON fn.fdcId = fi.fdcId
        LEFT JOIN nutrients n ON n.id = fn.nutrientId
        LEFT JOIN food_portions fp ON fp.fdcId = fi.fdcId
        WHERE fi.fdcId = :fdcId
        GROUP BY fi.fdcId, fi.name, fi.category, fp.gramWeight
        LIMIT 1
        """
    )
    suspend fun findFoodById(fdcId: Long): FoodSearchRow?
}

@Dao
interface MealHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MealHistoryEntity): Long

    @Query("SELECT * FROM meal_history WHERE loggedAtEpochMillis >= :cutoff ORDER BY loggedAtEpochMillis DESC")
    fun observeSince(cutoff: Long): Flow<List<MealHistoryEntity>>

    @Query("SELECT * FROM meal_history WHERE historyId = :mealId LIMIT 1")
    suspend fun findById(mealId: Long): MealHistoryEntity?

    @Update
    suspend fun update(entry: MealHistoryEntity)

    @Query("DELETE FROM meal_history WHERE historyId = :mealId")
    suspend fun deleteById(mealId: Long)
}

@Dao
interface IngredientHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<IngredientHistoryEntity>)

    @Query("SELECT * FROM ingredient_history WHERE historyId = :mealId ORDER BY position ASC")
    suspend fun findByMealId(mealId: Long): List<IngredientHistoryEntity>

    @Query("DELETE FROM ingredient_history WHERE historyId = :mealId")
    suspend fun deleteByMealId(mealId: Long)
}

@Dao
interface MealLogDao {
    @Query("SELECT * FROM meal_logs ORDER BY loggedAtEpochMillis DESC")
    fun observeAll(): Flow<List<MealLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meal: MealLogEntity)

    @Query("SELECT COUNT(*) FROM meal_logs")
    suspend fun countRows(): Int
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY recordedAtEpochMillis DESC")
    fun observeAll(): Flow<List<WeightRowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightRowEntity)

    @Query("SELECT COUNT(*) FROM weight_entries")
    suspend fun countRows(): Int
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface MealDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(detail: MealDetailEntity)

    @Query("SELECT * FROM meal_details WHERE mealId = :mealId")
    suspend fun findByMealId(mealId: Long): MealDetailEntity?

    @Query("SELECT * FROM meal_details WHERE listBucket = :bucket ORDER BY mealId ASC")
    suspend fun findByBucket(bucket: String): List<MealDetailEntity>

    @Query("DELETE FROM meal_details WHERE mealId = :mealId")
    suspend fun deleteByMealId(mealId: Long)
}

@Dao
interface MealIngredientDao {
    @Query("SELECT * FROM meal_ingredients WHERE mealId = :mealId ORDER BY position ASC")
    suspend fun findByMealId(mealId: Long): List<MealIngredientEntity>

    @Query("DELETE FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun deleteByMealId(mealId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MealIngredientEntity>)

    @Transaction
    suspend fun replaceForMeal(mealId: Long, ingredients: List<MealIngredientEntity>) {
        deleteByMealId(mealId)
        insertAll(ingredients)
    }
}

@Dao
interface PortionPreferenceDao {
    @Query("SELECT * FROM portion_preference WHERE foodId = :foodId LIMIT 1")
    suspend fun findByFoodId(foodId: Long): PortionPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: PortionPreferenceEntity)
}

@Database(
    entities = [
        AllFoodEntity::class,
        SavedRecipeEntity::class,
        SavedRecipeIngredientEntity::class,
        MealHistoryEntity::class,
        IngredientHistoryEntity::class,
        FoodItemEntity::class,
        NutrientEntity::class,
        FoodNutrientEntity::class,
        FoodPortionEntity::class,
        MealLogEntity::class,
        WeightRowEntity::class,
        UserProfileEntity::class,
        MealDetailEntity::class,
        PortionPreferenceEntity::class,
        MealIngredientEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class DatabaseStorage : RoomDatabase() {
    abstract fun allFoodDao(): AllFoodDao
    abstract fun savedRecipeDao(): SavedRecipeDao
    abstract fun savedRecipeIngredientDao(): SavedRecipeIngredientDao
    abstract fun mealHistoryDao(): MealHistoryDao
    abstract fun ingredientHistoryDao(): IngredientHistoryDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun nutrientDao(): NutrientDao
    abstract fun foodNutrientDao(): FoodNutrientDao
    abstract fun foodPortionDao(): FoodPortionDao
    abstract fun foodSearchDao(): FoodSearchDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun weightDao(): WeightDao
    abstract fun profileDao(): ProfileDao
    abstract fun mealDetailDao(): MealDetailDao
    abstract fun portionPreferenceDao(): PortionPreferenceDao
    abstract fun mealIngredientDao(): MealIngredientDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `portion_preference` (`foodId` INTEGER NOT NULL, `portionGrams` REAL NOT NULL, PRIMARY KEY(`foodId`))",
                )
            }
        }

        fun build(context: Context): DatabaseStorage =
            Room.databaseBuilder(
                context.applicationContext,
                DatabaseStorage::class.java,
                "valuetracker-single-file.db",
            ).addMigrations(MIGRATION_3_4).build()
    }
}

private data class FoodRow(
    val fdcId: Long,
    val name: String,
    val category: String?,
    val dataType: String,
)

private fun loadFoods(file: File, limit: Int): List<FoodRow> {
    val foods = mutableListOf<FoodRow>()
    file.bufferedReader().useLines { lines ->
        val iterator = lines.iterator()
        if (!iterator.hasNext()) return@useLines
        val header = parseCsvRow(iterator.next())
        val idIndex = header.indexOf("fdc_id")
        val dataTypeIndex = header.indexOf("data_type")
        val descriptionIndex = header.indexOf("description")
        val categoryIndex = header.indexOf("food_category_id")

        while (iterator.hasNext() && foods.size < limit) {
            val columns = parseCsvRow(iterator.next())
            val fdcId = columns.getOrNull(idIndex)?.toLongOrNull() ?: continue
            val dataType = columns.getOrNull(dataTypeIndex).orEmpty()
            if (dataType.equals("branded_food", ignoreCase = true)) continue
            val name = columns.getOrNull(descriptionIndex)?.trim().orEmpty()
            if (name.isBlank()) continue
            foods += FoodRow(
                fdcId = fdcId,
                name = name,
                category = columns.getOrNull(categoryIndex)?.ifBlank { null },
                dataType = dataType,
            )
        }
    }
    return foods
}

private fun loadNutrientLookup(file: File): Map<Long, String> {
    // USDA nutrient names vary from app labels, so seeding normalizes them once into stable internal keys.
    val lookup = mutableMapOf<Long, String>()
    file.bufferedReader().useLines { lines ->
        val iterator = lines.iterator()
        if (!iterator.hasNext()) return@useLines
        val header = parseCsvRow(iterator.next())
        val idIndex = header.indexOf("id")
        val nameIndex = header.indexOf("name")
        val unitIndex = header.indexOf("unit_name")

        while (iterator.hasNext()) {
            val columns = parseCsvRow(iterator.next())
            val id = columns.getOrNull(idIndex)?.toLongOrNull() ?: continue
            val name = columns.getOrNull(nameIndex).orEmpty()
            val unit = columns.getOrNull(unitIndex).orEmpty()
            val normalizedName = name.lowercase()
            // USDA nutrient names are noisy, so seeding normalizes only the labels this app can render and query later.
            val key = when {
                normalizedName == "protein" -> "protein"
                normalizedName == "carbohydrate, by difference" -> "carbs"
                normalizedName == "total lipid (fat)" -> "fat"
                normalizedName == "fiber, total dietary" -> "fiber"
                normalizedName == "sugars, total including nlea" -> "totalSugars"
                normalizedName == "starch" -> "starch"
                normalizedName == "calcium, ca" -> "calcium"
                normalizedName == "iron, fe" -> "iron"
                normalizedName == "magnesium, mg" -> "magnesium"
                normalizedName == "phosphorus, p" -> "phosphorus"
                normalizedName == "potassium, k" -> "potassium"
                normalizedName == "sodium, na" -> "sodium"
                normalizedName == "zinc, zn" -> "zinc"
                normalizedName == "selenium, se" -> "selenium"
                normalizedName == "caffeine" -> "caffeine"
                normalizedName == "alcohol, ethyl" -> "alcohol"
                normalizedName == "vitamin a, iu" -> "vitaminAiu"
                normalizedName == "vitamin c, total ascorbic acid" -> "vitaminCmg"
                normalizedName == "vitamin d (d2 + d3), international units" -> "vitaminDug"
                normalizedName == "vitamin e (alpha-tocopherol)" -> "vitaminEmg"
                normalizedName == "vitamin k (phylloquinone)" -> "vitaminKug"
                normalizedName == "thiamin" -> "thiaminMg"
                normalizedName == "riboflavin" -> "riboflavinMg"
                normalizedName == "niacin" -> "niacinMg"
                normalizedName == "vitamin b-6" -> "vitaminB6Mg"
                normalizedName == "folate, total" -> "folateUg"
                normalizedName == "vitamin b-12" -> "vitaminB12Ug"
                normalizedName == "pantothenic acid" -> "pantothenicAcidMg"
                normalizedName == "choline, total" -> "cholineMg"
                normalizedName == "18:3 n-3 c,c,c (ala)" -> "omega3"
                normalizedName == "18:2 n-6 c,c" -> "omega6"
                normalizedName == "fatty acids, total monounsaturated" -> "monounsaturatedFat"
                normalizedName == "fatty acids, total polyunsaturated" -> "polyunsaturatedFat"
                normalizedName == "fatty acids, total saturated" -> "saturatedFat"
                normalizedName == "energy" && unit.equals("KCAL", ignoreCase = true) -> "calories"
                else -> null
            } ?: continue
            lookup[id] = key
        }
    }
    return lookup
}

private fun loadFoodNutrients(
    file: File,
    nutrientLookup: Map<Long, String>,
    targetIds: Set<Long>,
): Map<Long, Map<String, Float>> {
    val amounts = mutableMapOf<Long, MutableMap<String, Float>>()
    file.bufferedReader().useLines { lines ->
        val iterator = lines.iterator()
        if (!iterator.hasNext()) return@useLines
        val header = parseCsvRow(iterator.next())
        val foodIndex = header.indexOf("fdc_id")
        val nutrientIndex = header.indexOf("nutrient_id")
        val amountIndex = header.indexOf("amount")

        while (iterator.hasNext()) {
            val columns = parseCsvRow(iterator.next())
            val foodId = columns.getOrNull(foodIndex)?.toLongOrNull() ?: continue
            if (foodId !in targetIds) continue
            val nutrientId = columns.getOrNull(nutrientIndex)?.toLongOrNull() ?: continue
            val key = nutrientLookup[nutrientId] ?: continue
            val amount = columns.getOrNull(amountIndex)?.toFloatOrNull() ?: continue
            amounts.getOrPut(foodId) { mutableMapOf() }[key] = amount
        }
    }
    return amounts
}

private fun loadPortions(
    file: File,
    targetIds: Set<Long>,
): Map<Long, Float> {
    // First portion wins here so each food gets one stable default gram weight instead of many competing serving sizes.
    val portions = mutableMapOf<Long, Float>()
    file.bufferedReader().useLines { lines ->
        val iterator = lines.iterator()
        if (!iterator.hasNext()) return@useLines
        val header = parseCsvRow(iterator.next())
        val foodIndex = header.indexOf("fdc_id")
        val gramsIndex = header.indexOf("gram_weight")

        while (iterator.hasNext()) {
            val columns = parseCsvRow(iterator.next())
            val foodId = columns.getOrNull(foodIndex)?.toLongOrNull() ?: continue
            if (foodId !in targetIds || portions.containsKey(foodId)) continue
            val grams = columns.getOrNull(gramsIndex)?.toFloatOrNull() ?: continue
            portions[foodId] = grams
        }
    }
    return portions
}

class DatabaseFoodRepository(
    private val foodSearchDao: FoodSearchDao,
    private val mealDetailDao: MealDetailDao,
    private val mealIngredientDao: MealIngredientDao,
    private val allFoodDao: AllFoodDao,
    private val savedRecipeDao: SavedRecipeDao,
    private val savedRecipeIngredientDao: SavedRecipeIngredientDao,
    private val portionPreferenceDao: PortionPreferenceDao,
) : FoodRepository {
    override suspend fun getFavoriteMeals(): List<Meal> =
        mealDetailDao.findByBucket("favorite").map { it.toMeal() }

    override suspend fun getHistoryMeals(): List<Meal> =
        mealDetailDao.findByBucket("history").map { it.toMeal() }

    override suspend fun getMealDetails(mealId: Long): MealDetails {
        // Prefer locally edited details first, then seeded food rows, then ad-hoc search imports as the last fallback.
        val savedDetail = mealDetailDao.findByMealId(mealId)
        if (savedDetail != null) {
            return savedDetail.toMealDetails(mealIngredientDao.findByMealId(mealId).map { it.toIngredient() })
        }

        val allFood = allFoodDao.findById(mealId)
        if (allFood != null) {
            return MealDetails(
                id = allFood.fdcId,
                title = allFood.foodName,
                baseCalories = allFood.caloriesKcal.toInt(),
                baseProteinGrams = allFood.proteinGrams.toInt(),
                baseCarbsGrams = allFood.carbsGrams.toInt(),
                baseFatGrams = allFood.fatGrams.toInt(),
                mealType = allFood.defaultPortionGrams?.let { "$it g" } ?: "100 g",
                isBookmarked = allFood.saved,
                imageUrl = null,
                ingredients = emptyList(),
                secondaryNutrients = allFood.toSecondaryNutrients(),
            )
        }

        val imported = foodSearchDao.findFoodById(mealId) ?: throw NoSuchElementException("Meal with id $mealId not found")
        return MealDetails(
            id = imported.fdcId,
            title = imported.name,
            baseCalories = imported.calories.toInt(),
            baseProteinGrams = parseNutrientAmount(imported.nutrientsSummary, "Protein").toInt(),
            baseCarbsGrams = parseNutrientAmount(imported.nutrientsSummary, "Carbohydrate, by difference").toInt(),
            baseFatGrams = parseNutrientAmount(imported.nutrientsSummary, "Total lipid (fat)").toInt(),
            mealType = imported.gramWeight?.let { "$it g" } ?: "100 g",
            imageUrl = null,
            ingredients = emptyList(),
        )
    }

    override suspend fun updateMealBookmark(mealId: Long, isBookmarked: Boolean) {
        val detail = mealDetailDao.findByMealId(mealId)
        if (detail != null) {
            mealDetailDao.upsert(
                detail.copy(
                    isBookmarked = isBookmarked,
                    listBucket = if (isBookmarked) "favorite" else "history",
                ),
            )
            return
        }
        allFoodDao.setSaved(mealId, isBookmarked)
        if (allFoodDao.findById(mealId) == null && isBookmarked) {
            // Imported search rows are not always seeded locally yet, so bookmarking backfills a minimal record for saved-food flows.
            val imported = foodSearchDao.findFoodById(mealId)
            if (imported != null) {
                allFoodDao.upsertAll(
                    listOf(
                        AllFoodEntity(
                            fdcId = imported.fdcId,
                            foodName = imported.name,
                            defaultPortionGrams = imported.gramWeight,
                            caloriesKcal = imported.calories,
                            proteinGrams = parseNutrientAmount(imported.nutrientsSummary, "Protein"),
                            carbsGrams = parseNutrientAmount(imported.nutrientsSummary, "Carbohydrate, by difference"),
                            fatGrams = parseNutrientAmount(imported.nutrientsSummary, "Total lipid (fat)"),
                            fiberGrams = 0f,
                            totalSugarsGrams = 0f,
                            starchGrams = 0f,
                            calciumMg = 0f,
                            ironMg = 0f,
                            magnesiumMg = 0f,
                            phosphorusMg = 0f,
                            potassiumMg = 0f,
                            sodiumMg = 0f,
                            zincMg = 0f,
                            seleniumUg = 0f,
                            caffeineMg = 0f,
                            alcoholGrams = 0f,
                            vitaminAiu = 0f,
                            vitaminCmg = 0f,
                            vitaminDug = 0f,
                            vitaminEmg = 0f,
                            vitaminKug = 0f,
                            thiaminMg = 0f,
                            riboflavinMg = 0f,
                            niacinMg = 0f,
                            vitaminB6Mg = 0f,
                            folateUg = 0f,
                            vitaminB12Ug = 0f,
                            pantothenicAcidMg = 0f,
                            cholineMg = 0f,
                            omega3Grams = 0f,
                            omega6Grams = 0f,
                            monounsaturatedFatGrams = 0f,
                            polyunsaturatedFatGrams = 0f,
                            saturatedFatGrams = 0f,
                            saved = true,
                        ),
                    ),
                )
            }
        }
    }

    override suspend fun deleteMeal(mealId: Long) {
        mealIngredientDao.deleteByMealId(mealId)
        mealDetailDao.deleteByMealId(mealId)
    }

    override suspend fun searchFoods(query: String): List<Meal> =
        foodSearchDao.searchFoods(query).map { row -> row.toMeal() }

    override fun observeSavedFoods(): Flow<List<Meal>> =
        allFoodDao.observeSavedFoods().map { it.map { it.toMeal() } }

    override fun observeSavedRecipes(): Flow<List<Meal>> =
        savedRecipeDao.observeSavedRecipes().map { it.map { it.toMeal() } }

    override suspend fun getSavedRecipeIngredients(recipeId: Long): List<SavedRecipeIngredientEntity> =
        savedRecipeIngredientDao.findByRecipeId(recipeId)

    override suspend fun setFoodSaved(fdcId: Long, saved: Boolean) {
        allFoodDao.setSaved(fdcId, saved)
    }

    override suspend fun setRecipeSaved(recipeId: Long, saved: Boolean) {
        savedRecipeDao.setSaved(recipeId, saved)
    }

    override suspend fun getSavedRecipesForBaseFood(fdcId: Long): List<Meal> =
        savedRecipeDao.findByBaseFdcId(fdcId).map { it.toMeal() }

    override suspend fun saveRecipe(baseFood: Meal, ingredients: List<SavedRecipeIngredientInput>, overrideExisting: Boolean) {
        val existing = savedRecipeDao.findByBaseFdcId(baseFood.id).firstOrNull()
        if (existing != null && overrideExisting) {
            savedRecipeDao.upsert(existing.copy(recipeName = baseFood.title, basePortionGrams = baseFood.mealType.removeSuffix(" g").toFloatOrNull(), saved = true, updatedAtEpochMillis = System.currentTimeMillis()))
            savedRecipeIngredientDao.replaceForRecipe(existing.recipeId, ingredients.mapIndexed { index, ingredient ->
                SavedRecipeIngredientEntity(existing.recipeId, index, ingredient.ingredientName, ingredient.grams, ingredient.ingredientFdcId)
            })
            return
        }

        val recipeId = savedRecipeDao.upsert(
            SavedRecipeEntity(
                baseFdcId = baseFood.id,
                recipeName = baseFood.title,
                basePortionGrams = baseFood.mealType.removeSuffix(" g").toFloatOrNull(),
                saved = true,
                createdAtEpochMillis = System.currentTimeMillis(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        savedRecipeIngredientDao.replaceForRecipe(recipeId, ingredients.mapIndexed { index, ingredient ->
            SavedRecipeIngredientEntity(recipeId, index, ingredient.ingredientName, ingredient.grams, ingredient.ingredientFdcId)
        })
    }

    override suspend fun getPortionPreference(foodId: Long): Float? =
        portionPreferenceDao.findByFoodId(foodId)?.portionGrams

    override suspend fun savePortionPreference(foodId: Long, portionGrams: Float) {
        portionPreferenceDao.upsert(PortionPreferenceEntity(foodId = foodId, portionGrams = portionGrams))
    }
}

class DatabaseNutritionRepository(
    private val mealLogDao: MealLogDao,
    private val profileDao: ProfileDao,
    private val mealHistoryDao: MealHistoryDao,
    private val ingredientHistoryDao: IngredientHistoryDao,
    private val allFoodDao: AllFoodDao,
) : NutritionRepository {
    override fun observeMeals(): Flow<List<Meal>> =
        mealLogDao.observeAll().map { rows -> rows.map { it.toMeal() } }

    override fun observeDailySummary(): Flow<DailySummary> =
        observeDailySummarySince(0)

    override fun observeDailySummarySince(cutoff: Long): Flow<DailySummary> =
        // Daily totals come from meal history because it reflects edits, deletions, and final logged portions.
        mealHistoryDao.observeSince(cutoff).map { rows ->
            val profile = profileDao.get()?.toDomain() ?: UserProfile()
            val totalCalories = rows.sumOf { it.calories }
            val totalProtein = rows.sumOf { it.proteinGrams }
            val totalCarbs = rows.sumOf { it.carbsGrams }
            val totalFat = rows.sumOf { it.fatGrams }
            DailySummary(
                consumedCalories = totalCalories,
                targetCalories = profile.targetCalories,
                consumedProteinGrams = totalProtein,
                targetProteinGrams = profile.targetProtein,
                consumedCarbsGrams = totalCarbs,
                targetCarbsGrams = profile.targetCarbs,
                consumedFatGrams = totalFat,
                targetFatGrams = profile.targetFat,
                proteinProgress = if (profile.targetProtein > 0) totalProtein / profile.targetProtein.toFloat() else 0f,
                carbsProgress = if (profile.targetCarbs > 0) totalCarbs / profile.targetCarbs.toFloat() else 0f,
                fatProgress = if (profile.targetFat > 0) totalFat / profile.targetFat.toFloat() else 0f,
                waterMl = 0,
            )
        }

    override fun observeMealHistorySince(cutoff: Long): Flow<List<Meal>> =
        mealHistoryDao.observeSince(cutoff).map { rows -> rows.map { it.toMeal() } }

    override suspend fun getHistoryMealDetails(mealId: Long): MealDetails {
        val meal = mealHistoryDao.findById(mealId) ?: throw NoSuchElementException("History meal with id $mealId not found")
        val ingredients = ingredientHistoryDao.findByMealId(mealId)
        var details = meal.toMealDetails(ingredients)
        val sourceFood = meal.sourceFdcId?.let { allFoodDao.findById(it) }
        if (sourceFood != null) {
            details = details.copy(
                secondaryNutrients = sourceFood.toSecondaryNutrients(),
                isBookmarked = sourceFood.saved,
            )
        }
        return details
    }

    override suspend fun logHistory(input: com.griffith.valuetracker.data.repository.LoggedMealInput) {
        val historyId = mealHistoryDao.insert(
            MealHistoryEntity(
                sourceFdcId = input.sourceFdcId,
                sourceRecipeId = input.sourceRecipeId,
                displayName = input.displayName,
                loggedAtEpochMillis = System.currentTimeMillis(),
                portionGrams = input.portionGrams,
                calories = input.calories,
                proteinGrams = input.proteinGrams,
                carbsGrams = input.carbsGrams,
                fatGrams = input.fatGrams,
                mealTypeLabel = input.mealTypeLabel,
                imageUrl = input.imageUrl,
            )
        )
        ingredientHistoryDao.insertAll(
            input.ingredients.mapIndexed { index, ingredient ->
                IngredientHistoryEntity(
                    historyId = historyId,
                    position = index,
                    ingredientTitle = ingredient.ingredientTitle,
                    ingredientFdcId = ingredient.ingredientFdcId,
                    normalizedWeightGrams = ingredient.normalizedWeightGrams,
                    calories = ingredient.calories,
                    proteinGrams = ingredient.proteinGrams,
                    carbsGrams = ingredient.carbsGrams,
                    fatGrams = ingredient.fatGrams,
                )
            }
        )
    }

    override suspend fun updateHistoryMeal(meal: Meal, ingredients: List<LoggedIngredientInput>, portionGrams: Float) {
        val existing = mealHistoryDao.findById(meal.id) ?: return
        mealHistoryDao.update(
            existing.copy(
                displayName = meal.title,
                portionGrams = portionGrams,
                calories = meal.calories,
                proteinGrams = meal.proteinGrams,
                carbsGrams = meal.carbsGrams,
                fatGrams = meal.fatGrams,
                mealTypeLabel = meal.mealType,
                imageUrl = meal.imageUrl,
            )
        )
        ingredientHistoryDao.deleteByMealId(meal.id)
        ingredientHistoryDao.insertAll(
            ingredients.mapIndexed { index, ingredient ->
                IngredientHistoryEntity(
                    historyId = meal.id,
                    position = index,
                    ingredientTitle = ingredient.ingredientTitle,
                    ingredientFdcId = ingredient.ingredientFdcId,
                    normalizedWeightGrams = ingredient.normalizedWeightGrams,
                    calories = ingredient.calories,
                    proteinGrams = ingredient.proteinGrams,
                    carbsGrams = ingredient.carbsGrams,
                    fatGrams = ingredient.fatGrams,
                )
            }
        )
    }

    override suspend fun deleteHistoryMeal(mealId: Long) {
        ingredientHistoryDao.deleteByMealId(mealId)
        mealHistoryDao.deleteById(mealId)
    }
}

class DatabaseProfileRepository(
    private val profileDao: ProfileDao,
) : ProfileRepository {
    override fun observeUserProfile(): Flow<UserProfile> = flow {
        emit(profileDao.get()?.toDomain() ?: UserProfile())
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        profileDao.upsert(profile.toEntity())
    }
}

class UserPrefsDataStore(private val context: Context) {
    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ONBOARDING_ANSWERS = stringPreferencesKey("onboarding_answers")
    }

    fun isOnboardingCompleted(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveOnboardingAnswers(answers: OnboardingAnswer) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_ANSWERS] = Json.encodeToString(answers)
        }
    }

    fun getOnboardingAnswers(): Flow<OnboardingAnswer?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_ANSWERS]?.let { json ->
            try {
                Json.decodeFromString<OnboardingAnswer>(json)
            } catch (_: Exception) {
                null
            }
        }
    }
}

class SeedDataInitializer(
    private val context: Context,
    private val databaseStorage: DatabaseStorage,
) {
    suspend fun seedIfEmpty() {
        val baseDir = File(context.filesDir, "food_db_assets")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        copyAsset("food.csv", baseDir)
        copyAsset("food_nutrient.csv", baseDir)
        copyAsset("nutrient.csv", baseDir)
        copyAsset("food_portion.csv", baseDir)
        copyAsset("branded_food.csv", baseDir)
        seedDatabaseIfEmpty(databaseStorage, baseDir)
    }

    private fun copyAsset(name: String, baseDir: File) {
        val target = baseDir.resolve(name)
        if (target.exists()) return
        context.assets.open("food_db/$name").use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }
}

suspend fun seedDatabaseIfEmpty(database: DatabaseStorage, baseDir: File) {
    if (database.foodItemDao().countRows() == 0) {
        val foods = loadFoods(baseDir.resolve("food.csv"), 5000)
        val targetIds = foods.mapTo(linkedSetOf()) { it.fdcId }
        val nutrientLookup = loadNutrientLookup(baseDir.resolve("nutrient.csv"))
        val nutrientNamesByKey = nutrientLookup.entries.associate { it.value to it.key }
        val nutrientRows = nutrientLookup.map { (id, key) ->
            NutrientEntity(
                id = id,
                name = when (key) {
                    "calories" -> "Energy"
                    "protein" -> "Protein"
                    "carbs" -> "Carbohydrate, by difference"
                    else -> "Total lipid (fat)"
                },
                unitName = if (key == "calories") "KCAL" else "G",
                nutrientNumber = id.toString(),
            )
        }
        val nutrientAmounts = loadFoodNutrients(baseDir.resolve("food_nutrient.csv"), nutrientLookup, targetIds)
        val portionWeights = loadPortions(baseDir.resolve("food_portion.csv"), targetIds)

        database.foodItemDao().upsertAll(foods.map { FoodItemEntity(it.fdcId, it.name, it.category, it.dataType) })
        database.nutrientDao().upsertAll(nutrientRows)
        database.foodNutrientDao().upsertAll(
            nutrientAmounts.flatMap { (fdcId, nutrients) ->
                nutrients.mapNotNull { (key, amount) ->
                    nutrientNamesByKey[key]?.let { nutrientId ->
                        FoodNutrientEntity(fdcId = fdcId, nutrientId = nutrientId, amount = amount)
                    }
                }
            },
        )
        database.foodPortionDao().upsertAll(
            portionWeights.map { (fdcId, gramWeight) ->
                FoodPortionEntity(id = fdcId, fdcId = fdcId, gramWeight = gramWeight)
            },
        )
        database.allFoodDao().upsertAll(
            foods.map { food ->
                val nutrients = nutrientAmounts[food.fdcId].orEmpty()
                AllFoodEntity(
                    fdcId = food.fdcId,
                    foodName = food.name,
                    defaultPortionGrams = portionWeights[food.fdcId],
                    caloriesKcal = nutrients["calories"] ?: 0f,
                    proteinGrams = nutrients["protein"] ?: 0f,
                    carbsGrams = nutrients["carbs"] ?: 0f,
                    fatGrams = nutrients["fat"] ?: 0f,
                    fiberGrams = nutrients["fiber"] ?: 0f,
                    totalSugarsGrams = nutrients["totalSugars"] ?: 0f,
                    starchGrams = nutrients["starch"] ?: 0f,
                    calciumMg = nutrients["calcium"] ?: 0f,
                    ironMg = nutrients["iron"] ?: 0f,
                    magnesiumMg = nutrients["magnesium"] ?: 0f,
                    phosphorusMg = nutrients["phosphorus"] ?: 0f,
                    potassiumMg = nutrients["potassium"] ?: 0f,
                    sodiumMg = nutrients["sodium"] ?: 0f,
                    zincMg = nutrients["zinc"] ?: 0f,
                    seleniumUg = nutrients["selenium"] ?: 0f,
                    caffeineMg = nutrients["caffeine"] ?: 0f,
                    alcoholGrams = nutrients["alcohol"] ?: 0f,
                    vitaminAiu = nutrients["vitaminAiu"] ?: 0f,
                    vitaminCmg = nutrients["vitaminCmg"] ?: 0f,
                    vitaminDug = nutrients["vitaminDug"] ?: 0f,
                    vitaminEmg = nutrients["vitaminEmg"] ?: 0f,
                    vitaminKug = nutrients["vitaminKug"] ?: 0f,
                    thiaminMg = nutrients["thiaminMg"] ?: 0f,
                    riboflavinMg = nutrients["riboflavinMg"] ?: 0f,
                    niacinMg = nutrients["niacinMg"] ?: 0f,
                    vitaminB6Mg = nutrients["vitaminB6Mg"] ?: 0f,
                    folateUg = nutrients["folateUg"] ?: 0f,
                    vitaminB12Ug = nutrients["vitaminB12Ug"] ?: 0f,
                    pantothenicAcidMg = nutrients["pantothenicAcidMg"] ?: 0f,
                    cholineMg = nutrients["cholineMg"] ?: 0f,
                    omega3Grams = nutrients["omega3"] ?: 0f,
                    omega6Grams = nutrients["omega6"] ?: 0f,
                    monounsaturatedFatGrams = nutrients["monounsaturatedFat"] ?: 0f,
                    polyunsaturatedFatGrams = nutrients["polyunsaturatedFat"] ?: 0f,
                    saturatedFatGrams = nutrients["saturatedFat"] ?: 0f,
                    saved = false,
                )
            },
        )
    }
}

private fun FoodSearchRow.toMeal() = Meal(
    id = fdcId,
    title = name,
    calories = calories.toInt(),
    proteinGrams = parseNutrientAmount(nutrientsSummary, "Protein").toInt(),
    carbsGrams = parseNutrientAmount(nutrientsSummary, "Carbohydrate, by difference").toInt(),
    fatGrams = parseNutrientAmount(nutrientsSummary, "Total lipid (fat)").toInt(),
    mealType = gramWeight?.let { "$it g" } ?: "100 g",
    loggedAtEpochMillis = 0,
)

private fun AllFoodEntity.toMeal() = Meal(
    id = fdcId,
    title = foodName,
    calories = caloriesKcal.toInt(),
    proteinGrams = proteinGrams.toInt(),
    carbsGrams = carbsGrams.toInt(),
    fatGrams = fatGrams.toInt(),
    mealType = defaultPortionGrams?.let { "$it g" } ?: "100 g",
    loggedAtEpochMillis = 0,
)

private fun AllFoodEntity.toSecondaryNutrients(): List<SecondaryNutrient> = listOfNotNull(
    secondaryNutrient("Fiber", fiberGrams, "g"),
    secondaryNutrient("Sugars", totalSugarsGrams, "g"),
    secondaryNutrient("Starch", starchGrams, "g"),
    secondaryNutrient("Calcium", calciumMg, "mg"),
    secondaryNutrient("Iron", ironMg, "mg"),
    secondaryNutrient("Magnesium", magnesiumMg, "mg"),
    secondaryNutrient("Phosphorus", phosphorusMg, "mg"),
    secondaryNutrient("Potassium", potassiumMg, "mg"),
    secondaryNutrient("Sodium", sodiumMg, "mg"),
    secondaryNutrient("Zinc", zincMg, "mg"),
    secondaryNutrient("Selenium", seleniumUg, "ug"),
    secondaryNutrient("Caffeine", caffeineMg, "mg"),
    secondaryNutrient("Alcohol", alcoholGrams, "g"),
    secondaryNutrient("Vitamin A", vitaminAiu, "IU"),
    secondaryNutrient("Vitamin C", vitaminCmg, "mg"),
    secondaryNutrient("Vitamin D", vitaminDug, "IU"),
    secondaryNutrient("Vitamin E", vitaminEmg, "mg"),
    secondaryNutrient("Vitamin K", vitaminKug, "ug"),
    secondaryNutrient("Thiamin", thiaminMg, "mg"),
    secondaryNutrient("Riboflavin", riboflavinMg, "mg"),
    secondaryNutrient("Niacin", niacinMg, "mg"),
    secondaryNutrient("Vitamin B6", vitaminB6Mg, "mg"),
    secondaryNutrient("Folate", folateUg, "ug"),
    secondaryNutrient("Vitamin B12", vitaminB12Ug, "ug"),
    secondaryNutrient("Pantothenic Acid", pantothenicAcidMg, "mg"),
    secondaryNutrient("Choline", cholineMg, "mg"),
    secondaryNutrient("Omega 3", omega3Grams, "g"),
    secondaryNutrient("Omega 6", omega6Grams, "g"),
    secondaryNutrient("Mono Fat", monounsaturatedFatGrams, "g"),
    secondaryNutrient("Poly Fat", polyunsaturatedFatGrams, "g"),
    secondaryNutrient("Sat Fat", saturatedFatGrams, "g"),
)

private fun secondaryNutrient(label: String, amount: Float, unit: String): SecondaryNutrient? =
    if (amount <= 0f) null else SecondaryNutrient(label, "$amount $unit")

private fun SavedRecipeEntity.toMeal() = Meal(
    id = recipeId,
    title = recipeName,
    calories = 0,
    proteinGrams = 0,
    carbsGrams = 0,
    fatGrams = 0,
    mealType = basePortionGrams?.let { "$it g" } ?: "100 g",
    loggedAtEpochMillis = updatedAtEpochMillis,
)

private fun MealHistoryEntity.toMeal() = Meal(
    id = historyId,
    title = displayName,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    mealType = mealTypeLabel,
    loggedAtEpochMillis = loggedAtEpochMillis,
    imageUrl = imageUrl,
)

private fun MealHistoryEntity.toMealDetails(ingredients: List<IngredientHistoryEntity>) = MealDetails(
    id = historyId,
    title = displayName,
    baseCalories = calories,
    baseProteinGrams = proteinGrams,
    baseCarbsGrams = carbsGrams,
    baseFatGrams = fatGrams,
    mealType = portionGrams.toPortionLabel(),
    servings = 1f,
    hasOil = false,
    addedOilGrams = 0,
    isBookmarked = false,
    imageUrl = imageUrl,
    ingredients = ingredients.map { it.toIngredient() },
)

private fun IngredientHistoryEntity.toIngredient() = Ingredient(
    name = ingredientTitle,
    baseGrams = normalizedWeightGrams.toInt(),
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
)

private fun Float.toPortionLabel(): String =
    if (this % 1f == 0f) "${this.toInt()} g" else "$this g"

private fun MealLogEntity.toMeal() = Meal(
    id = id,
    title = title,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    mealType = mealType,
    loggedAtEpochMillis = loggedAtEpochMillis,
)

private fun MealDetailEntity.toMeal() = Meal(
    id = mealId,
    title = title,
    calories = baseCalories,
    proteinGrams = baseProteinGrams,
    carbsGrams = baseCarbsGrams,
    fatGrams = baseFatGrams,
    mealType = mealType,
    loggedAtEpochMillis = mealId,
)

private fun MealDetailEntity.toMealDetails(ingredients: List<Ingredient>) = MealDetails(
    id = mealId,
    title = title,
    baseCalories = baseCalories,
    baseProteinGrams = baseProteinGrams,
    baseCarbsGrams = baseCarbsGrams,
    baseFatGrams = baseFatGrams,
    mealType = mealType,
    servings = servings,
    hasOil = hasOil,
    addedOilGrams = addedOilGrams,
    isBookmarked = isBookmarked,
    ingredients = ingredients,
)

private fun MealIngredientEntity.toIngredient() = Ingredient(
    name = name,
    baseGrams = baseGrams,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
)

private fun UserProfileEntity.toDomain() = UserProfile(
    firstName = firstName,
    goalType = goalType,
    targetCalories = targetCalories,
    targetProtein = targetProtein,
    targetCarbs = targetCarbs,
    targetFat = targetFat,
    heightCm = heightCm,
    weightKg = weightKg,
    age = age,
)

private fun UserProfile.toEntity() = UserProfileEntity(
    id = 1,
    firstName = firstName,
    goalType = goalType,
    targetCalories = targetCalories,
    targetProtein = targetProtein,
    targetCarbs = targetCarbs,
    targetFat = targetFat,
    heightCm = heightCm,
    weightKg = weightKg,
    age = age,
)

fun parseNutrientAmount(summary: String, nutrientName: String): Float {
    // Search rows flatten nutrients into one string, so detail hydration has to recover exact values by label.
    val prefix = "$nutrientName: "
    val segment = summary.split(" | ").firstOrNull { it.startsWith(prefix) } ?: return 0f
    return segment.removePrefix(prefix).substringBefore(' ').toFloatOrNull() ?: 0f
}

private fun parseCsvRow(line: String): List<String> {
    // USDA CSV files contain quoted commas, so naive split(',') would corrupt names and nutrients.
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var index = 0

    while (index < line.length) {
        val char = line[index]
        when {
            char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                current.append('"')
                index++
            }
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                values += current.toString()
                current.clear()
            }
            else -> current.append(char)
        }
        index++
    }

    values += current.toString()
    return values
}
