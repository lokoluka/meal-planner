package com.lokosoft.mealplanner.repository

import com.lokosoft.mealplanner.data.Ingredient
import com.lokosoft.mealplanner.data.IngredientAmount
import com.lokosoft.mealplanner.data.Recipe
import com.lokosoft.mealplanner.data.RecipeWithIngredients
import com.lokosoft.mealplanner.ui.recipe.IngredientWithAmount
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for recipe data operations.
 * Implementations can use local Room database or remote Firebase Firestore.
 */
interface RecipeRepository {
    
    /**
     * Get all recipes with their ingredients
     */
    suspend fun getAllRecipes(): List<RecipeWithIngredients>
    
    /**
     * Get all available ingredients
     */
    suspend fun getAllIngredients(): List<Ingredient>
    
    /**
     * Get a specific recipe by ID
     */
    suspend fun getRecipeById(recipeId: Long): RecipeWithIngredients?
    
    /**
     * Get ingredients for a specific recipe
     */
    suspend fun getIngredientsForRecipe(recipeId: Long): List<IngredientAmount>
    
    /**
     * Add a new recipe
     */
    suspend fun addRecipe(
        name: String,
        instructions: String,
        servings: Int,
        ingredients: List<IngredientWithAmount>
    ): Long
    
    /**
     * Update an existing recipe
     */
    suspend fun updateRecipe(
        recipeId: Long,
        name: String,
        instructions: String,
        servings: Int,
        ingredients: List<IngredientWithAmount>
    )
    
    /**
     * Delete a recipe
     */
    suspend fun deleteRecipe(recipe: Recipe)
    
    /**
     * Check if this repository supports real-time sync
     */
    fun supportsRealTimeSync(): Boolean = false
}
