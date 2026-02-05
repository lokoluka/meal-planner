package com.lokosoft.mealplanner.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokosoft.mealplanner.data.AppDatabase
import com.lokosoft.mealplanner.data.DayOfWeek
import com.lokosoft.mealplanner.data.MealPlan
import com.lokosoft.mealplanner.data.MealPlanWithRecipe
import com.lokosoft.mealplanner.data.MealType
import com.lokosoft.mealplanner.data.RecipeWithIngredients
import com.lokosoft.mealplanner.data.WeeklyPlan
import com.lokosoft.mealplanner.sync.FirebaseSyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MealCalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val mealPlanDao = AppDatabase.getDatabase(application).mealPlanDao()
    private val recipeDao = AppDatabase.getDatabase(application).recipeDao()
    private val syncManager = FirebaseSyncManager(
        recipeDao = recipeDao,
        mealPlanDao = mealPlanDao
    )

    private val _weeklyPlans = MutableStateFlow<List<WeeklyPlan>>(emptyList())
    val weeklyPlans: StateFlow<List<WeeklyPlan>> = _weeklyPlans

    private val _currentWeeklyPlanId = MutableStateFlow<Long?>(null)
    val currentWeeklyPlanId: StateFlow<Long?> = _currentWeeklyPlanId
    
    private val _selectedWeeklyPlan = MutableStateFlow<WeeklyPlan?>(null)
    val selectedWeeklyPlan: StateFlow<WeeklyPlan?> = _selectedWeeklyPlan

    private val _mealPlans = MutableStateFlow<Map<Pair<DayOfWeek, MealType>, List<MealPlanWithRecipe>>>(emptyMap())
    val mealPlans: StateFlow<Map<Pair<DayOfWeek, MealType>, List<MealPlanWithRecipe>>> = _mealPlans

    private val _recipes = MutableStateFlow<List<RecipeWithIngredients>>(emptyList())
    val recipes: StateFlow<List<RecipeWithIngredients>> = _recipes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadWeeklyPlans()
        viewModelScope.launch {
            loadRecipes()
        }
    }

    fun loadWeeklyPlans() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            _weeklyPlans.value = if (userId != null) {
                mealPlanDao.getWeeklyPlansForUser(userId)
            } else {
                emptyList()
            }
            _isLoading.value = false
        }
    }

    fun createWeeklyPlan(name: String, startDate: Long, commensals: Int) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val weeklyPlanId = mealPlanDao.insertWeeklyPlan(
                WeeklyPlan(
                    name = name,
                    startDate = startDate,
                    commensals = commensals,
                    userId = userId
                )
            )
            loadWeeklyPlans()
            selectWeeklyPlan(weeklyPlanId)
            // Auto-sync after creating plan
            syncManager.syncWeeklyPlans()
        }
    }

    fun addWeeklyPlanToFamilies(weeklyPlanId: Long, familyIds: List<Long>) {
        viewModelScope.launch {
            familyIds.forEach { familyId ->
                mealPlanDao.insertWeeklyPlanFamilyCrossRef(
                    com.lokosoft.mealplanner.data.WeeklyPlanFamilyCrossRef(weeklyPlanId, familyId)
                )
            }
            loadWeeklyPlans()
            // Auto-sync after adding to families
            syncManager.syncWeeklyPlans()
        }
    }

    fun removeWeeklyPlanFromFamily(weeklyPlanId: Long, familyId: Long) {
        viewModelScope.launch {
            mealPlanDao.deleteWeeklyPlanFamilyCrossRef(
                com.lokosoft.mealplanner.data.WeeklyPlanFamilyCrossRef(weeklyPlanId, familyId)
            )
            loadWeeklyPlans()
            // Auto-sync after removing from family
            syncManager.syncWeeklyPlans()
        }
    }

    suspend fun getFamiliesForWeeklyPlan(weeklyPlanId: Long): List<Long> {
        return mealPlanDao.getFamilyIdsForWeeklyPlan(weeklyPlanId)
    }

    fun updateWeeklyPlanCommensals(weeklyPlanId: Long, commensals: Int) {
        viewModelScope.launch {
            val plan = _weeklyPlans.value.find { it.weeklyPlanId == weeklyPlanId }
            plan?.let {
                mealPlanDao.updateWeeklyPlan(it.copy(commensals = commensals))
                loadWeeklyPlans()
            }
        }
    }

    fun deleteWeeklyPlan(weeklyPlan: WeeklyPlan) {
        viewModelScope.launch {
            mealPlanDao.deleteWeeklyPlan(weeklyPlan)
            if (_currentWeeklyPlanId.value == weeklyPlan.weeklyPlanId) {
                _currentWeeklyPlanId.value = null
                _mealPlans.value = emptyMap()
            }
            loadWeeklyPlans()
        }
    }

    fun selectWeeklyPlan(weeklyPlanId: Long) {
        viewModelScope.launch {
            _currentWeeklyPlanId.value = weeklyPlanId
            _selectedWeeklyPlan.value = mealPlanDao.getWeeklyPlanById(weeklyPlanId)
            loadRecipes()  // Reload recipes when opening a plan
            loadMealPlansForWeek(weeklyPlanId)
        }
    }

    fun deselectWeeklyPlan() {
        _currentWeeklyPlanId.value = null
        _selectedWeeklyPlan.value = null
        _mealPlans.value = emptyMap()
    }

    private suspend fun loadMealPlansForWeek(weeklyPlanId: Long) {
        _isLoading.value = true
        val plans = mealPlanDao.getMealPlansByWeeklyPlan(weeklyPlanId)
        _mealPlans.value = plans.groupBy { 
            Pair(it.mealPlan.dayOfWeek, it.mealPlan.mealType) 
        }
        _isLoading.value = false
    }

    private suspend fun loadRecipes() {
        _recipes.value = recipeDao.getRecipesWithIngredients()
    }

    fun assignRecipeToMeal(
        dayOfWeek: DayOfWeek,
        mealType: MealType,
        recipeId: Long,
        servings: Int = 1,
        commensals: Int = 2
    ) {
        viewModelScope.launch {
            val weeklyPlanId = _currentWeeklyPlanId.value ?: return@launch
            
            // Don't delete existing meals - allow multiple recipes per slot
            // Just insert new meal plan
            mealPlanDao.insertMealPlan(
                MealPlan(
                    weeklyPlanId = weeklyPlanId,
                    recipeId = recipeId,
                    dayOfWeek = dayOfWeek,
                    mealType = mealType,
                    servings = servings,
                    commensals = commensals
                )
            )
            
            loadMealPlansForWeek(weeklyPlanId)
        }
    }

    fun removeMealPlan(mealPlanId: Long) {
        viewModelScope.launch {
            val weeklyPlanId = _currentWeeklyPlanId.value ?: return@launch
            val mealPlan = _mealPlans.value.values.flatten().find { it.mealPlan.mealPlanId == mealPlanId }
            mealPlan?.let {
                mealPlanDao.deleteMealPlan(it.mealPlan)
                loadMealPlansForWeek(weeklyPlanId)
                // Auto-sync after removing meal
                syncManager.syncWeeklyPlans()
            }
        }
    }

    fun clearAllMealsInWeek() {
        viewModelScope.launch {
            val weeklyPlanId = _currentWeeklyPlanId.value ?: return@launch
            mealPlanDao.clearWeeklyPlanMeals(weeklyPlanId)
            loadMealPlansForWeek(weeklyPlanId)
        }
    }
    
    fun restoreMealPlans(snapshot: Map<Pair<DayOfWeek, MealType>, List<MealPlanWithRecipe>>) {
        viewModelScope.launch {
            val weeklyPlanId = _currentWeeklyPlanId.value ?: return@launch
            
            // Clear all current meal plans
            mealPlanDao.clearWeeklyPlanMeals(weeklyPlanId)
            
            // Restore from snapshot
            snapshot.values.flatten().forEach { mealPlanWithRecipe ->
                mealPlanDao.insertMealPlan(mealPlanWithRecipe.mealPlan)
            }
            
            // Reload to update UI
            loadMealPlansForWeek(weeklyPlanId)
        }
    }
    
    fun updateWeeklyPlanCommensals(commensals: Int) {
        viewModelScope.launch {
            val weeklyPlanId = _currentWeeklyPlanId.value ?: return@launch
            val currentPlan = mealPlanDao.getWeeklyPlanById(weeklyPlanId)
            currentPlan?.let { plan ->
                val updatedPlan = plan.copy(commensals = commensals)
                mealPlanDao.updateWeeklyPlan(updatedPlan)
                _selectedWeeklyPlan.value = updatedPlan
                loadWeeklyPlans()
            }
        }
    }
}
