package com.lokosoft.mealplanner.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokosoft.mealplanner.data.AppDatabase
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
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

    init {
        // Observe Room changes and trigger sync when user is authenticated.
        val recipeChanges = database.recipeDao().getRecipesWithIngredientsFlow().map { "recipes" }
        val planChanges = database.mealPlanDao().getAllWeeklyPlansFlow().map { "plans" }

        val changes = merge(recipeChanges, planChanges)
            .debounce(2000)

        viewModelScope.launch {
            changes.collect { changeType ->
                val user = FirebaseAuth.getInstance().currentUser
                Log.d("SyncViewModel", "Detected DB change: $changeType, user=${user?.uid}")
                if (user == null || user.isAnonymous) {
                    Log.d("SyncViewModel", "User not authenticated or anonymous, skipping sync")
                    return@collect
                }

                try {
                    _isSyncing.value = true
                    _syncError.value = null

                    when (changeType) {
                        "recipes" -> {
                            Log.d("SyncViewModel", "Triggering syncRecipes for user=${user.uid}")
                            syncManager.syncRecipes()
                        }
                        "plans" -> {
                            Log.d("SyncViewModel", "Triggering syncWeeklyPlans for user=${user.uid}")
                            syncManager.syncWeeklyPlans()
                        }
                        else -> {
                            syncManager.syncAll()
                        }
                    }

                    _lastSyncTime.value = System.currentTimeMillis()
                } catch (e: Exception) {
                    _syncError.value = e.message ?: "Sync failed"
                    Log.e("SyncViewModel", "Sync failed", e)
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }
    
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
