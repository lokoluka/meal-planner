package com.lokosoft.mealplanner.ui.ingredients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokosoft.mealplanner.R
import com.lokosoft.mealplanner.data.Ingredient
import com.lokosoft.mealplanner.data.IngredientCategory
import com.lokosoft.mealplanner.data.IngredientPackage
import com.lokosoft.mealplanner.data.IngredientWithPackages
import com.lokosoft.mealplanner.data.MeasurementUnit
import com.lokosoft.mealplanner.data.PackageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientsScreen(
    onNavigateBack: () -> Unit,
    viewModel: IngredientsViewModel = viewModel()
) {
    val ingredients by viewModel.filteredIngredients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val potentialDuplicates by viewModel.potentialDuplicates.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDuplicatesDialog by remember { mutableStateOf(false) }
    var selectedIngredient by remember { mutableStateOf<IngredientWithPackages?>(null) }
    var showCategoryFilter by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ingredients_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    // Category filter button
                    IconButton(onClick = { showCategoryFilter = true }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Filter by category"
                        )
                    }
                    
                    // Show duplicates
                    if (potentialDuplicates.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge { Text(potentialDuplicates.size.toString()) }
                            }
                        ) {
                            IconButton(onClick = { showDuplicatesDialog = true }) {
                                Icon(Icons.Default.Info, contentDescription = "View duplicates")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_ingredients)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Category chip
            if (selectedCategory != null) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.setSelectedCategory(null) },
                        label = { Text(selectedCategory!!.name) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (ingredients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedCategory != null) {
                                stringResource(R.string.no_ingredients_found)
                            } else {
                                stringResource(R.string.no_ingredients_database)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.add_ingredient_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ingredients, key = { it.ingredient.ingredientId }) { ingredientWithPackages ->
                        IngredientCard(
                            ingredientWithPackages = ingredientWithPackages,
                            onClick = {
                                selectedIngredient = ingredientWithPackages
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add ingredient dialog
    if (showAddDialog) {
        IngredientDialog(
            ingredientWithPackages = null,
            onDismiss = { showAddDialog = false },
            onSave = { ingredient ->
                viewModel.addIngredient(ingredient)
                showAddDialog = false
            },
            onSavePackage = { pkg -> viewModel.addPackage(pkg) },
            onDeletePackage = { pkg -> viewModel.deletePackage(pkg) },
            onAutoSuggestCategory = { name -> viewModel.suggestCategory(name) }
        )
    }
    
    // Edit ingredient dialog
    if (showEditDialog && selectedIngredient != null) {
        IngredientDialog(
            ingredientWithPackages = selectedIngredient,
            onDismiss = {
                showEditDialog = false
                selectedIngredient = null
            },
            onSave = { ingredient ->
                viewModel.updateIngredient(ingredient)
                showEditDialog = false
                selectedIngredient = null
            },
            onDelete = { ingredient ->
                viewModel.deleteIngredient(ingredient)
                showEditDialog = false
                selectedIngredient = null
            },
            onSavePackage = { pkg -> viewModel.addPackage(pkg) },
            onUpdatePackage = { pkg -> viewModel.updatePackage(pkg) },
            onDeletePackage = { pkg -> viewModel.deletePackage(pkg) },
            onAutoSuggestCategory = { name -> viewModel.suggestCategory(name) }
        )
    }
    
    // Category filter dialog
    if (showCategoryFilter) {
        CategoryFilterDialog(
            selectedCategory = selectedCategory,
            onDismiss = { showCategoryFilter = false },
            onCategorySelected = { category ->
                viewModel.setSelectedCategory(category)
                showCategoryFilter = false
            }
        )
    }
    
    // Duplicates dialog
    if (showDuplicatesDialog) {
        DuplicatesDialog(
            duplicates = potentialDuplicates,
            onDismiss = { showDuplicatesDialog = false },
            onMerge = { keep, remove ->
                viewModel.mergeIngredients(keep, remove)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientCard(
    ingredientWithPackages: IngredientWithPackages,
    onClick: () -> Unit
) {
    val ingredient = ingredientWithPackages.ingredient
    val packages = ingredientWithPackages.packages
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                AssistChip(
                    onClick = {},
                    label = { Text(ingredient.category.localizedName(), style = MaterialTheme.typography.labelSmall) }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.default_unit_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ingredient.defaultUnit.abbreviation(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (packages.isNotEmpty()) {
                    Column {
                        Text(
                            text = stringResource(R.string.package_info_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        packages.take(2).forEach { pkg ->
                            Text(
                                text = "${pkg.packageType.localizedName()}: ${pkg.packageSize.toInt()}${pkg.packageUnit.abbreviation()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (packages.size > 2) {
                            Text(
                                text = "+${packages.size - 2} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientDialog(
    ingredientWithPackages: IngredientWithPackages?,
    onDismiss: () -> Unit,
    onSave: (Ingredient) -> Unit,
    onDelete: ((Ingredient) -> Unit)? = null,
    onSavePackage: (IngredientPackage) -> Unit,
    onUpdatePackage: ((IngredientPackage) -> Unit)? = null,
    onDeletePackage: (IngredientPackage) -> Unit,
    onAutoSuggestCategory: (String) -> IngredientCategory
) {
    val ingredient = ingredientWithPackages?.ingredient
    val packages = ingredientWithPackages?.packages ?: emptyList()
    
    var name by remember { mutableStateOf(ingredient?.name ?: "") }
    var category by remember { mutableStateOf(ingredient?.category ?: IngredientCategory.OTHER) }
    var defaultUnit by remember { mutableStateOf(ingredient?.defaultUnit ?: MeasurementUnit.GRAM) }
    
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showUnitMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPackageDialog by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<IngredientPackage?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (ingredient == null) stringResource(R.string.add_ingredient_title) else stringResource(R.string.edit_ingredient_title)) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name field with auto-suggest
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        // Auto-suggest category when name changes (only for new ingredients)
                        if (ingredient == null && it.length >= 3) {
                            category = onAutoSuggestCategory(it)
                        }
                    },
                    label = { Text(stringResource(R.string.ingredient_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = category.localizedName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        IngredientCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.localizedName()) },
                                onClick = {
                                    category = cat
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
                
                // Default unit dropdown
                ExposedDropdownMenuBox(
                    expanded = showUnitMenu,
                    onExpandedChange = { showUnitMenu = it }
                ) {
                    OutlinedTextField(
                        value = defaultUnit.localizedName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.default_unit_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUnitMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showUnitMenu,
                        onDismissRequest = { showUnitMenu = false }
                    ) {
                        MeasurementUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.localizedName()) },
                                onClick = {
                                    defaultUnit = unit
                                    showUnitMenu = false
                                }
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Package options section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.package_options),
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (ingredient != null) {
                        TextButton(onClick = { showPackageDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add))
                        }
                    }
                }
                
                if (ingredient != null && packages.isNotEmpty()) {
                    packages.forEach { pkg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pkg.packageType.localizedName(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${pkg.packageSize.toInt()} ${pkg.packageUnit.abbreviation()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingPackage = pkg
                                            showPackageDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeletePackage(pkg) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                } else if (ingredient != null) {
                    Text(
                        text = stringResource(R.string.no_packages_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.save_ingredient_first),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val savedIngredient = Ingredient(
                            ingredientId = ingredient?.ingredientId ?: 0,
                            name = name,
                            category = category,
                            defaultUnit = defaultUnit
                        )
                        onSave(savedIngredient)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (ingredient != null && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirmation = true }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
    
    // Package dialog
    if (showPackageDialog && ingredient != null) {
        PackageDialog(
            ingredientId = ingredient.ingredientId,
            existingPackage = editingPackage,
            onDismiss = {
                showPackageDialog = false
                editingPackage = null
            },
            onSave = { pkg ->
                if (editingPackage != null && onUpdatePackage != null) {
                    onUpdatePackage(pkg)
                } else {
                    onSavePackage(pkg)
                }
                showPackageDialog = false
                editingPackage = null
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation && ingredient != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_ingredient_title)) },
            text = { Text(stringResource(R.string.delete_ingredient_message, ingredient.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke(ingredient)
                        showDeleteConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageDialog(
    ingredientId: Long,
    existingPackage: IngredientPackage?,
    onDismiss: () -> Unit,
    onSave: (IngredientPackage) -> Unit
) {
    var packageType by remember { mutableStateOf(existingPackage?.packageType ?: PackageType.PACKAGE) }
    var packageSize by remember { mutableStateOf(existingPackage?.packageSize?.toString() ?: "") }
    var packageUnit by remember { mutableStateOf(existingPackage?.packageUnit ?: MeasurementUnit.GRAM) }
    
    var showPackageTypeMenu by remember { mutableStateOf(false) }
    var showPackageUnitMenu by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingPackage == null) stringResource(R.string.add_package) else stringResource(R.string.edit_package)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Package type dropdown
                ExposedDropdownMenuBox(
                    expanded = showPackageTypeMenu,
                    onExpandedChange = { showPackageTypeMenu = it }
                ) {
                    OutlinedTextField(
                        value = packageType.localizedName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.package_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPackageTypeMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showPackageTypeMenu,
                        onDismissRequest = { showPackageTypeMenu = false }
                    ) {
                        PackageType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.localizedName()) },
                                onClick = {
                                    packageType = type
                                    showPackageTypeMenu = false
                                }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Package size
                    OutlinedTextField(
                        value = packageSize,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) packageSize = it },
                        label = { Text(stringResource(R.string.package_size_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    // Package unit
                    ExposedDropdownMenuBox(
                        expanded = showPackageUnitMenu,
                        onExpandedChange = { showPackageUnitMenu = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = packageUnit.localizedName(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.unit_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPackageUnitMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showPackageUnitMenu,
                            onDismissRequest = { showPackageUnitMenu = false }
                        ) {
                            MeasurementUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.localizedName()) },
                                    onClick = {
                                        packageUnit = unit
                                        showPackageUnitMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val size = packageSize.toDoubleOrNull() ?: 0.0
                    if (size > 0) {
                        val pkg = IngredientPackage(
                            packageId = existingPackage?.packageId ?: 0,
                            ingredientId = ingredientId,
                            packageType = packageType,
                            packageSize = size,
                            packageUnit = packageUnit
                        )
                        onSave(pkg)
                    }
                },
                enabled = packageSize.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun CategoryFilterDialog(
    selectedCategory: IngredientCategory?,
    onDismiss: () -> Unit,
    onCategorySelected: (IngredientCategory?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_by_category)) },
        text = {
            Column {
                TextButton(
                    onClick = { onCategorySelected(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.all_categories), modifier = Modifier.fillMaxWidth())
                }
                
                HorizontalDivider()
                
                IngredientCategory.entries.forEach { category ->
                    TextButton(
                        onClick = { onCategorySelected(category) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.localizedName())
                            if (category == selectedCategory) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun DuplicatesDialog(
    duplicates: List<Pair<Ingredient, Ingredient>>,
    onDismiss: () -> Unit,
    onMerge: (keep: Ingredient, remove: Ingredient) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.potential_duplicates_title)) },
        text = {
            LazyColumn {
                items(duplicates) { (ing1, ing2) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${ing1.name} ↔ ${ing2.name}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onMerge(ing1, ing2) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Keep ${ing1.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Button(
                                    onClick = { onMerge(ing2, ing1) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Keep ${ing2.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

// Extension function for PackageType display
@Composable
fun PackageType.localizedName(): String {
    val context = LocalContext.current
    return when (this) {
        PackageType.UNIT -> context.getString(R.string.package_type_unit)
        PackageType.PACKAGE -> context.getString(R.string.package_type_package)
        PackageType.BULK -> context.getString(R.string.package_type_bulk)
        PackageType.BOTTLE -> context.getString(R.string.package_type_bottle)
        PackageType.CARTON -> context.getString(R.string.package_type_carton)
        PackageType.BAG -> context.getString(R.string.package_type_bag)
        PackageType.CAN -> context.getString(R.string.package_type_can)
        PackageType.JAR -> context.getString(R.string.package_type_jar)
        PackageType.OTHER -> context.getString(R.string.package_type_other)
    }
}

fun PackageType.displayName(): String {
    return when (this) {
        PackageType.UNIT -> "Unit"
        PackageType.PACKAGE -> "Package"
        PackageType.BULK -> "Bulk"
        PackageType.BOTTLE -> "Bottle"
        PackageType.CARTON -> "Carton"
        PackageType.BAG -> "Bag"
        PackageType.CAN -> "Can"
        PackageType.JAR -> "Jar"
        PackageType.OTHER -> "Other"
    }
}

// Extension function for IngredientCategory display
@Composable
fun IngredientCategory.localizedName(): String {
    val context = LocalContext.current
    return when (this) {
        IngredientCategory.MEAT -> context.getString(R.string.category_meat)
        IngredientCategory.FISH -> context.getString(R.string.category_fish)
        IngredientCategory.DAIRY -> context.getString(R.string.category_dairy)
        IngredientCategory.VEGETABLES -> context.getString(R.string.category_vegetables)
        IngredientCategory.FRUITS -> context.getString(R.string.category_fruits)
        IngredientCategory.PANTRY -> context.getString(R.string.category_pantry)
        IngredientCategory.SPICES -> context.getString(R.string.category_spices)
        IngredientCategory.BEVERAGES -> context.getString(R.string.category_beverages)
        IngredientCategory.OTHER -> context.getString(R.string.category_other)
    }
}

// Extension function for MeasurementUnit display
@Composable
fun MeasurementUnit.localizedName(): String {
    val context = LocalContext.current
    return when (this) {
        MeasurementUnit.GRAM -> context.getString(R.string.unit_gram)
        MeasurementUnit.KILOGRAM -> context.getString(R.string.unit_kilogram)
        MeasurementUnit.MILLILITER -> context.getString(R.string.unit_milliliter)
        MeasurementUnit.LITER -> context.getString(R.string.unit_liter)
        MeasurementUnit.TEASPOON -> context.getString(R.string.unit_teaspoon)
        MeasurementUnit.TABLESPOON -> context.getString(R.string.unit_tablespoon)
        MeasurementUnit.CUP -> context.getString(R.string.unit_cup)
        MeasurementUnit.PIECE -> context.getString(R.string.unit_piece)
    }
}

// Extension function for MeasurementUnit abbreviations
fun MeasurementUnit.abbreviation(): String {
    return when (this) {
        MeasurementUnit.GRAM -> "g"
        MeasurementUnit.KILOGRAM -> "kg"
        MeasurementUnit.MILLILITER -> "ml"
        MeasurementUnit.LITER -> "L"
        MeasurementUnit.TEASPOON -> "tsp"
        MeasurementUnit.TABLESPOON -> "tbsp"
        MeasurementUnit.CUP -> "cup"
        MeasurementUnit.PIECE -> "pc"
    }
}
