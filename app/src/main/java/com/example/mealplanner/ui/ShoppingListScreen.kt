package com.example.mealplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mealplanner.R
import com.example.mealplanner.ui.LanguagePickerDialog
import com.example.mealplanner.ui.shopping.ShoppingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    modifier: Modifier = Modifier,
    viewModel: ShoppingListViewModel = viewModel()
) {
    val shoppingList by viewModel.shoppingList.collectAsState()
    val weeklyPlans by viewModel.weeklyPlans.collectAsState()
    val selectedPlanId by viewModel.selectedWeeklyPlanId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showPlanSelector by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshWeeklyPlans()
    }

    LaunchedEffect(showPlanSelector) {
        if (showPlanSelector) {
            viewModel.refreshWeeklyPlans()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shopping_list_title)) },
                actions = {
                    if (weeklyPlans.isNotEmpty()) {
                        TextButton(onClick = { showPlanSelector = true }) {
                            Text(stringResource(R.string.change_plan))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                selectedPlanId == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_plan_selected),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showPlanSelector = true }) {
                            Text(stringResource(R.string.select_plan))
                        }
                    }
                }
                shoppingList.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.list_empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.list_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Total item count at the top
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.items_total, shoppingList.size),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Group items by category
                        val groupedItems = shoppingList.groupBy { it.category }
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            groupedItems.keys.sortedBy { it.ordinal }.forEach { category ->
                                val items = groupedItems[category] ?: emptyList()
                                
                                // Section header with count
                                item(key = "header_$category") {
                                    Text(
                                        text = stringResource(
                                            R.string.category_items_count,
                                            category.name.lowercase().replaceFirstChar { it.uppercase() },
                                            items.size
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                
                                // Items in this section
                                items(
                                    items = items,
                                    key = { item -> "${item.ingredientName}_${item.unit}" }
                                ) { item ->
                                    ShoppingListItem(
                                        item = item,
                                        onToggle = { viewModel.toggleItemChecked(item.ingredientName, item.unit) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        val checkedCount = shoppingList.count { it.isChecked }
                        val totalCount = shoppingList.size
                        Text(
                            text = stringResource(R.string.checked_count, checkedCount, totalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showPlanSelector) {
            AlertDialog(
                onDismissRequest = { showPlanSelector = false },
                title = { Text(stringResource(R.string.select_weekly_plan_title)) },
                text = {
                    if (weeklyPlans.isEmpty()) {
                        Text(stringResource(R.string.no_plans_available))
                    } else {
                        LazyColumn {
                            items(weeklyPlans) { plan ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = {
                                        viewModel.selectWeeklyPlan(plan.weeklyPlanId)
                                        showPlanSelector = false
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (plan.weeklyPlanId == selectedPlanId) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Text(
                                        text = plan.name,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlanSelector = false }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }

        if (showLanguageDialog) {
            LanguagePickerDialog(onDismiss = { showLanguageDialog = false })
        }
    }
}

@Composable
fun ShoppingListItem(
    item: com.example.mealplanner.ui.shopping.ShoppingListItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.ingredientName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                    )
                )
            }
            Text(
                text = "${item.totalAmount.convertAndFormat(item.unit)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun Double.convertAndFormat(unit: com.example.mealplanner.data.MeasurementUnit): String {
    // Convert volume measurements to ml for shopping
    return when (unit) {
        com.example.mealplanner.data.MeasurementUnit.TEASPOON -> {
            val ml = this * 5.0
            "${ml.toFractionString()} ml"
        }
        com.example.mealplanner.data.MeasurementUnit.TABLESPOON -> {
            val ml = this * 15.0
            "${ml.toFractionString()} ml"
        }
        com.example.mealplanner.data.MeasurementUnit.CUP -> {
            val ml = this * 240.0
            "${ml.toFractionString()} ml"
        }
        else -> "${this.toFractionString()} ${unit.abbreviation()}"
    }
}

private fun Double.toFractionString(): String {
    val whole = this.toInt()
    val fraction = this - whole
    
    val fractionString = when {
        fraction >= 0.875 -> "" // Round up to next whole
        fraction >= 0.625 -> "¾"
        fraction >= 0.375 -> "½"
        fraction >= 0.125 -> "¼"
        else -> ""
    }
    
    val adjustedWhole = if (fraction >= 0.875) whole + 1 else whole
    
    return when {
        adjustedWhole == 0 && fractionString.isNotEmpty() -> fractionString
        adjustedWhole > 0 && fractionString.isNotEmpty() -> "$adjustedWhole $fractionString"
        adjustedWhole > 0 -> adjustedWhole.toString()
        else -> String.format("%.1f", this)
    }
}

private fun com.example.mealplanner.data.MeasurementUnit.abbreviation(): String {
    return when (this) {
        com.example.mealplanner.data.MeasurementUnit.GRAM -> "g"
        com.example.mealplanner.data.MeasurementUnit.KILOGRAM -> "kg"
        com.example.mealplanner.data.MeasurementUnit.MILLILITER -> "ml"
        com.example.mealplanner.data.MeasurementUnit.LITER -> "L"
        com.example.mealplanner.data.MeasurementUnit.TEASPOON -> "tsp"
        com.example.mealplanner.data.MeasurementUnit.TABLESPOON -> "tbsp"
        com.example.mealplanner.data.MeasurementUnit.CUP -> "cup"
        com.example.mealplanner.data.MeasurementUnit.PIECE -> "pc"
    }
}