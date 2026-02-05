package com.lokosoft.mealplanner.repository

import com.lokosoft.mealplanner.data.Ingredient
import com.lokosoft.mealplanner.data.IngredientAmount
import com.lokosoft.mealplanner.data.Recipe
import com.lokosoft.mealplanner.data.RecipeDao
import com.lokosoft.mealplanner.data.RecipeIngredientCrossRef
import com.lokosoft.mealplanner.data.RecipeWithIngredients
import com.lokosoft.mealplanner.ui.recipe.IngredientWithAmount

/**
 * Local implementation of RecipeRepository using Room database.
 * Used for guest users (anonymous auth) where data is stored only on device.
 */
class LocalRecipeRepository(
    private val recipeDao: RecipeDao
) : RecipeRepository {
    
    override suspend fun getAllRecipes(): List<RecipeWithIngredients> {
        return recipeDao.getRecipesWithIngredients()
    }
    
    override suspend fun getAllIngredients(): List<Ingredient> {
        return recipeDao.getAllIngredients()
    }
    
    override suspend fun getRecipeById(recipeId: Long): RecipeWithIngredients? {
        return recipeDao.getRecipeWithIngredientsById(recipeId)
    }
    
    override suspend fun getIngredientsForRecipe(recipeId: Long): List<IngredientAmount> {
        return recipeDao.getIngredientsForRecipe(recipeId)
    }
    
    override suspend fun addRecipe(
        name: String,
        instructions: String,
        servings: Int,
        ingredients: List<IngredientWithAmount>
    ): Long {
        val recipeId = recipeDao.insertRecipe(
            Recipe(
                name = name,
                instructions = instructions,
                servings = servings
            )
        )
        
        ingredients.forEach { ingredientWithAmount ->
            val existingIngredient = recipeDao.getIngredientByName(ingredientWithAmount.name)
            val ingredientId = if (existingIngredient == null) {
                recipeDao.insertIngredient(
                    Ingredient(
                        name = ingredientWithAmount.name,
                        defaultUnit = ingredientWithAmount.unit,
                        category = ingredientWithAmount.category
                    )
                )
            } else {
                recipeDao.updateIngredient(
                    existingIngredient.copy(category = ingredientWithAmount.category)
                )
                existingIngredient.ingredientId
            }
            
            recipeDao.insertRecipeIngredientCrossRef(
                RecipeIngredientCrossRef(
                    recipeId = recipeId,
                    ingredientId = ingredientId,
                    amount = ingredientWithAmount.amount,
                    unit = ingredientWithAmount.unit
                )
            )
        }
        
        return recipeId
    }
    
    override suspend fun updateRecipe(
        recipeId: Long,
        name: String,
        instructions: String,
        servings: Int,
        ingredients: List<IngredientWithAmount>
    ) {
        recipeDao.updateRecipe(
            Recipe(
                recipeId = recipeId,
                name = name,
                instructions = instructions,
                servings = servings
            )
        )
        
        // Delete old ingredient associations
        recipeDao.deleteRecipeIngredients(recipeId)
        
        // Add new ingredients
        ingredients.forEach { ingredientWithAmount ->
            val existingIngredient = recipeDao.getIngredientByName(ingredientWithAmount.name)
            val ingredientId = if (existingIngredient == null) {
                recipeDao.insertIngredient(
                    Ingredient(
                        name = ingredientWithAmount.name,
                        defaultUnit = ingredientWithAmount.unit,
                        category = ingredientWithAmount.category
                    )
                )
            } else {
                recipeDao.updateIngredient(
                    existingIngredient.copy(category = ingredientWithAmount.category)
                )
                existingIngredient.ingredientId
            }
            
            recipeDao.insertRecipeIngredientCrossRef(
                RecipeIngredientCrossRef(
                    recipeId = recipeId,
                    ingredientId = ingredientId,
                    amount = ingredientWithAmount.amount,
                    unit = ingredientWithAmount.unit
                )
            )
        }
    }
    
    override suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.deleteRecipe(recipe)
    }
    
    override fun supportsRealTimeSync(): Boolean = false
}
