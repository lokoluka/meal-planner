package com.lokosoft.mealplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokosoft.mealplanner.R
import com.lokosoft.mealplanner.auth.AuthViewModel
import com.lokosoft.mealplanner.ui.calendar.MealCalendarViewModel
import com.lokosoft.mealplanner.ui.recipe.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    recipeViewModel: RecipeViewModel,
    calendarViewModel: MealCalendarViewModel,
    onNavigateToFamily: () -> Unit = {}
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDemoDataDialog by remember { mutableStateOf(false) }
    
    val hasDemoData by recipeViewModel.hasDemoData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Family Management
            ListItem(
                headlineContent = { Text(stringResource(R.string.nav_family)) },
                supportingContent = { Text(stringResource(R.string.family_management_description)) },
                leadingContent = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { onNavigateToFamily() }
            )
            
            HorizontalDivider()
            
            // Create/Delete Demo Data
            ListItem(
                headlineContent = { 
                    Text(stringResource(
                        if (hasDemoData) R.string.delete_demo_data 
                        else R.string.create_demo_data
                    )) 
                },
                supportingContent = { 
                    Text(stringResource(
                        if (hasDemoData) R.string.delete_demo_data_description 
                        else R.string.create_demo_data_description
                    )) 
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showDemoDataDialog = true }
            )
            
            HorizontalDivider()
            
            // Language Setting
            ListItem(
                headlineContent = { Text(stringResource(R.string.language_title)) },
                supportingContent = { Text(stringResource(R.string.language_note)) },
                leadingContent = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            
            HorizontalDivider()
            
            // Sign Out
            ListItem(
                headlineContent = { Text(stringResource(R.string.sign_out)) },
                supportingContent = { Text(stringResource(R.string.sign_out_message)) },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { showSignOutDialog = true }
            )
        }
        
        // Demo Data Dialog
        if (showDemoDataDialog) {
            AlertDialog(
                onDismissRequest = { showDemoDataDialog = false },
                title = { 
                    Text(stringResource(
                        if (hasDemoData) R.string.delete_demo_data_title 
                        else R.string.create_demo_data_title
                    )) 
                },
                text = { 
                    Text(stringResource(
                        if (hasDemoData) R.string.delete_demo_data_message 
                        else R.string.create_demo_data_message
                    )) 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (hasDemoData) {
                                recipeViewModel.deleteDemoData {
                                    calendarViewModel.loadWeeklyPlans()
                                }
                            } else {
                                recipeViewModel.createDemoData {
                                    calendarViewModel.loadWeeklyPlans()
                                }
                            }
                            showDemoDataDialog = false
                        }
                    ) {
                        Text(stringResource(
                            if (hasDemoData) R.string.delete 
                            else R.string.create
                        ))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDemoDataDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        
        // Language Dialog
        if (showLanguageDialog) {
            LanguagePickerDialog(
                onDismiss = { showLanguageDialog = false }
            )
        }
        
        // Sign Out Dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text(stringResource(R.string.sign_out_title)) },
                text = { Text(stringResource(R.string.sign_out_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            authViewModel.signOut()
                            showSignOutDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.sign_out))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
