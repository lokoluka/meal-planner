package com.lokosoft.mealplanner.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokosoft.mealplanner.data.AppDatabase
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(FlowPreview::class)
class SyncViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val syncManager = FirebaseSyncManager(
        recipeDao = database.recipeDao(),
        mealPlanDao = database.mealPlanDao()
    )
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var recipeListener: ListenerRegistration? = null
    private var weeklyPlansListener: ListenerRegistration? = null
    private val authStateListener = FirebaseAuth.AuthStateListener {
        // Rebind listeners on auth changes (sign-in/out).
        removeFirebaseListeners()
        observeFirebaseChanges()
    }
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime
    
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    private val syncMutex = Mutex()
    @Volatile
    private var suppressDbChanges = false
    private val dbSyncCooldownMs = 8000L
    @Volatile
    private var lastSyncCompletedAt = 0L

    init {
        val app = FirebaseApp.getInstance()
        Log.d(
            "SyncViewModel",
            "Firebase app=${app.name} projectId=${app.options.projectId} applicationId=${app.options.applicationId}"
        )
        Log.d("SyncViewModel", "SyncViewModel instance=${hashCode()}")
        auth.addAuthStateListener(authStateListener)
        // Observe Room changes and trigger sync when user is authenticated.
        val recipeChanges = database.recipeDao().getRecipesWithIngredientsFlow().map { "recipes" }
        val planChanges = database.mealPlanDao().getAllWeeklyPlansFlow().map { "plans" }

        val changes = merge(recipeChanges, planChanges)
            .debounce(5000)

        viewModelScope.launch {
            changes.collect { changeType ->
                val user = FirebaseAuth.getInstance().currentUser
                Log.d("SyncViewModel", "Detected DB change: $changeType, user=${user?.uid}")
                if (user == null || user.isAnonymous) {
                    Log.d("SyncViewModel", "User not authenticated or anonymous, skipping sync")
                    return@collect
                }

                val now = System.currentTimeMillis()
                if (now - lastSyncCompletedAt < dbSyncCooldownMs) {
                    Log.d("SyncViewModel", "Skipping DB-triggered sync during cooldown window")
                    return@collect
                }

                if (suppressDbChanges) {
                    Log.d("SyncViewModel", "Skipping DB-triggered sync while suppressDbChanges=true")
                    return@collect
                }

                if (!syncMutex.tryLock()) {
                    Log.d("SyncViewModel", "Sync already in progress, skipping DB-triggered sync")
                    return@collect
                }

                try {
                    _isSyncing.value = true
                    _syncError.value = null

                    when (changeType) {
                        "recipes" -> {
                            Log.d("SyncViewModel", "Triggering syncRecipes for user=${user.uid}")
                            suppressDbChanges = true
                            syncManager.syncRecipes()
                        }
                        "plans" -> {
                            Log.d("SyncViewModel", "Triggering syncWeeklyPlans for user=${user.uid}")
                            suppressDbChanges = true
                            syncManager.syncWeeklyPlans()
                        }
                        else -> {
                            suppressDbChanges = true
                            syncManager.syncAll()
                        }
                    }

                    _lastSyncTime.value = System.currentTimeMillis()
                } catch (e: Exception) {
                    _syncError.value = e.message ?: "Sync failed"
                    Log.e("SyncViewModel", "Sync failed", e)
                } finally {
                    suppressDbChanges = false
                    _isSyncing.value = false
                    syncMutex.unlock()
                    lastSyncCompletedAt = System.currentTimeMillis()
                }
            }
        }

        // Observe Firebase changes for real-time sync
        observeFirebaseChanges()
    }

    fun onAuthStateChanged() {
        removeFirebaseListeners()
        observeFirebaseChanges()
    }

    private fun removeFirebaseListeners() {
        recipeListener?.remove()
        recipeListener = null
        weeklyPlansListener?.remove()
        weeklyPlansListener = null
    }

    private fun observeFirebaseChanges() {
        val user = auth.currentUser
        if (user == null || user.isAnonymous) return

        Log.d("SyncViewModel", "Registering Firebase listeners for user=${user.uid}")

        // Listen for recipe changes in Firebase
        recipeListener = firestore.collection("users")
            .document(user.uid)
            .collection("recipes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SyncViewModel", "Error listening to recipe changes", error)
                    if (error is FirebaseFirestoreException &&
                        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.e("SyncViewModel", "Permission denied for recipe listener, removing listener")
                        removeFirebaseListeners()
                    }
                    return@addSnapshotListener
                }

                if (!syncMutex.tryLock()) {
                    Log.d("SyncViewModel", "Sync already in progress, skipping Firebase recipe sync")
                    return@addSnapshotListener
                }

                Log.d("SyncViewModel", "Detected Firebase recipe changes, triggering sync")
                viewModelScope.launch {
                    try {
                        _isSyncing.value = true
                        _syncError.value = null
                        suppressDbChanges = true
                        syncManager.syncRecipes()
                        _lastSyncTime.value = System.currentTimeMillis()
                        Log.d("SyncViewModel", "Sync completed successfully for recipes")
                    } catch (e: Exception) {
                        _syncError.value = e.message ?: "Sync failed"
                        Log.e("SyncViewModel", "Sync failed", e)
                    } finally {
                        suppressDbChanges = false
                        _isSyncing.value = false
                        syncMutex.unlock()
                        lastSyncCompletedAt = System.currentTimeMillis()
                        Log.d("SyncViewModel", "Released sync lock for recipe changes")
                    }
                }
            }

        // Listen for weekly plans changes in Firebase
        Log.d("SyncViewModel", "Listening weeklyPlans at users/${user.uid}/weeklyPlans")
        weeklyPlansListener = firestore.collection("users")
            .document(user.uid)
            .collection("weeklyPlans")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SyncViewModel", "Error listening to weekly plans changes", error)
                    if (error is FirebaseFirestoreException &&
                        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.e("SyncViewModel", "Permission denied for weekly plans listener, removing listener")
                        removeFirebaseListeners()
                    }
                    return@addSnapshotListener
                }

                if (!syncMutex.tryLock()) {
                    Log.d("SyncViewModel", "Sync already in progress, skipping Firebase weekly plans sync")
                    return@addSnapshotListener
                }

                Log.d("SyncViewModel", "Detected Firebase weekly plans changes, triggering sync")
                viewModelScope.launch {
                    try {
                        _isSyncing.value = true
                        _syncError.value = null
                        suppressDbChanges = true
                        syncManager.syncWeeklyPlans()
                        _lastSyncTime.value = System.currentTimeMillis()
                        Log.d("SyncViewModel", "Sync completed successfully for weekly plans")
                    } catch (e: Exception) {
                        _syncError.value = e.message ?: "Sync failed"
                        Log.e("SyncViewModel", "Sync failed", e)
                    } finally {
                        suppressDbChanges = false
                        _isSyncing.value = false
                        syncMutex.unlock()
                        lastSyncCompletedAt = System.currentTimeMillis()
                        Log.d("SyncViewModel", "Released sync lock for weekly plans changes")
                    }
                }
            }
    }
    
    fun syncNow() {
        viewModelScope.launch {
            if (!syncMutex.tryLock()) {
                Log.d("SyncViewModel", "Sync already in progress, skipping manual sync")
                return@launch
            }

            try {
                _isSyncing.value = true
                _syncError.value = null
                suppressDbChanges = true

                syncManager.syncAll()

                _lastSyncTime.value = System.currentTimeMillis()
                Log.d("SyncViewModel", "Sync completed successfully for all data")
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Sync failed"
                Log.e("SyncViewModel", "Manual sync failed", e)
            } finally {
                suppressDbChanges = false
                _isSyncing.value = false
                syncMutex.unlock()
                lastSyncCompletedAt = System.currentTimeMillis()
                Log.d("SyncViewModel", "Released sync lock for manual sync")
            }
        }
    }
    
    fun clearError() {
        _syncError.value = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        removeFirebaseListeners()
        super.onCleared()
    }
}
