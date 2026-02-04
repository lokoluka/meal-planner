package com.example.mealplanner

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mealplanner.auth.AuthScreen
import com.example.mealplanner.auth.AuthViewModel
import com.example.mealplanner.ui.MealPlannerApp
import com.example.mealplanner.ui.theme.MealPlannerTheme
import com.example.mealplanner.utils.LocaleHelper
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        enableEdgeToEdge()
        setContent {
            MealPlannerTheme {
                AppRoot()
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        val language = LocaleHelper.getLanguage(newBase)
        val context = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(context)
    }
}

@Composable
fun AppRoot() {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    if (currentUser == null) {
        AuthScreen(
            viewModel = authViewModel,
            onSignedIn = { /* User signed in */ }
        )
    } else {
        MealPlannerApp()
    }
}
