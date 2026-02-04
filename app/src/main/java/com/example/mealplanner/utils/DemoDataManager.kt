package com.example.mealplanner.utils

import android.app.Application
import com.example.mealplanner.R
import com.example.mealplanner.data.AppDatabase
import com.example.mealplanner.data.DayOfWeek
import com.example.mealplanner.data.IngredientCategory
import com.example.mealplanner.data.MealPlan
import com.example.mealplanner.data.MealType
import com.example.mealplanner.data.MeasurementUnit
import com.example.mealplanner.data.Recipe
import com.example.mealplanner.data.RecipeDao
import com.example.mealplanner.data.WeeklyPlan
import com.example.mealplanner.repository.RecipeRepository
import com.example.mealplanner.ui.recipe.IngredientWithAmount
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Utility class to create demo/sample data for testing and demonstration purposes
 */
object DemoDataManager {
    
    // Recipe name resource IDs for demo data
    private val demoRecipeNameResIds = listOf(
        R.string.demo_recipe_pancakes,
        R.string.demo_recipe_grilled_chicken_salad,
        R.string.demo_recipe_pasta_bolognese,
        R.string.demo_recipe_grilled_salmon_rice,
        R.string.demo_recipe_vegetable_stir_fry,
        R.string.demo_recipe_beef_tacos,
        R.string.demo_recipe_mushroom_risotto,
        R.string.demo_recipe_baked_cod,
        R.string.demo_recipe_chicken_curry,
        R.string.demo_recipe_greek_salad,
        R.string.demo_recipe_pork_chops,
        R.string.demo_recipe_shrimp_pasta,
        R.string.demo_recipe_lentil_soup,
        R.string.demo_recipe_turkey_meatballs
    )
    
    suspend fun hasDemoData(application: Application, repository: RecipeRepository): Boolean = withContext(Dispatchers.IO) {
        val recipes = repository.getAllRecipes()
        val demoNames = demoRecipeNameResIds.map { application.getString(it) }
        demoNames.any { demoName ->
            recipes.any { it.recipe.name == demoName }
        }
    }
    
    suspend fun deleteDemoData(application: Application, repository: RecipeRepository) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(application)
        val mealPlanDao = database.mealPlanDao()
        
        // Delete demo weekly plan
        val weeklyPlanName = application.getString(R.string.demo_weekly_plan_name)
        val allPlans = mealPlanDao.getAllWeeklyPlans()
        allPlans.find { it.name == weeklyPlanName }?.let { demoPlan ->
            mealPlanDao.deleteWeeklyPlan(demoPlan)
        }
        
        // Delete demo recipes
        val demoNames = demoRecipeNameResIds.map { application.getString(it) }
        val recipes = repository.getAllRecipes()
        recipes.filter { recipeWithIngredients ->
            demoNames.contains(recipeWithIngredients.recipe.name)
        }.forEach { recipeWithIngredients ->
            repository.deleteRecipe(recipeWithIngredients.recipe)
        }
    }
    
    suspend fun createDemoData(application: Application, repository: RecipeRepository) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(application)
        val mealPlanDao = database.mealPlanDao()
        
        // Create 14 recipes for a balanced weekly menu
        val recipeIds = mutableListOf<Long>()
        
        // Recipe 1: Pancakes (Breakfast/Light Lunch)
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_pancakes),
            instructions = application.getString(R.string.demo_instructions_pancakes),
            servings = 4,
            ingredients = listOf(
                IngredientWithAmount("Flour", 250.0, MeasurementUnit.GRAM, IngredientCategory.PANTRY),
                IngredientWithAmount("Milk", 300.0, MeasurementUnit.MILLILITER, IngredientCategory.DAIRY),
                IngredientWithAmount("Egg", 2.0, MeasurementUnit.PIECE, IngredientCategory.DAIRY),
                IngredientWithAmount("Sugar", 50.0, MeasurementUnit.GRAM, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 2: Grilled Chicken Salad
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_grilled_chicken_salad),
            instructions = application.getString(R.string.demo_instructions_grilled_chicken_salad),
            servings = 2,
            ingredients = listOf(
                IngredientWithAmount("Chicken Breast", 400.0, MeasurementUnit.GRAM, IngredientCategory.MEAT),
                IngredientWithAmount("Lettuce", 200.0, MeasurementUnit.GRAM, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Tomato", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Olive Oil", 30.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 3: Pasta Bolognese
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_pasta_bolognese),
            instructions = application.getString(R.string.demo_instructions_pasta_bolognese),
            servings = 4,
            ingredients = listOf(
                IngredientWithAmount("Pasta", 500.0, MeasurementUnit.GRAM, IngredientCategory.PANTRY),
                IngredientWithAmount("Ground Beef", 500.0, MeasurementUnit.GRAM, IngredientCategory.MEAT),
                IngredientWithAmount("Tomato", 4.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Onion", 1.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Garlic", 3.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Olive Oil", 30.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 4: Grilled Salmon with Rice
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_grilled_salmon_rice),
            instructions = application.getString(R.string.demo_instructions_grilled_salmon_rice),
            servings = 2,
            ingredients = listOf(
                IngredientWithAmount("Salmon", 400.0, MeasurementUnit.GRAM, IngredientCategory.FISH),
                IngredientWithAmount("Rice", 200.0, MeasurementUnit.GRAM, IngredientCategory.PANTRY),
                IngredientWithAmount("Carrot", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Butter", 20.0, MeasurementUnit.GRAM, IngredientCategory.DAIRY)
            )
        ))
        
        // Recipe 5: Vegetable Stir Fry
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_vegetable_stir_fry),
            instructions = application.getString(R.string.demo_instructions_vegetable_stir_fry),
            servings = 3,
            ingredients = listOf(
                IngredientWithAmount("Broccoli", 300.0, MeasurementUnit.GRAM, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Bell Pepper", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Carrot", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Garlic", 3.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Soy Sauce", 50.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY),
                IngredientWithAmount("Olive Oil", 30.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 6: Beef Tacos
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_beef_tacos),
            instructions = application.getString(R.string.demo_instructions_beef_tacos),
            servings = 4,
            ingredients = listOf(
                IngredientWithAmount("Ground Beef", 600.0, MeasurementUnit.GRAM, IngredientCategory.MEAT),
                IngredientWithAmount("Tortillas", 8.0, MeasurementUnit.PIECE, IngredientCategory.PANTRY),
                IngredientWithAmount("Lettuce", 150.0, MeasurementUnit.GRAM, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Tomato", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Cheese", 150.0, MeasurementUnit.GRAM, IngredientCategory.DAIRY),
                IngredientWithAmount("Sour Cream", 100.0, MeasurementUnit.GRAM, IngredientCategory.DAIRY)
            )
        ))
        
        // Recipe 7: Mushroom Risotto
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_mushroom_risotto),
            instructions = application.getString(R.string.demo_instructions_mushroom_risotto),
            servings = 4,
            ingredients = listOf(
                IngredientWithAmount("Rice", 300.0, MeasurementUnit.GRAM, IngredientCategory.PANTRY),
                IngredientWithAmount("Mushrooms", 400.0, MeasurementUnit.GRAM, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Onion", 1.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Butter", 50.0, MeasurementUnit.GRAM, IngredientCategory.DAIRY),
                IngredientWithAmount("Cheese", 100.0, MeasurementUnit.GRAM, IngredientCategory.DAIRY)
            )
        ))
        
        // Recipe 8: Baked Cod with Vegetables
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_baked_cod),
            instructions = application.getString(R.string.demo_instructions_baked_cod),
            servings = 2,
            ingredients = listOf(
                IngredientWithAmount("Cod Fillet", 400.0, MeasurementUnit.GRAM, IngredientCategory.FISH),
                IngredientWithAmount("Zucchini", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Cherry Tomatoes", 200.0, MeasurementUnit.GRAM, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Olive Oil", 40.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 9: Chicken Curry
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_chicken_curry),
            instructions = application.getString(R.string.demo_instructions_chicken_curry),
            servings = 4,
            ingredients = listOf(
                IngredientWithAmount("Chicken Breast", 600.0, MeasurementUnit.GRAM, IngredientCategory.MEAT),
                IngredientWithAmount("Coconut Milk", 400.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY),
                IngredientWithAmount("Tomato", 3.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Onion", 1.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Garlic", 3.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Olive Oil", 30.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 10: Greek Salad with Feta
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_greek_salad),
            instructions = application.getString(R.string.demo_instructions_greek_salad),
            servings = 3,
            ingredients = listOf(
                IngredientWithAmount("Cucumber", 1.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Tomato", 3.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Bell Pepper", 1.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Red Onion", 1.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Feta Cheese", 200.0, MeasurementUnit.GRAM, IngredientCategory.DAIRY),
                IngredientWithAmount("Olive Oil", 50.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 11: Pork Chops with Mashed Potatoes
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_pork_chops),
            instructions = application.getString(R.string.demo_instructions_pork_chops),
            servings = 3,
            ingredients = listOf(
                IngredientWithAmount("Pork Chops", 600.0, MeasurementUnit.GRAM, IngredientCategory.MEAT),
                IngredientWithAmount("Potatoes", 800.0, MeasurementUnit.GRAM, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Green Beans", 300.0, MeasurementUnit.GRAM, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Butter", 40.0, MeasurementUnit.GRAM, IngredientCategory.DAIRY),
                IngredientWithAmount("Milk", 100.0, MeasurementUnit.MILLILITER, IngredientCategory.DAIRY)
            )
        ))
        
        // Recipe 12: Shrimp Pasta
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_shrimp_pasta),
            instructions = application.getString(R.string.demo_instructions_shrimp_pasta),
            servings = 3,
            ingredients = listOf(
                IngredientWithAmount("Pasta", 400.0, MeasurementUnit.GRAM, IngredientCategory.PANTRY),
                IngredientWithAmount("Shrimp", 500.0, MeasurementUnit.GRAM, IngredientCategory.FISH),
                IngredientWithAmount("Cherry Tomatoes", 250.0, MeasurementUnit.GRAM, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Garlic", 4.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Olive Oil", 40.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 13: Lentil Soup
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_lentil_soup),
            instructions = application.getString(R.string.demo_instructions_lentil_soup),
            servings = 6,
            ingredients = listOf(
                IngredientWithAmount("Lentils", 300.0, MeasurementUnit.GRAM, IngredientCategory.PANTRY),
                IngredientWithAmount("Carrot", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Celery", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Onion", 1.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Tomato", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Garlic", 3.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Olive Oil", 30.0, MeasurementUnit.MILLILITER, IngredientCategory.PANTRY)
            )
        ))
        
        // Recipe 14: Turkey Meatballs with Spaghetti
        recipeIds.add(repository.addRecipe(
            name = application.getString(R.string.demo_recipe_turkey_meatballs),
            instructions = application.getString(R.string.demo_instructions_turkey_meatballs),
            servings = 4,
            ingredients = listOf(
                IngredientWithAmount("Ground Turkey", 500.0, MeasurementUnit.GRAM, IngredientCategory.MEAT),
                IngredientWithAmount("Spaghetti", 400.0, MeasurementUnit.GRAM, IngredientCategory.PANTRY),
                IngredientWithAmount("Tomato", 5.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Onion", 1.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Garlic", 2.0, MeasurementUnit.PIECE, IngredientCategory.VEGETABLES),
                IngredientWithAmount("Egg", 1.0, MeasurementUnit.PIECE, IngredientCategory.DAIRY)
            )
        ))
        
        // Create a weekly plan
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startDate = calendar.timeInMillis
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
        val weeklyPlanId = mealPlanDao.insertWeeklyPlan(
            WeeklyPlan(
                name = application.getString(R.string.demo_weekly_plan_name),
                startDate = startDate,
                commensals = 2,
                userId = userId
            )
        )
        
        // Add meals to the weekly plan (balanced menu)
        // Monday
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[1], dayOfWeek = DayOfWeek.MONDAY, mealType = MealType.LUNCH, servings = 1, commensals = 2)) // Grilled Chicken Salad
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[2], dayOfWeek = DayOfWeek.MONDAY, mealType = MealType.DINNER, servings = 1, commensals = 2)) // Pasta Bolognese
        
        // Tuesday
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[4], dayOfWeek = DayOfWeek.TUESDAY, mealType = MealType.LUNCH, servings = 1, commensals = 2)) // Vegetable Stir Fry
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[3], dayOfWeek = DayOfWeek.TUESDAY, mealType = MealType.DINNER, servings = 1, commensals = 2)) // Grilled Salmon with Rice
        
        // Wednesday
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[9], dayOfWeek = DayOfWeek.WEDNESDAY, mealType = MealType.LUNCH, servings = 1, commensals = 2)) // Greek Salad with Feta
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[8], dayOfWeek = DayOfWeek.WEDNESDAY, mealType = MealType.DINNER, servings = 1, commensals = 2)) // Chicken Curry
        
        // Thursday
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[12], dayOfWeek = DayOfWeek.THURSDAY, mealType = MealType.LUNCH, servings = 1, commensals = 2)) // Lentil Soup
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[7], dayOfWeek = DayOfWeek.THURSDAY, mealType = MealType.DINNER, servings = 1, commensals = 2)) // Baked Cod with Vegetables
        
        // Friday
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[11], dayOfWeek = DayOfWeek.FRIDAY, mealType = MealType.LUNCH, servings = 1, commensals = 2)) // Shrimp Pasta
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[5], dayOfWeek = DayOfWeek.FRIDAY, mealType = MealType.DINNER, servings = 1, commensals = 2)) // Beef Tacos
        
        // Saturday
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[0], dayOfWeek = DayOfWeek.SATURDAY, mealType = MealType.LUNCH, servings = 1, commensals = 2)) // Pancakes
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[10], dayOfWeek = DayOfWeek.SATURDAY, mealType = MealType.DINNER, servings = 1, commensals = 2)) // Pork Chops with Mashed Potatoes
        
        // Sunday
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[6], dayOfWeek = DayOfWeek.SUNDAY, mealType = MealType.LUNCH, servings = 1, commensals = 2)) // Mushroom Risotto
        mealPlanDao.insertMealPlan(MealPlan(weeklyPlanId = weeklyPlanId, recipeId = recipeIds[13], dayOfWeek = DayOfWeek.SUNDAY, mealType = MealType.DINNER, servings = 1, commensals = 2)) // Turkey Meatballs with Spaghetti
    }
}
