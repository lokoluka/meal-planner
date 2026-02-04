package com.example.mealplanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("SELECT * FROM recipes WHERE recipeId = :recipeId")
    suspend fun getRecipeById(recipeId: Long): Recipe?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIngredient(ingredient: Ingredient): Long

    @Update
    suspend fun updateIngredient(ingredient: Ingredient)

    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun getIngredientByName(name: String): Ingredient?

    @Query("SELECT * FROM ingredients WHERE ingredientId = :ingredientId")
    suspend fun getIngredientById(ingredientId: Long): Ingredient?

    @Query("SELECT * FROM ingredients")
    suspend fun getAllIngredients(): List<Ingredient>
    
    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)
    
    @Query("SELECT * FROM ingredients WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    suspend fun searchIngredients(searchQuery: String): List<Ingredient>
    
    @Query("SELECT * FROM ingredients WHERE category = :category ORDER BY name ASC")
    suspend fun getIngredientsByCategory(category: IngredientCategory): List<Ingredient>
    
    // Ingredient packages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredientPackage(ingredientPackage: IngredientPackage): Long
    
    @Update
    suspend fun updateIngredientPackage(ingredientPackage: IngredientPackage)
    
    @Delete
    suspend fun deleteIngredientPackage(ingredientPackage: IngredientPackage)
    
    @Query("SELECT * FROM ingredient_packages WHERE ingredientId = :ingredientId")
    suspend fun getPackagesForIngredient(ingredientId: Long): List<IngredientPackage>
    
    @Transaction
    @Query("SELECT * FROM ingredients WHERE ingredientId = :ingredientId")
    suspend fun getIngredientWithPackages(ingredientId: Long): IngredientWithPackages?
    
    @Transaction
    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    suspend fun getAllIngredientsWithPackages(): List<IngredientWithPackages>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredientCrossRef(crossRef: RecipeIngredientCrossRef)

    @Query("DELETE FROM recipe_ingredient_cross_ref WHERE recipeId = :recipeId")
    suspend fun deleteRecipeIngredients(recipeId: Long)

    @Transaction
    @Query("SELECT * FROM recipes")
    suspend fun getRecipesWithIngredients(): List<RecipeWithIngredients>

    @Transaction
    @Query("SELECT * FROM recipes WHERE recipeId = :recipeId")
    suspend fun getRecipeWithIngredientsById(recipeId: Long): RecipeWithIngredients?

    @Transaction
    @Query("SELECT ingredients.ingredientId, ingredients.name, recipe_ingredient_cross_ref.amount, recipe_ingredient_cross_ref.unit FROM recipe_ingredient_cross_ref INNER JOIN ingredients ON recipe_ingredient_cross_ref.ingredientId = ingredients.ingredientId WHERE recipe_ingredient_cross_ref.recipeId = :recipeId")
    suspend fun getIngredientsForRecipe(recipeId: Long): List<IngredientAmount>
}
