package com.example.mealplanner.auth

import android.app.Activity
import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.AppDatabase
import com.example.mealplanner.repository.DataMigrationManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val oneTapClient: SignInClient = Identity.getSignInClient(application)
    
    // Data migration manager
    private val migrationManager = DataMigrationManager(
        recipeDao = AppDatabase.getDatabase(application).recipeDao()
    )
    
    // You need to get this from Firebase Console -> Authentication -> Sign-in method -> Google -> Web SDK configuration
    private val webClientId = "620624588681-frmrvdsiris9q75g7fl3qji8ao1s1ck2.apps.googleusercontent.com"
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    // Migration state
    val migrationState = migrationManager.migrationState
    val migrationProgress = migrationManager.migrationProgress
    
    private val _showMigrationDialog = MutableStateFlow(false)
    val showMigrationDialog: StateFlow<Boolean> = _showMigrationDialog
    
    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }
    
    fun signInAnonymously() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    _currentUser.value = result.user
                    _isLoading.value = false
                }
                .addOnFailureListener { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
        }
    }
    
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }
    
    fun signInWithGoogle(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .build()
            
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        launcher.launch(intentSenderRequest)
                    } catch (e: Exception) {
                        _errorMessage.value = e.message
                        _isLoading.value = false
                    }
                }
                .addOnFailureListener { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
        }
    }
    
    fun handleGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            val wasAnonymous = auth.currentUser?.isAnonymous == true
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    _currentUser.value = result.user
                    _isLoading.value = false
                    
                    // Check if we need to migrate local data
                    if (wasAnonymous) {
                        checkAndPromptForMigration()
                    }
                }
                .addOnFailureListener { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Check if there's local data and prompt user for migration
     */
    private fun checkAndPromptForMigration() {
        viewModelScope.launch {
            val hasData = migrationManager.hasLocalDataToMigrate()
            if (hasData) {
                _showMigrationDialog.value = true
            }
        }
    }
    
    /**
     * Migrate local data to Firebase
     * @param clearLocal If true, clears local data after successful migration
     */
    fun migrateData(clearLocal: Boolean = false) {
        viewModelScope.launch {
            _showMigrationDialog.value = false
            migrationManager.migrateLocalDataToFirebase(clearLocal)
        }
    }
    
    /**
     * Dismiss migration dialog without migrating
     */
    fun dismissMigrationDialog() {
        _showMigrationDialog.value = false
        migrationManager.resetMigrationState()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
