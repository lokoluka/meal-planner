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

    companion object {
        private val pendingRecipeDeletes = mutableSetOf<Long>()

        fun markPendingRecipeDelete(recipeId: Long) {
            pendingRecipeDeletes.add(recipeId)
        }

        fun clearPendingRecipeDelete(recipeId: Long) {
            pendingRecipeDeletes.remove(recipeId)
        }

        fun isPendingRecipeDelete(recipeId: Long): Boolean {
            return pendingRecipeDeletes.contains(recipeId)
        }
    }
    
    private fun getUserId(): String? = auth.currentUser?.uid
    
    // Sync Recipes
    suspend fun syncRecipes() {
        val userId = getUserId() ?: return
        Log.d("FirebaseSync", "Starting syncRecipes for user=$userId")
        
        try {
            // Upload local recipes to Firebase
            val localRecipes = recipeDao.getRecipesWithIngredients()
            Log.d("FirebaseSync", "Uploading ${localRecipes.size} local recipes for user=$userId")
            localRecipes.forEach { recipeWithIngredients ->
                if (isPendingRecipeDelete(recipeWithIngredients.recipe.recipeId)) {
                    Log.d("FirebaseSync", "Skipping upload for pending delete recipeId=${recipeWithIngredients.recipe.recipeId}")
                } else {
                    uploadRecipe(userId, recipeWithIngredients)
                    Log.d("FirebaseSync", "Uploaded recipe ${recipeWithIngredients.recipe.name} (id=${recipeWithIngredients.recipe.recipeId}) for user=$userId")
                }
            }
            
            // Download recipes from Firebase
            downloadRecipes(userId)
            Log.d("FirebaseSync", "Completed syncRecipes for user=$userId")
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
    
    suspend fun deleteRecipeFromFirebase(userId: String, recipeId: Long): Boolean {
        try {
            Log.d("FirebaseSync", "Attempting to delete recipe with ID: $recipeId from Firebase for user: $userId")
            firestore.collection("users")
                .document(userId)
                .collection("recipes")
                .document(recipeId.toString())
                .delete()
                .await()
            Log.d("FirebaseSync", "Successfully deleted recipe with ID: $recipeId from Firebase for user: $userId")
            return true
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting recipe with ID: $recipeId from Firebase for user: $userId", e)
            return false
        }
    }
    
    private suspend fun downloadRecipes(userId: String) {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("recipes")
            .get()
            .await()

        val firebaseRecipeIds = mutableSetOf<Long>()
        val existingRecipes = recipeDao.getRecipesWithIngredients()
        val recipesById = existingRecipes.associateBy { it.recipe.recipeId }
        val recipesByName = existingRecipes.associateBy { it.recipe.name.trim().lowercase() }

        snapshot.documents.forEach { doc ->
            try {
                val firebaseRecipeId = doc.getLong("recipeId")
                val name = doc.getString("name") ?: return@forEach
                val instructions = doc.getString("instructions") ?: ""
                val servings = doc.getLong("servings")?.toInt() ?: 1
                val nameKey = name.trim().lowercase()

                if (firebaseRecipeId != null) {
                    firebaseRecipeIds.add(firebaseRecipeId)
                }

                val recipeIdToUse = when {
                    firebaseRecipeId != null -> firebaseRecipeId
                    recipesByName.containsKey(nameKey) -> recipesByName.getValue(nameKey).recipe.recipeId
                    else -> 0L
                }

                Log.d("FirebaseSync", "Upserting recipe from Firebase: name=$name, firebaseId=$firebaseRecipeId, localId=$recipeIdToUse")

                val effectiveRecipeId = if (recipeIdToUse != 0L) {
                    recipeDao.updateRecipe(
                        Recipe(
                            recipeId = recipeIdToUse,
                            name = name,
                            instructions = instructions,
                            servings = servings
                        )
                    )
                    Log.d("FirebaseSync", "Updated existing recipe with ID: $recipeIdToUse for name: $name")
                    recipeIdToUse
                } else {
                    val insertedId = recipeDao.insertRecipe(
                        Recipe(
                            name = name,
                            instructions = instructions,
                            servings = servings
                        )
                    )
                    Log.d("FirebaseSync", "Inserted new recipe for name: $name with generated ID=$insertedId")
                    insertedId
                }

                if (recipeIdToUse != 0L || recipesById.containsKey(effectiveRecipeId)) {
                    recipeDao.deleteRecipeIngredients(effectiveRecipeId)
                }

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
                    val categoryName = ingData["category"] as? String
                    val category = try {
                        if (categoryName != null) IngredientCategory.valueOf(categoryName) else IngredientCategory.OTHER
                    } catch (e: Exception) {
                        IngredientCategory.OTHER
                    }

                    // Get or create ingredient
                    var ingredientId = recipeDao.getIngredientByName(ingName)?.ingredientId
                    if (ingredientId == null) {
                        ingredientId = recipeDao.insertIngredient(
                            Ingredient(name = ingName, defaultUnit = unit, category = category)
                        )
                    } else {
                        recipeDao.updateIngredient(
                            Ingredient(ingredientId = ingredientId, name = ingName, defaultUnit = unit, category = category)
                        )
                    }

                    recipeDao.insertRecipeIngredientCrossRef(
                        RecipeIngredientCrossRef(effectiveRecipeId, ingredientId, amount, unit)
                    )
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error downloading recipe", e)
            }
        }

        // Remove local recipes not present in Firebase
        val localRecipes = recipeDao.getRecipesWithIngredients()
        localRecipes.forEach { localRecipe ->
            if (!firebaseRecipeIds.contains(localRecipe.recipe.recipeId)) {
                Log.d("FirebaseSync", "Removing local recipe ${localRecipe.recipe.name} not in Firebase")
                recipeDao.deleteRecipe(localRecipe.recipe)
            }
        }
    }
    
    // Sync Weekly Plans
    suspend fun syncWeeklyPlans() {
        val userId = getUserId() ?: return
        Log.d("FirebaseSync", "Starting syncWeeklyPlans for user=$userId")
        
        try {
            // Upload local weekly plans
            val localPlans = mealPlanDao.getAllWeeklyPlans()
            Log.d("FirebaseSync", "Uploading ${localPlans.size} weekly plans for user=$userId")
            localPlans.forEach { plan ->
                uploadWeeklyPlan(userId, plan)
            }
            
            // Download weekly plans from Firebase
            downloadWeeklyPlans(userId)
            // Download weekly plans shared with the user's families
            downloadFamilyWeeklyPlans(userId)
            Log.d("FirebaseSync", "Completed syncWeeklyPlans for user=$userId")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing weekly plans", e)
        }
    }

    /**
     * Delete weekly plan from Firebase. Tries to remove by id and by matching
     * name/startDate across user's collection and family's collections.
     */
    suspend fun deleteWeeklyPlanFromFirebase(userId: String, plan: com.lokosoft.mealplanner.data.WeeklyPlan) {
        try {
            // Delete by doc id (if it was uploaded with local id as doc id)
            firestore.collection("users")
                .document(userId)
                .collection("weeklyPlans")
                .document(plan.weeklyPlanId.toString())
                .delete()
                .await()
            Log.d("FirebaseSync", "Deleted user weeklyPlan doc id=${plan.weeklyPlanId} for user=$userId (by id)")
        } catch (e: Exception) {
            Log.d("FirebaseSync", "No user doc deleted by id for weeklyPlanId=${plan.weeklyPlanId}: ${e.message}")
        }

        try {
            // Also attempt to delete by matching name and startDate in user's collection
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("weeklyPlans")
                .whereEqualTo("name", plan.name)
                .whereEqualTo("startDate", plan.startDate)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                try {
                    doc.reference.delete().await()
                    Log.d("FirebaseSync", "Deleted user weeklyPlan doc ${doc.id} by name/startDate for user=$userId")
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Failed to delete user weeklyPlan doc ${doc.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error querying user weeklyPlans for deletion", e)
        }

        // Delete from family collections
        try {
            val familyIds = mealPlanDao.getFamilyIdsForWeeklyPlan(plan.weeklyPlanId)
            familyIds.forEach { familyId ->
                try {
                    firestore.collection("families")
                        .document(familyId.toString())
                        .collection("weeklyPlans")
                        .document(plan.weeklyPlanId.toString())
                        .delete()
                        .await()
                    Log.d("FirebaseSync", "Deleted family weeklyPlan doc id=${plan.weeklyPlanId} in family=$familyId (by id)")
                } catch (e: Exception) {
                    Log.d("FirebaseSync", "No family doc deleted by id for weeklyPlanId=${plan.weeklyPlanId} in family=$familyId: ${e.message}")
                }

                try {
                    val snapFam = firestore.collection("families")
                        .document(familyId.toString())
                        .collection("weeklyPlans")
                        .whereEqualTo("name", plan.name)
                        .whereEqualTo("startDate", plan.startDate)
                        .get()
                        .await()

                    snapFam.documents.forEach { doc ->
                        try {
                            doc.reference.delete().await()
                            Log.d("FirebaseSync", "Deleted family weeklyPlan doc ${doc.id} by name/startDate in family=$familyId")
                        } catch (e: Exception) {
                            Log.e("FirebaseSync", "Failed to delete family weeklyPlan doc ${doc.id} in family=$familyId", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Error querying family weeklyPlans for deletion in family=$familyId", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error fetching family ids for weeklyPlanId=${plan.weeklyPlanId}", e)
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
                        val recipeId = (mealData["recipeId"] as? Number)?.toLong()
                        val recipeName = mealData["recipeName"] as? String
                        val dayOfWeekName = mealData["dayOfWeek"] as? String ?: return@forEach
                        val mealTypeName = mealData["mealType"] as? String ?: return@forEach
                        val servings = (mealData["servings"] as? Number)?.toInt() ?: 1
                        val mealCommensals = (mealData["commensals"] as? Number)?.toInt() ?: 2
                        
                        // Find recipe by id (preferred), fallback to name
                        val localRecipes = recipeDao.getRecipesWithIngredients()
                        val recipe = when {
                            recipeId != null -> localRecipes.find { it.recipe.recipeId == recipeId }
                            recipeName != null -> localRecipes.find { it.recipe.name == recipeName }
                            else -> null
                        }
                        
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
                            val recipeId = (mealData["recipeId"] as? Number)?.toLong()
                            val recipeName = mealData["recipeName"] as? String
                            val dayOfWeekName = mealData["dayOfWeek"] as? String ?: return@forEach
                            val mealTypeName = mealData["mealType"] as? String ?: return@forEach
                            val servings = (mealData["servings"] as? Number)?.toInt() ?: 1
                            val mealCommensals = (mealData["commensals"] as? Number)?.toInt() ?: 2

                            // Find recipe by id (preferred), fallback to name
                            val localRecipes = recipeDao.getRecipesWithIngredients()
                            val recipe = when {
                                recipeId != null -> localRecipes.find { it.recipe.recipeId == recipeId }
                                recipeName != null -> localRecipes.find { it.recipe.name == recipeName }
                                else -> null
                            }

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
