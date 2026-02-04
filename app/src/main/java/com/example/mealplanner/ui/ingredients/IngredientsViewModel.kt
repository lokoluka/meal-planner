package com.example.mealplanner.ui.ingredients

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.AppDatabase
import com.example.mealplanner.data.Ingredient
import com.example.mealplanner.data.IngredientCategory
import com.example.mealplanner.data.IngredientPackage
import com.example.mealplanner.data.IngredientWithPackages
import com.example.mealplanner.repository.IngredientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IngredientsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val recipeDao = AppDatabase.getDatabase(application).recipeDao()
    private val repository = IngredientRepository(recipeDao)
    
    private val _ingredientsWithPackages = MutableStateFlow<List<IngredientWithPackages>>(emptyList())
    val ingredientsWithPackages: StateFlow<List<IngredientWithPackages>> = _ingredientsWithPackages
    
    private val _filteredIngredients = MutableStateFlow<List<IngredientWithPackages>>(emptyList())
    val filteredIngredients: StateFlow<List<IngredientWithPackages>> = _filteredIngredients
    
    private val _potentialDuplicates = MutableStateFlow<List<Pair<Ingredient, Ingredient>>>(emptyList())
    val potentialDuplicates: StateFlow<List<Pair<Ingredient, Ingredient>>> = _potentialDuplicates
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _selectedCategory = MutableStateFlow<IngredientCategory?>(null)
    val selectedCategory: StateFlow<IngredientCategory?> = _selectedCategory
    
    init {
        loadIngredients()
    }
    
    fun loadIngredients() {
        viewModelScope.launch {
            _isLoading.value = true
            _ingredientsWithPackages.value = repository.getAllIngredientsWithPackages()
            applyFilters()
            findDuplicates()
            _isLoading.value = false
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }
    
    fun setSelectedCategory(category: IngredientCategory?) {
        _selectedCategory.value = category
        applyFilters()
    }
    
    private fun applyFilters() {
        var filtered = _ingredientsWithPackages.value
        
        // Apply search filter
        val query = _searchQuery.value
        if (query.isNotBlank()) {
            filtered = filtered.filter { 
                it.ingredient.name.contains(query, ignoreCase = true) 
            }
        }
        
        // Apply category filter
        val category = _selectedCategory.value
        if (category != null) {
            filtered = filtered.filter { it.ingredient.category == category }
        }
        
        _filteredIngredients.value = filtered.sortedBy { it.ingredient.name }
    }
    
    fun addIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.addIngredient(ingredient)
            loadIngredients()
        }
    }
    
    fun updateIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.updateIngredient(ingredient)
            loadIngredients()
        }
    }
    
    fun deleteIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.deleteIngredient(ingredient)
            loadIngredients()
        }
    }
    
    fun mergeIngredients(keepIngredient: Ingredient, removeIngredient: Ingredient) {
        viewModelScope.launch {
            // In a real implementation, you'd also need to update all recipe references
            // For now, just remove the duplicate
            repository.deleteIngredient(removeIngredient)
            loadIngredients()
        }
    }
    
    private fun findDuplicates() {
        viewModelScope.launch {
            val ingredients = _ingredientsWithPackages.value.map { it.ingredient }
            _potentialDuplicates.value = repository.findPotentialDuplicates(ingredients)
        }
    }
    
    // Package management
    fun addPackage(ingredientPackage: IngredientPackage) {
        viewModelScope.launch {
            repository.addPackage(ingredientPackage)
            loadIngredients()
        }
    }
    
    fun updatePackage(ingredientPackage: IngredientPackage) {
        viewModelScope.launch {
            repository.updatePackage(ingredientPackage)
            loadIngredients()
        }
    }
    
    fun deletePackage(ingredientPackage: IngredientPackage) {
        viewModelScope.launch {
            repository.deletePackage(ingredientPackage)
            loadIngredients()
        }
    }
    
    /**
     * Auto-suggest a category based on ingredient name
     */
    fun suggestCategory(ingredientName: String): IngredientCategory {
        val name = ingredientName.lowercase().trim()
        
        return when {
            // Meat
            name.contains("chicken") || name.contains("beef") || name.contains("pork") ||
            name.contains("turkey") || name.contains("meat") || name.contains("sausage") ||
            name.contains("bacon") || name.contains("ham") -> IngredientCategory.MEAT
            
            // Fish
            name.contains("fish") || name.contains("salmon") || name.contains("tuna") ||
            name.contains("cod") || name.contains("shrimp") || name.contains("seafood") ||
            name.contains("prawn") -> IngredientCategory.FISH
            
            // Dairy
            name.contains("milk") || name.contains("cheese") || name.contains("butter") ||
            name.contains("cream") || name.contains("yogurt") || name.contains("feta") ||
            name.contains("mozzarella") || name.contains("cheddar") -> IngredientCategory.DAIRY
            
            // Vegetables
            name.contains("tomato") || name.contains("onion") || name.contains("pepper") ||
            name.contains("lettuce") || name.contains("carrot") || name.contains("potato") ||
            name.contains("cucumber") || name.contains("broccoli") || name.contains("garlic") ||
            name.contains("mushroom") || name.contains("celery") || name.contains("zucchini") ||
            name.contains("beans") || name.contains("peas") || name.contains("spinach") -> IngredientCategory.VEGETABLES
            
            // Fruits
            name.contains("apple") || name.contains("banana") || name.contains("orange") ||
            name.contains("lemon") || name.contains("lime") || name.contains("berry") ||
            name.contains("grape") || name.contains("mango") -> IngredientCategory.FRUITS
            
            // Pantry
            name.contains("rice") || name.contains("pasta") || name.contains("flour") ||
            name.contains("sugar") || name.contains("oil") || name.contains("sauce") ||
            name.contains("tortilla") || name.contains("bread") || name.contains("lentil") ||
            name.contains("broth") || name.contains("soy") || name.contains("vinegar") -> IngredientCategory.PANTRY
            
            // Spices
            name.contains("salt") || name.contains("pepper") || name.contains("spice") ||
            name.contains("oregano") || name.contains("basil") || name.contains("cumin") ||
            name.contains("paprika") || name.contains("curry") || name.contains("cinnamon") -> IngredientCategory.SPICES
            
            // Beverages
            name.contains("juice") || name.contains("coffee") || name.contains("tea") ||
            name.contains("soda") || name.contains("water") -> IngredientCategory.BEVERAGES
            
            else -> IngredientCategory.OTHER
        }
    }
}
