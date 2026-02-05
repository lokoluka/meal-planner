package com.lokosoft.mealplanner.repository

import android.util.Log
import com.lokosoft.mealplanner.data.Ingredient
import com.lokosoft.mealplanner.data.IngredientAmount
import com.lokosoft.mealplanner.data.MeasurementUnit
import com.lokosoft.mealplanner.data.Recipe
import com.lokosoft.mealplanner.data.RecipeDao
import com.lokosoft.mealplanner.data.RecipeIngredientCrossRef
import com.lokosoft.mealplanner.data.RecipeWithIngredients
import com.lokosoft.mealplanner.ui.recipe.IngredientWithAmount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of RecipeRepository using Firestore.
 * Used for authenticated users where data is synced to the cloud.
 * Also maintains a local cache in Room for offline access.
 */
class FirebaseRecipeRepository(
    private val recipeDao: RecipeDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : RecipeRepository {
    
    private fun getUserId(): String? = auth.currentUser?.uid
    
    override suspend fun getAllRecipes(): List<RecipeWithIngredients> {
        // Try to fetch from Firebase first, fall back to local cache
        val userId = getUserId()
        if (userId != null) {
            try {
                syncFromFirebase(userId)
            } catch (e: Exception) {
                Log.e("FirebaseRecipeRepo", "Failed to sync from Firebase, using local cache", e)
            }
        }
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
        // Add to local database first
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
        
        // Sync to Firebase
        val userId = getUserId()
        if (userId != null) {
            try {
                val recipeData = hashMapOf(
                    "recipeId" to recipeId,
                    "name" to name,
                    "instructions" to instructions,
                    "servings" to servings,
                    "ingredients" to ingredients.map { ing ->
                        hashMapOf(
                            "name" to ing.name,
                            "amount" to ing.amount,
                            "unit" to ing.unit.name,
                            "category" to ing.category.name
                        )
                    }
                )
                
                firestore.collection("users")
                    .document(userId)
                    .collection("recipes")
                    .document(recipeId.toString())
                    .set(recipeData, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e("FirebaseRecipeRepo", "Failed to sync recipe to Firebase", e)
            }
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
        // Update local database
        recipeDao.updateRecipe(
            Recipe(
                recipeId = recipeId,
                name = name,
                instructions = instructions,
                servings = servings
            )
        )
        
        recipeDao.deleteRecipeIngredients(recipeId)
        
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
        
        // Sync to Firebase
        val userId = getUserId()
        if (userId != null) {
            try {
                val recipeData = hashMapOf(
                    "recipeId" to recipeId,
                    "name" to name,
                    "instructions" to instructions,
                    "servings" to servings,
                    "ingredients" to ingredients.map { ing ->
                        hashMapOf(
                            "name" to ing.name,
                            "amount" to ing.amount,
                            "unit" to ing.unit.name,
                            "category" to ing.category.name
                        )
                    }
                )
                
                firestore.collection("users")
                    .document(userId)
                    .collection("recipes")
                    .document(recipeId.toString())
                    .set(recipeData, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e("FirebaseRecipeRepo", "Failed to sync recipe update to Firebase", e)
            }
        }
    }
    
    override suspend fun deleteRecipe(recipe: Recipe) {
        // Delete from local database
        recipeDao.deleteRecipe(recipe)
        
        // Delete from Firebase
        val userId = getUserId()
        if (userId != null) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("recipes")
                    .document(recipe.recipeId.toString())
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("FirebaseRecipeRepo", "Failed to delete recipe from Firebase", e)
            }
        }
    }
    
    override fun supportsRealTimeSync(): Boolean = true
    
    /**
     * Sync recipes from Firebase to local database
     */
    private suspend fun syncFromFirebase(userId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("recipes")
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                try {
                    val firebaseRecipeId = doc.getLong("recipeId")
                    val name = doc.getString("name") ?: return@forEach
                    val instructions = doc.getString("instructions") ?: ""
                    val servings = doc.getLong("servings")?.toInt() ?: 1
                    
                    // Check if recipe already exists locally by recipeId (if available) or name
                    val existingRecipes = recipeDao.getRecipesWithIngredients()
                    val exists = if (firebaseRecipeId != null) {
                        existingRecipes.any { it.recipe.recipeId == firebaseRecipeId }
                    } else {
                        existingRecipes.any { it.recipe.name == name }
                    }
                    
                    if (!exists) {
                        val recipeId = recipeDao.insertRecipe(
                            Recipe(name = name, instructions = instructions, servings = servings)
                        )
                        
                        // Add ingredients
                        @Suppress("UNCHECKED_CAST")
                        val ingredients = doc.get("ingredients") as? List<HashMap<String, Any>>
                        ingredients?.forEach { ingData ->
                            val ingName = ingData["name"] as? String ?: return@forEach
                            val amount = (ingData["amount"] as? Number)?.toDouble() ?: 0.0
                            val unitName = ingData["unit"] as? String ?: "GRAM"
                            val unit = try {
                                MeasurementUnit.valueOf(unitName)
                            } catch (e: Exception) {
                                MeasurementUnit.GRAM
                            }
                            
                            var ingredientId = recipeDao.getIngredientByName(ingName)?.ingredientId
                            if (ingredientId == null) {
                                ingredientId = recipeDao.insertIngredient(Ingredient(name = ingName))
                            }
                            
                            recipeDao.insertRecipeIngredientCrossRef(
                                RecipeIngredientCrossRef(recipeId, ingredientId, amount, unit)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseRecipeRepo", "Error processing recipe document", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRecipeRepo", "Error syncing from Firebase", e)
            throw e
        }
    }
}
