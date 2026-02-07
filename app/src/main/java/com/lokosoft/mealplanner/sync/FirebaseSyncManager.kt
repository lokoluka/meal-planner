package com.lokosoft.mealplanner.sync

import android.util.Log
import com.lokosoft.mealplanner.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager(
    private val recipeDao: RecipeDao,
    private val mealPlanDao: MealPlanDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private fun getUserId(): String? = auth.currentUser?.uid
    
    // Sync Recipes
    suspend fun syncRecipes() {
        val userId = getUserId() ?: return
        
        try {
            // Upload local recipes to Firebase
            val localRecipes = recipeDao.getRecipesWithIngredients()
            localRecipes.forEach { recipeWithIngredients ->
                uploadRecipe(userId, recipeWithIngredients)
            }
            
            // Download recipes from Firebase
            downloadRecipes(userId)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing recipes", e)
        }
    }
    
    private suspend fun uploadRecipe(userId: String, recipeWithIngredients: RecipeWithIngredients) {
        val recipe = recipeWithIngredients.recipe
        val ingredients = recipeDao.getIngredientsForRecipe(recipe.recipeId)
        
        val recipeData = hashMapOf(
            "recipeId" to recipe.recipeId,
            "name" to recipe.name,
            "instructions" to recipe.instructions,
            "servings" to recipe.servings,
            "ingredients" to ingredients.map { ing ->
                hashMapOf(
                    "name" to ing.name,
                    "amount" to ing.amount,
                    "unit" to ing.unit.name
                )
            }
        )
        
        firestore.collection("users")
            .document(userId)
            .collection("recipes")
            .document(recipe.recipeId.toString())
            .set(recipeData, SetOptions.merge())
            .await()
    }
    
    suspend fun deleteRecipeFromFirebase(userId: String, recipeId: Long) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("recipes")
                .document(recipeId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting recipe from Firebase", e)
        }
    }
    
    private suspend fun downloadRecipes(userId: String) {
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
                        val unit = MeasurementUnit.valueOf(unitName)
                        
                        // Get or create ingredient
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
                Log.e("FirebaseSync", "Error downloading recipe", e)
            }
        }
    }
    
    // Sync Weekly Plans
    suspend fun syncWeeklyPlans() {
        val userId = getUserId() ?: return
        
        try {
            // Upload local weekly plans
            val localPlans = mealPlanDao.getAllWeeklyPlans()
            localPlans.forEach { plan ->
                uploadWeeklyPlan(userId, plan)
            }
            
            // Download weekly plans from Firebase
            downloadWeeklyPlans(userId)
            // Download weekly plans shared with the user's families
            downloadFamilyWeeklyPlans(userId)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing weekly plans", e)
        }
    }
    
    private suspend fun uploadWeeklyPlan(userId: String, plan: WeeklyPlan) {
        val mealPlans = mealPlanDao.getMealPlansByWeeklyPlan(plan.weeklyPlanId)
        val familyIds = mealPlanDao.getFamilyIdsForWeeklyPlan(plan.weeklyPlanId)
        
        val planData = hashMapOf(
            "name" to plan.name,
            "startDate" to plan.startDate,
            "commensals" to plan.commensals,
            "createdDate" to plan.createdDate,
            "userId" to plan.userId,
            "familyIds" to familyIds,
            "meals" to mealPlans.map { mealPlan ->
                hashMapOf(
                    "recipeId" to mealPlan.recipe.recipeId,
                    "recipeName" to mealPlan.recipe.name,
                    "dayOfWeek" to mealPlan.mealPlan.dayOfWeek.name,
                    "mealType" to mealPlan.mealPlan.mealType.name,
                    "servings" to mealPlan.mealPlan.servings,
                    "commensals" to mealPlan.mealPlan.commensals
                )
            }
        )
        
        // Upload to user's personal collection
        firestore.collection("users")
            .document(userId)
            .collection("weeklyPlans")
            .document(plan.weeklyPlanId.toString())
            .set(planData, SetOptions.merge())
            .await()
        
        // Also upload to each family's collection
        familyIds.forEach { familyId ->
            firestore.collection("families")
                .document(familyId.toString())
                .collection("weeklyPlans")
                .document(plan.weeklyPlanId.toString())
                .set(planData, SetOptions.merge())
                .await()
        }
    }
    
    private suspend fun downloadWeeklyPlans(userId: String) {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("weeklyPlans")
            .get()
            .await()
        
        snapshot.documents.forEach { doc ->
            try {
                val name = doc.getString("name") ?: return@forEach
                val startDate = doc.getLong("startDate") ?: return@forEach
                val commensals = doc.getLong("commensals")?.toInt() ?: 2
                val createdDate = doc.getLong("createdDate") ?: System.currentTimeMillis()
                val planUserId = doc.getString("userId") ?: userId
                
                // Check if plan already exists
                val existingPlans = mealPlanDao.getAllWeeklyPlans()
                val exists = existingPlans.any { it.name == name && it.startDate == startDate }
                
                if (!exists) {
                    val weeklyPlanId = mealPlanDao.insertWeeklyPlan(
                        WeeklyPlan(
                            name = name,
                            startDate = startDate,
                            commensals = commensals,
                            userId = planUserId,
                            createdDate = createdDate
                        )
                    )
                    
                    // Add family associations
                    @Suppress("UNCHECKED_CAST")
                    val familyIds = doc.get("familyIds") as? List<Long>
                    familyIds?.forEach { familyId ->
                        mealPlanDao.insertWeeklyPlanFamilyCrossRef(
                            WeeklyPlanFamilyCrossRef(weeklyPlanId, familyId)
                        )
                    }
                    
                    // Add meals
                    @Suppress("UNCHECKED_CAST")
                    val meals = doc.get("meals") as? List<HashMap<String, Any>>
                    meals?.forEach { mealData ->
                        val recipeName = mealData["recipeName"] as? String ?: return@forEach
                        val dayOfWeekName = mealData["dayOfWeek"] as? String ?: return@forEach
                        val mealTypeName = mealData["mealType"] as? String ?: return@forEach
                        val servings = (mealData["servings"] as? Number)?.toInt() ?: 1
                        val mealCommensals = (mealData["commensals"] as? Number)?.toInt() ?: 2
                        
                        // Find recipe by name
                        val localRecipes = recipeDao.getRecipesWithIngredients()
                        val recipe = localRecipes.find { it.recipe.name == recipeName }
                        
                        recipe?.let { recipeWithIngredients ->
                            mealPlanDao.insertMealPlan(
                                MealPlan(
                                    weeklyPlanId = weeklyPlanId,
                                    recipeId = recipeWithIngredients.recipe.recipeId,
                                    dayOfWeek = DayOfWeek.valueOf(dayOfWeekName),
                                    mealType = MealType.valueOf(mealTypeName),
                                    servings = servings,
                                    commensals = mealCommensals
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error downloading weekly plan", e)
            }
        }
    }

    private suspend fun downloadFamilyWeeklyPlans(userId: String) {
        val familiesSnapshot = firestore.collection("users")
            .document(userId)
            .collection("families")
            .get()
            .await()

        val familyIds = familiesSnapshot.documents.mapNotNull { doc ->
            doc.getLong("familyId") ?: doc.id.toLongOrNull()
        }

        familyIds.forEach { familyId ->
            val snapshot = firestore.collection("families")
                .document(familyId.toString())
                .collection("weeklyPlans")
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                try {
                    val name = doc.getString("name") ?: return@forEach
                    val startDate = doc.getLong("startDate") ?: return@forEach
                    val commensals = doc.getLong("commensals")?.toInt() ?: 2
                    val createdDate = doc.getLong("createdDate") ?: System.currentTimeMillis()
                    val planUserId = doc.getString("userId") ?: userId

                    val existingPlans = mealPlanDao.getAllWeeklyPlans()
                    val existingPlan = existingPlans.find { it.name == name && it.startDate == startDate }

                    val weeklyPlanId = if (existingPlan == null) {
                        mealPlanDao.insertWeeklyPlan(
                            WeeklyPlan(
                                name = name,
                                startDate = startDate,
                                commensals = commensals,
                                userId = planUserId,
                                createdDate = createdDate
                            )
                        )
                    } else {
                        existingPlan.weeklyPlanId
                    }

                    // Ensure cross-ref exists
                    mealPlanDao.insertWeeklyPlanFamilyCrossRef(
                        WeeklyPlanFamilyCrossRef(weeklyPlanId, familyId)
                    )

                    // Add meals if plan is new
                    if (existingPlan == null) {
                        @Suppress("UNCHECKED_CAST")
                        val meals = doc.get("meals") as? List<HashMap<String, Any>>
                        meals?.forEach { mealData ->
                            val recipeName = mealData["recipeName"] as? String ?: return@forEach
                            val dayOfWeekName = mealData["dayOfWeek"] as? String ?: return@forEach
                            val mealTypeName = mealData["mealType"] as? String ?: return@forEach
                            val servings = (mealData["servings"] as? Number)?.toInt() ?: 1
                            val mealCommensals = (mealData["commensals"] as? Number)?.toInt() ?: 2

                            // Find recipe by name
                            val localRecipes = recipeDao.getRecipesWithIngredients()
                            val recipe = localRecipes.find { it.recipe.name == recipeName }

                            recipe?.let { recipeWithIngredients ->
                                mealPlanDao.insertMealPlan(
                                    MealPlan(
                                        weeklyPlanId = weeklyPlanId,
                                        recipeId = recipeWithIngredients.recipe.recipeId,
                                        dayOfWeek = DayOfWeek.valueOf(dayOfWeekName),
                                        mealType = MealType.valueOf(mealTypeName),
                                        servings = servings,
                                        commensals = mealCommensals
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Error downloading family weekly plan", e)
                }
            }
        }
    }
    
    suspend fun syncAll() {
        syncRecipes()
        syncWeeklyPlans()
    }
}
