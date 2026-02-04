package com.example.mealplanner.ui.recipe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.AppDatabase
import com.example.mealplanner.data.IngredientAmount
import com.example.mealplanner.data.MeasurementUnit
import com.example.mealplanner.data.Recipe
import com.example.mealplanner.data.RecipeIngredientCrossRef
import com.example.mealplanner.data.RecipeWithIngredients
import com.example.mealplanner.repository.FirebaseRecipeRepository
import com.example.mealplanner.repository.LocalRecipeRepository
import com.example.mealplanner.repository.RecipeRepository
import com.example.mealplanner.utils.DemoDataManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeDao = AppDatabase.getDatabase(application).recipeDao()
    private val auth = FirebaseAuth.getInstance()
    
    // Repository that switches based on auth state
    private var repository: RecipeRepository = getRepository()
    
    private val _hasDemoData = MutableStateFlow(false)
    val hasDemoData: StateFlow<Boolean> = _hasDemoData
    
    /**
     * Get the appropriate repository based on authentication state.
     * - Anonymous/Guest users: LocalRecipeRepository (Room only)
     * - Authenticated users: FirebaseRecipeRepository (Firestore + Room cache)
     */
    private fun getRepository(): RecipeRepository {
        val user = auth.currentUser
        return if (user != null && !user.isAnonymous) {
            FirebaseRecipeRepository(recipeDao)
        } else {
            LocalRecipeRepository(recipeDao)
        }
    }
    
    /**
     * Call this when auth state changes to switch repositories
     */
    fun onAuthStateChanged() {
        repository = getRepository()
        loadRecipes()
        checkDemoData()
    }
    
    /**
     * Create demo/sample data for testing
     */
    fun createDemoData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            DemoDataManager.createDemoData(getApplication(), repository)
            loadRecipes()
            loadAllIngredients()
            checkDemoData()
            _isLoading.value = false
            onComplete()
        }
    }
    
    /**
     * Delete all demo/sample data
     */
    fun deleteDemoData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            DemoDataManager.deleteDemoData(getApplication(), repository)
            loadRecipes()
            loadAllIngredients()
            checkDemoData()
            _isLoading.value = false
            onComplete()
        }
    }

    private val _recipes = MutableStateFlow<List<RecipeWithIngredients>>(emptyList())
    val recipes: StateFlow<List<RecipeWithIngredients>> = _recipes

    private val _allIngredients = MutableStateFlow<List<com.example.mealplanner.data.Ingredient>>(emptyList())
    val allIngredients: StateFlow<List<com.example.mealplanner.data.Ingredient>> = _allIngredients

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadRecipes()
        loadAllIngredients()
        checkDemoData()
    }
    
    private fun checkDemoData() {
        viewModelScope.launch {
            _hasDemoData.value = DemoDataManager.hasDemoData(getApplication(), repository)
        }
    }
    
    private fun loadAllIngredients() {
        viewModelScope.launch {
            _allIngredients.value = repository.getAllIngredients()
        }
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            _recipes.value = repository.getAllRecipes()
            _isLoading.value = false
        }
    }

    fun addRecipe(
        name: String,
        instructions: String,
        servings: Int,
        ingredients: List<IngredientWithAmount>
    ) {
        viewModelScope.launch {
            repository.addRecipe(name, instructions, servings, ingredients)
            loadRecipes()
            loadAllIngredients()
        }
    }

    fun updateRecipe(
        recipeId: Long,
        name: String,
        instructions: String,
        servings: Int,
        ingredients: List<IngredientWithAmount>
    ) {
        viewModelScope.launch {
            repository.updateRecipe(recipeId, name, instructions, servings, ingredients)
            loadRecipes()
            loadAllIngredients()
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
            loadRecipes()
        }
    }

    suspend fun getRecipeWithIngredients(recipeId: Long): RecipeWithIngredients? {
        return repository.getRecipeById(recipeId)
    }

    suspend fun getIngredientsForRecipe(recipeId: Long): List<IngredientAmount> {
        return repository.getIngredientsForRecipe(recipeId)
    }
}

data class IngredientWithAmount(
    val name: String,
    val amount: Double,
    val unit: MeasurementUnit,
    val category: com.example.mealplanner.data.IngredientCategory = com.example.mealplanner.data.IngredientCategory.OTHER
)
