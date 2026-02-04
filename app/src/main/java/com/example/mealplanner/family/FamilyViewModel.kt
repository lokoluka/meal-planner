package com.example.mealplanner.family

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.AppDatabase
import com.example.mealplanner.data.Family
import com.example.mealplanner.data.FamilyMember
import com.example.mealplanner.data.FamilyWithMembers
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val familyDao = database.familyDao()
    private val mealPlanDao = database.mealPlanDao()
    private val auth = FirebaseAuth.getInstance()
    
    private val _families = MutableStateFlow<List<FamilyWithMembers>>(emptyList())
    val families: StateFlow<List<FamilyWithMembers>> = _families.asStateFlow()
    
    private val _selectedFamily = MutableStateFlow<FamilyWithMembers?>(null)
    val selectedFamily: StateFlow<FamilyWithMembers?> = _selectedFamily.asStateFlow()

    private val _familyPlans = MutableStateFlow<List<com.example.mealplanner.data.WeeklyPlan>>(emptyList())
    val familyPlans: StateFlow<List<com.example.mealplanner.data.WeeklyPlan>> = _familyPlans.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadFamilies()
    }
    
    private fun loadFamilies() {
        viewModelScope.launch {
            familyDao.getAllFamiliesWithMembers().collect { familiesList ->
                _families.value = familiesList
            }
        }
    }
    
    fun selectFamily(familyId: Long) {
        loadFamilyPlans(familyId)
        viewModelScope.launch {
            familyDao.getFamilyWithMembers(familyId).collect { family ->
                _selectedFamily.value = family
            }
        }
    }
    
    fun deselectFamily() {
        _selectedFamily.value = null
        _familyPlans.value = emptyList()
    }

    private fun loadFamilyPlans(familyId: Long) {
        viewModelScope.launch {
            _familyPlans.value = mealPlanDao.getWeeklyPlansForFamily(familyId)
        }
    }
    
    fun createFamily(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _errorMessage.value = "You must be logged in to create a family"
                    _isLoading.value = false
                    return@launch
                }
                
                val family = Family(
                    name = name,
                    ownerId = currentUser.uid
                )
                
                val familyId = familyDao.insertFamily(family)
                
                // Add the creator as the first member
                val member = FamilyMember(
                    familyId = familyId,
                    userId = currentUser.uid,
                    email = currentUser.email ?: "",
                    displayName = currentUser.displayName
                )
                familyDao.insertFamilyMember(member)
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    fun addMember(familyId: Long, email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // In a real app, you would search for the user by email in Firebase
                // and send them an invitation. For now, we'll create a placeholder
                val member = FamilyMember(
                    familyId = familyId,
                    userId = "pending_$email",
                    email = email,
                    displayName = null
                )
                familyDao.insertFamilyMember(member)
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    fun removeMember(member: FamilyMember) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                familyDao.deleteFamilyMember(member)
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    fun deleteFamily(family: Family) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                familyDao.deleteFamily(family)
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
