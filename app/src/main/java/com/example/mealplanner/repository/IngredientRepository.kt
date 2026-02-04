package com.example.mealplanner.repository

import com.example.mealplanner.data.Ingredient
import com.example.mealplanner.data.IngredientCategory
import com.example.mealplanner.data.IngredientPackage
import com.example.mealplanner.data.IngredientWithPackages
import com.example.mealplanner.data.RecipeDao

/**
 * Repository for ingredient management operations
 */
class IngredientRepository(private val recipeDao: RecipeDao) {
    
    suspend fun getAllIngredients(): List<Ingredient> {
        return recipeDao.getAllIngredients()
    }
    
    suspend fun getAllIngredientsWithPackages(): List<IngredientWithPackages> {
        return recipeDao.getAllIngredientsWithPackages()
    }
    
    suspend fun getIngredientById(ingredientId: Long): Ingredient? {
        return recipeDao.getIngredientById(ingredientId)
    }
    
    suspend fun getIngredientWithPackages(ingredientId: Long): IngredientWithPackages? {
        return recipeDao.getIngredientWithPackages(ingredientId)
    }
    
    suspend fun searchIngredients(query: String): List<Ingredient> {
        return recipeDao.searchIngredients(query)
    }
    
    suspend fun getIngredientsByCategory(category: IngredientCategory): List<Ingredient> {
        return recipeDao.getIngredientsByCategory(category)
    }
    
    suspend fun addIngredient(ingredient: Ingredient): Long {
        return recipeDao.insertIngredient(ingredient)
    }
    
    suspend fun updateIngredient(ingredient: Ingredient) {
        recipeDao.updateIngredient(ingredient)
    }
    
    suspend fun deleteIngredient(ingredient: Ingredient) {
        recipeDao.deleteIngredient(ingredient)
    }
    
    // Package management
    suspend fun addPackage(ingredientPackage: IngredientPackage): Long {
        return recipeDao.insertIngredientPackage(ingredientPackage)
    }
    
    suspend fun updatePackage(ingredientPackage: IngredientPackage) {
        recipeDao.updateIngredientPackage(ingredientPackage)
    }
    
    suspend fun deletePackage(ingredientPackage: IngredientPackage) {
        recipeDao.deleteIngredientPackage(ingredientPackage)
    }
    
    suspend fun getPackagesForIngredient(ingredientId: Long): List<IngredientPackage> {
        return recipeDao.getPackagesForIngredient(ingredientId)
    }
    
    /**
     * Find potential duplicate ingredients based on name similarity
     */
    fun findPotentialDuplicates(ingredients: List<Ingredient>): List<Pair<Ingredient, Ingredient>> {
        val duplicates = mutableListOf<Pair<Ingredient, Ingredient>>()
        
        for (i in ingredients.indices) {
            for (j in i + 1 until ingredients.size) {
                val ing1 = ingredients[i]
                val ing2 = ingredients[j]
                
                // Check for similar names (fuzzy matching)
                if (areSimilar(ing1.name, ing2.name)) {
                    duplicates.add(Pair(ing1, ing2))
                }
            }
        }
        
        return duplicates
    }
    
    /**
     * Check if two ingredient names are similar (simple fuzzy matching)
     */
    private fun areSimilar(name1: String, name2: String): Boolean {
        val n1 = name1.lowercase().trim()
        val n2 = name2.lowercase().trim()
        
        // Exact match
        if (n1 == n2) return true
        
        // One contains the other
        if (n1.contains(n2) || n2.contains(n1)) return true
        
        // Check for common plurals
        val singularForms = listOf(
            Pair(n1, n1.removeSuffix("s")),
            Pair(n1, n1.removeSuffix("es")),
            Pair(n2, n2.removeSuffix("s")),
            Pair(n2, n2.removeSuffix("es"))
        )
        
        for ((original, singular) in singularForms) {
            if (singular.isNotEmpty() && original != singular) {
                if (n1 == singular || n2 == singular) return true
            }
        }
        
        return false
    }
}
