package com.lokosoft.mealplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokosoft.mealplanner.R
import com.lokosoft.mealplanner.family.FamilyScreen
import com.lokosoft.mealplanner.ui.calendar.MealCalendarViewModel
import com.lokosoft.mealplanner.ui.ingredients.IngredientsScreen
import com.lokosoft.mealplanner.ui.recipe.RecipeScreen
import com.lokosoft.mealplanner.ui.recipe.RecipeViewModel

// 1. Add an icon property to the Screen class for better organization
sealed class Screen(val route: String, val nameRes: Int, val icon: ImageVector) {
    object Calendar : Screen("calendar", R.string.nav_calendar, Icons.Filled.DateRange)
    object Recipes : Screen("recipes", R.string.nav_recipes, Icons.AutoMirrored.Filled.List)
    object ShoppingList : Screen("shopping", R.string.nav_shopping, Icons.Filled.ShoppingCart)
    object Family : Screen("family", R.string.nav_family, Icons.Filled.Person)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings)
}

// A new top-level composable to hold the NavController and Scaffold structure
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerApp() {
    val navController = rememberNavController()
    val recipeViewModel: RecipeViewModel = viewModel()
    val calendarViewModel: MealCalendarViewModel = viewModel()
    
    Scaffold(
        bottomBar = {
            AppBottomNavigation(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Calendar.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Calendar.route) {
                MealCalendarScreen(
                    viewModel = calendarViewModel,
                    onNavigateToFamily = {
                        navController.navigate(Screen.Family.route)
                    },
                    onOpenDrawer = { }
                )
            }
            composable(Screen.Recipes.route) {
                RecipeScreen(
                    viewModel = recipeViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToIngredients = { navController.navigate("ingredients") }
                )
            }
            composable("ingredients") {
                IngredientsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ShoppingList.route) {
                ShoppingListScreen()
            }
            composable(Screen.Family.route) {
                FamilyScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    recipeViewModel = recipeViewModel,
                    calendarViewModel = calendarViewModel,
                    onNavigateToFamily = {
                        navController.navigate(Screen.Family.route)
                    }
                )
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    val items = listOf(
        Screen.Calendar,
        Screen.Recipes,
        Screen.ShoppingList,
        Screen.Settings
    )

    NavigationBar(
        modifier = modifier
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            val labelText = stringResource(screen.nameRes)
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = labelText) },
                label = { Text(labelText) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
