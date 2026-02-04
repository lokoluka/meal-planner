package com.example.mealplanner.repository

import android.util.Log
import com.example.mealplanner.data.RecipeDao
import com.example.mealplanner.data.RecipeWithIngredients
import com.example.mealplanner.ui.recipe.IngredientWithAmount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages migration of data from local storage to Firebase when a guest user
 * becomes an authenticated user (e.g., signs in with Google).
 */
class DataMigrationManager(
    private val recipeDao: RecipeDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    
    private val _migrationState = MutableStateFlow<MigrationState>(MigrationState.Idle)
    val migrationState: StateFlow<MigrationState> = _migrationState
    
    private val _migrationProgress = MutableStateFlow(0f)
    val migrationProgress: StateFlow<Float> = _migrationProgress
    
    sealed class MigrationState {
        object Idle : MigrationState()
        object InProgress : MigrationState()
        object Completed : MigrationState()
        data class Failed(val error: String) : MigrationState()
    }
    
    /**
     * Check if there is local data that needs migration
     */
    suspend fun hasLocalDataToMigrate(): Boolean = withContext(Dispatchers.IO) {
        try {
            val recipes = recipeDao.getRecipesWithIngredients()
            recipes.isNotEmpty()
        } catch (e: Exception) {
            Log.e("DataMigration", "Error checking local data", e)
            false
        }
    }
    
    /**
     * Migrate all local recipes to Firebase for the current user
     * @param clearLocalAfterMigration If true, clears local data after successful migration
     */
    suspend fun migrateLocalDataToFirebase(
        clearLocalAfterMigration: Boolean = false
    ): MigrationResult = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _migrationState.value = MigrationState.Failed("No authenticated user")
            return@withContext MigrationResult.Failed("User not authenticated")
        }
        
        try {
            _migrationState.value = MigrationState.InProgress
            _migrationProgress.value = 0f
            
            val localRecipes = recipeDao.getRecipesWithIngredients()
            
            if (localRecipes.isEmpty()) {
                _migrationState.value = MigrationState.Completed
                _migrationProgress.value = 1f
                return@withContext MigrationResult.Success(0, 0)
            }
            
            var successCount = 0
            var failureCount = 0
            val total = localRecipes.size.toFloat()
            
            localRecipes.forEachIndexed { index, recipeWithIngredients ->
                try {
                    migrateRecipe(userId, recipeWithIngredients)
                    successCount++
                } catch (e: Exception) {
                    Log.e("DataMigration", "Failed to migrate recipe: ${recipeWithIngredients.recipe.name}", e)
                    failureCount++
                }
                _migrationProgress.value = (index + 1) / total
            }
            
            if (clearLocalAfterMigration && successCount > 0 && failureCount == 0) {
                // Clear local recipes after successful migration
                localRecipes.forEach { recipeWithIngredients ->
                    try {
                        recipeDao.deleteRecipe(recipeWithIngredients.recipe)
                    } catch (e: Exception) {
                        Log.e("DataMigration", "Failed to clear local recipe", e)
                    }
                }
            }
            
            _migrationState.value = MigrationState.Completed
            _migrationProgress.value = 1f
            
            MigrationResult.Success(successCount, failureCount)
            
        } catch (e: Exception) {
            Log.e("DataMigration", "Migration failed", e)
            _migrationState.value = MigrationState.Failed(e.message ?: "Unknown error")
            _migrationProgress.value = 0f
            MigrationResult.Failed(e.message ?: "Migration failed")
        }
    }
    
    /**
     * Migrate a single recipe to Firebase
     */
    private suspend fun migrateRecipe(userId: String, recipeWithIngredients: RecipeWithIngredients) {
        val recipe = recipeWithIngredients.recipe
        val ingredients = recipeDao.getIngredientsForRecipe(recipe.recipeId)
        
        val recipeData = hashMapOf(
            "name" to recipe.name,
            "instructions" to recipe.instructions,
            "servings" to recipe.servings,
            "ingredients" to ingredients.map { ing ->
                hashMapOf(
                    "name" to ing.name,
                    "amount" to ing.amount,
                    "unit" to ing.unit.name
                )
            },
            "migratedAt" to System.currentTimeMillis()
        )
        
        firestore.collection("users")
            .document(userId)
            .collection("recipes")
            .document(recipe.name)
            .set(recipeData)
            .await()
    }
    
    /**
     * Reset migration state
     */
    fun resetMigrationState() {
        _migrationState.value = MigrationState.Idle
        _migrationProgress.value = 0f
    }
    
    sealed class MigrationResult {
        data class Success(val successCount: Int, val failureCount: Int) : MigrationResult()
        data class Failed(val message: String) : MigrationResult()
    }
}
