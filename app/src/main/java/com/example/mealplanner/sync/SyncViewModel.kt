package com.example.mealplanner.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val syncManager = FirebaseSyncManager(
        recipeDao = database.recipeDao(),
        mealPlanDao = database.mealPlanDao()
    )
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime
    
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError
    
    fun syncNow() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncError.value = null
                
                syncManager.syncAll()
                
                _lastSyncTime.value = System.currentTimeMillis()
                _isSyncing.value = false
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Sync failed"
                _isSyncing.value = false
            }
        }
    }
    
    fun clearError() {
        _syncError.value = null
    }
}
