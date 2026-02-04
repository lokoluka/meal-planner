package com.example.mealplanner.ui.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.AppDatabase
import com.example.mealplanner.data.IngredientCategory
import com.example.mealplanner.data.MeasurementUnit
import com.example.mealplanner.data.WeeklyPlan
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ShoppingListItem(
    val ingredientName: String,
    val totalAmount: Double,
    val unit: MeasurementUnit,
    val category: IngredientCategory,
    val isChecked: Boolean = false
)

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val mealPlanDao = AppDatabase.getDatabase(application).mealPlanDao()
    private val recipeDao = AppDatabase.getDatabase(application).recipeDao()

    private val _weeklyPlans = MutableStateFlow<List<WeeklyPlan>>(emptyList())
    val weeklyPlans: StateFlow<List<WeeklyPlan>> = _weeklyPlans

    private val _selectedWeeklyPlanId = MutableStateFlow<Long?>(null)
    val selectedWeeklyPlanId: StateFlow<Long?> = _selectedWeeklyPlanId

    private val _shoppingList = MutableStateFlow<List<ShoppingListItem>>(emptyList())
    val shoppingList: StateFlow<List<ShoppingListItem>> = _shoppingList
    
    private val _checkedItems = MutableStateFlow<Set<String>>(emptySet())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadWeeklyPlans()
    }

    private fun loadWeeklyPlans() {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            _weeklyPlans.value = if (userId != null) {
                mealPlanDao.getWeeklyPlansForUser(userId)
            } else {
                emptyList()
            }
            // Auto-select the first plan if available
            if (_weeklyPlans.value.isNotEmpty() && _selectedWeeklyPlanId.value == null) {
                selectWeeklyPlan(_weeklyPlans.value.first().weeklyPlanId)
            }
        }
    }

    fun refreshWeeklyPlans() {
        loadWeeklyPlans()
    }

    fun selectWeeklyPlan(weeklyPlanId: Long) {
        viewModelScope.launch {
            _selectedWeeklyPlanId.value = weeklyPlanId
            loadShoppingList(weeklyPlanId)
        }
    }

    fun loadShoppingListForPlan(weeklyPlanId: Long) {
        viewModelScope.launch {
            loadShoppingList(weeklyPlanId)
        }
    }

    private suspend fun loadShoppingList(weeklyPlanId: Long) {
        _isLoading.value = true
        
        // Get all meal plans for this week
        val mealPlans = mealPlanDao.getMealPlansByWeeklyPlan(weeklyPlanId)
        
        // Map to accumulate ingredients with their category
        val ingredientMap = mutableMapOf<Pair<String, MeasurementUnit>, Pair<Double, IngredientCategory>>()
        
        // For each meal plan, get the recipe ingredients
        mealPlans.forEach { mealPlanWithRecipe ->
            val recipeId = mealPlanWithRecipe.recipe.recipeId
            val recipeServings = mealPlanWithRecipe.recipe.servings
            val mealCommensals = mealPlanWithRecipe.mealPlan.commensals
            
            // Get ingredients for this recipe
            val ingredients = recipeDao.getIngredientsForRecipe(recipeId)
            
            // Calculate the multiplier based on meal's commensals
            // Prevent division by zero - if servings is 0, use 1:1 ratio
            val multiplier = if (recipeServings > 0) {
                mealCommensals.toDouble() / recipeServings.toDouble()
            } else {
                1.0
            }
            
            // Add to the map
            ingredients.forEach { ingredientAmount ->
                val ingredient = recipeDao.getIngredientById(ingredientAmount.ingredientId)
                ingredient?.let {
                    val key = Pair(ingredient.name, ingredientAmount.unit)
                    val adjustedAmount = ingredientAmount.amount * multiplier
                    val currentAmount = ingredientMap[key]?.first ?: 0.0
                    ingredientMap[key] = Pair(currentAmount + adjustedAmount, ingredient.category)
                }
            }
        }
        
        // Convert map to list and sort by category then name
        val checkedItems = _checkedItems.value
        _shoppingList.value = ingredientMap.map { (key, value) ->
            val itemKey = "${key.first}_${key.second}"
            ShoppingListItem(
                ingredientName = key.first,
                totalAmount = value.first,
                unit = key.second,
                category = value.second,
                isChecked = itemKey in checkedItems
            )
        }.sortedWith(compareBy<ShoppingListItem> { it.isChecked }.thenBy { it.category }.thenBy { it.ingredientName })
        
        _isLoading.value = false
    }
    
    fun toggleItemChecked(ingredientName: String, unit: MeasurementUnit) {
        val itemKey = "${ingredientName}_$unit"
        _checkedItems.value = if (itemKey in _checkedItems.value) {
            _checkedItems.value - itemKey
        } else {
            _checkedItems.value + itemKey
        }
        // Update the shopping list to reflect the change
        _shoppingList.value = _shoppingList.value.map { item ->
            if (item.ingredientName == ingredientName && item.unit == unit) {
                item.copy(isChecked = !item.isChecked)
            } else {
                item
            }
        }.sortedWith(compareBy<ShoppingListItem> { it.isChecked }.thenBy { it.category }.thenBy { it.ingredientName })
    }
    
    fun clearCheckedItems() {
        _checkedItems.value = emptySet()
        _shoppingList.value = _shoppingList.value.map { it.copy(isChecked = false) }
    }
}
