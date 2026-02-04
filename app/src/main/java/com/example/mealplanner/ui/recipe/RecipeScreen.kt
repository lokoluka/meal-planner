package com.example.mealplanner.ui.recipe

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mealplanner.R
import com.example.mealplanner.data.IngredientAmount
import com.example.mealplanner.data.MeasurementUnit
import com.example.mealplanner.data.RecipeWithIngredients
import com.example.mealplanner.ui.LanguagePickerDialog
import com.example.mealplanner.ai.RecipeImportService
import com.example.mealplanner.ai.ParsedRecipe
import kotlinx.coroutines.launch

enum class RecipeSortOption {
    NAME, INGREDIENTS, SERVINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    modifier: Modifier = Modifier,
    viewModel: RecipeViewModel = viewModel(),
    onBack: () -> Unit = {},
    onNavigateToIngredients: () -> Unit = {}
) {
    val recipes by viewModel.recipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editingRecipe by remember { mutableStateOf<RecipeWithIngredients?>(null) }
    var parsedRecipeData by remember { mutableStateOf<ParsedRecipe?>(null) }
    var quickViewRecipe by remember { mutableStateOf<RecipeWithIngredients?>(null) }
    var quickViewIngredients by remember { mutableStateOf<List<IngredientAmount>>(emptyList()) }
    var showQuickView by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(RecipeSortOption.NAME) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

    val filteredRecipes = remember(recipes, searchQuery, sortOption) {
        val tokens = searchQuery
            .lowercase()
            .split(',', ';', '\n', '\t', ' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val filtered = if (tokens.isEmpty()) {
            recipes
        } else {
            recipes.filter { recipe ->
                val ingredientNames = recipe.ingredients.map { it.name.lowercase() }
                tokens.all { token -> ingredientNames.any { name -> name.contains(token) } }
            }
        }
        
        // Apply sorting
        when (sortOption) {
            RecipeSortOption.NAME -> filtered.sortedBy { it.recipe.name.lowercase() }
            RecipeSortOption.INGREDIENTS -> filtered.sortedBy { it.ingredients.size }
            RecipeSortOption.SERVINGS -> filtered.sortedBy { it.recipe.servings }
        }
    }

    LaunchedEffect(showQuickView, quickViewRecipe?.recipe?.recipeId) {
        if (showQuickView && quickViewRecipe != null) {
            quickViewIngredients = viewModel.getIngredientsForRecipe(quickViewRecipe!!.recipe.recipeId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recipes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_sort))
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cd_manage_ingredients)) },
                                onClick = {
                                    showSortMenu = false
                                    onNavigateToIngredients()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_name_az)) },
                                onClick = {
                                    sortOption = RecipeSortOption.NAME
                                    showSortMenu = false
                                },
                                trailingIcon = if (sortOption == RecipeSortOption.NAME) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ingredients)) },
                                onClick = {
                                    sortOption = RecipeSortOption.INGREDIENTS
                                    showSortMenu = false
                                },
                                trailingIcon = if (sortOption == RecipeSortOption.INGREDIENTS) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.servings)) },
                                onClick = {
                                    sortOption = RecipeSortOption.SERVINGS
                                    showSortMenu = false
                                },
                                trailingIcon = if (sortOption == RecipeSortOption.SERVINGS) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { showImportDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_import_recipe), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                FloatingActionButton(
                    onClick = {
                        editingRecipe = null
                        showDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_recipe))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (recipes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.no_recipes_yet),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.no_recipes_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            editingRecipe = null
                            showDialog = true
                        }
                    ) {
                        Text(stringResource(R.string.add_recipe))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.filter_by_ingredients)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredRecipes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_matching_recipes),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(filteredRecipes, key = { it.recipe.recipeId }) { recipe ->
                                RecipeCard(
                                    recipe = recipe,
                                    recipeTypeIcon = recipeTypeIcon(recipe),
                                    onQuickView = {
                                        quickViewRecipe = recipe
                                        showQuickView = true
                                    },
                                    onEdit = {
                                        editingRecipe = recipe
                                        showDialog = true
                                    },
                                    onDelete = { viewModel.deleteRecipe(recipe.recipe) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            val scope = rememberCoroutineScope()
            AddEditRecipeDialog(
                recipe = editingRecipe,
                onDismiss = { 
                    showDialog = false
                    parsedRecipeData = null
                },
                onSave = { name, instructions, servings, ingredients ->
                    scope.launch {
                        if (editingRecipe != null) {
                            viewModel.updateRecipe(
                                recipeId = editingRecipe!!.recipe.recipeId,
                                name = name,
                                instructions = instructions,
                                servings = servings,
                                ingredients = ingredients
                            )
                        } else {
                            viewModel.addRecipe(
                                name = name,
                                instructions = instructions,
                                servings = servings,
                                ingredients = ingredients
                            )
                        }
                        showDialog = false
                        parsedRecipeData = null
                    }
                },
                viewModel = viewModel,
                parsedRecipeData = parsedRecipeData
            )
        }

        if (showQuickView && quickViewRecipe != null) {
            QuickViewSheet(
                recipe = quickViewRecipe!!,
                ingredientAmounts = quickViewIngredients,
                onDismiss = { showQuickView = false }
            )
        }
        
        if (showImportDialog) {
            ImportRecipeDialog(
                onDismiss = { showImportDialog = false },
                onRecipeParsed = { parsedRecipe ->
                    parsedRecipeData = parsedRecipe
                    showImportDialog = false
                    editingRecipe = null
                    showDialog = true
                }
            )
        }

        if (showLanguageDialog) {
            LanguagePickerDialog(onDismiss = { showLanguageDialog = false })
        }
    }
}

@Composable
fun RecipeCard(
    recipe: RecipeWithIngredients,
    recipeTypeIcon: ImageVector,
    onQuickView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onQuickView() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.recipe.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(
                            R.string.recipe_card_subtitle,
                            recipe.ingredients.size,
                            recipe.recipe.servings
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quick_view)) },
                            onClick = {
                                showMenu = false
                                onQuickView()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share)) },
                            onClick = {
                                showMenu = false
                                // TODO: Implement share functionality
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_recipe_title)) },
            text = { Text(stringResource(R.string.delete_recipe_message, recipe.recipe.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickViewSheet(
    recipe: RecipeWithIngredients,
    ingredientAmounts: List<IngredientAmount>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = recipe.recipe.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(
                    R.string.recipe_card_subtitle,
                    recipe.ingredients.size,
                    recipe.recipe.servings
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (recipe.recipe.instructions.isNotEmpty()) {
                Text(text = stringResource(R.string.instructions), style = MaterialTheme.typography.titleSmall)
                Text(text = recipe.recipe.instructions, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (ingredientAmounts.isNotEmpty()) {
                Text(text = stringResource(R.string.ingredients), style = MaterialTheme.typography.titleSmall)
                ingredientAmounts.forEach { ingredient ->
                    Text(
                        text = "• ${ingredient.name} - ${ingredient.amount.toFractionString()} ${ingredient.unit.abbreviation()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (recipe.ingredients.isNotEmpty()) {
                Text(text = stringResource(R.string.ingredients), style = MaterialTheme.typography.titleSmall)
                recipe.ingredients.forEach { ingredient ->
                    Text(text = "• ${ingredient.name}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) { Text(stringResource(R.string.close)) }
        }
    }
}

private fun recipeTypeIcon(recipe: RecipeWithIngredients): ImageVector {
    return when {
        recipe.ingredients.size <= 5 -> Icons.Default.Menu
        recipe.recipe.servings >= 6 -> Icons.Default.Share
        else -> Icons.Default.MoreVert
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecipeDialog(
    recipe: RecipeWithIngredients?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, List<IngredientWithAmount>) -> Unit,
    viewModel: RecipeViewModel,
    parsedRecipeData: ParsedRecipe? = null
) {
    var name by remember { mutableStateOf(parsedRecipeData?.name ?: recipe?.recipe?.name ?: "") }
    var instructions by remember { mutableStateOf(parsedRecipeData?.instructions ?: recipe?.recipe?.instructions ?: "") }
    var servings by remember { mutableStateOf(parsedRecipeData?.servings?.toString() ?: recipe?.recipe?.servings?.toString() ?: "1") }
    var ingredients by remember { 
        mutableStateOf<List<IngredientWithAmount>>(
            if (parsedRecipeData != null) {
                parsedRecipeData.ingredients.map {
                    IngredientWithAmount(
                        name = it.name,
                        amount = it.amount,
                        unit = it.toMeasurementUnit(),
                        category = it.toIngredientCategory()
                    )
                }
            } else {
                emptyList()
            }
        )
    }
    var isLoadingIngredients by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(recipe) {
        if (recipe != null && parsedRecipeData == null) {
            isLoadingIngredients = true
            val loadedIngredients = viewModel.getIngredientsForRecipe(recipe.recipe.recipeId)
            ingredients = loadedIngredients.map {
                IngredientWithAmount(name = it.name, amount = it.amount, unit = it.unit)
            }
            isLoadingIngredients = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
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
                    text = if (recipe == null) stringResource(R.string.add_recipe_title) else stringResource(R.string.edit_recipe_title),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.recipe_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = servings,
                onValueChange = { servings = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(R.string.servings)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text(stringResource(R.string.instructions)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.ingredients), style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(
                    onClick = {
                        ingredients = ingredients + IngredientWithAmount(
                            name = "",
                            amount = 1.0,
                            unit = MeasurementUnit.PIECE
                        )
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_ingredient_cd))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingIngredients) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                IngredientTableHeader()
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    itemsIndexed(ingredients) { index, ingredient ->
                        IngredientItem(
                            ingredient = ingredient,
                            onIngredientChange = { updatedIngredient ->
                                ingredients = ingredients.toMutableList().apply { this[index] = updatedIngredient }
                            },
                            onDelete = {
                                ingredients = ingredients.toMutableList().apply { removeAt(index) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val parsedServings = servings.toIntOrNull() ?: 1
                        if (name.isNotBlank()) {
                            onSave(name, instructions, parsedServings, ingredients.filter { it.name.isNotBlank() && it.amount > 0 })
                        }
                    },
                    enabled = name.isNotBlank() && 
                              servings.toIntOrNull()?.let { it > 0 } == true &&
                              ingredients.isNotEmpty() &&
                              ingredients.any { it.name.isNotBlank() && it.amount > 0 }
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@Composable
private fun IngredientTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.ingredient_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.qty_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.unit_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(86.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(32.dp))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientItem(
    ingredient: IngredientWithAmount,
    onIngredientChange: (IngredientWithAmount) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val viewModel: RecipeViewModel = viewModel()
    val allIngredients by viewModel.allIngredients.collectAsState()
    var showSuggestions by remember { mutableStateOf(false) }

    val suggestions = remember(ingredient.name, allIngredients) {
        if (ingredient.name.isNotBlank()) {
            allIngredients.filter { it.name.contains(ingredient.name, ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = ingredient.name,
                onValueChange = {
                    onIngredientChange(ingredient.copy(name = it))
                    showSuggestions = it.isNotBlank()
                },
                placeholder = { Text(stringResource(R.string.ingredient_placeholder)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                )
            )

            TextField(
                value = if (ingredient.amount == 0.0) "" else ingredient.amount.toString(),
                onValueChange = {
                    val amount = if (it.isEmpty()) 0.0 else (it.toDoubleOrNull() ?: ingredient.amount)
                    onIngredientChange(ingredient.copy(amount = amount))
                },
                placeholder = { Text(stringResource(R.string.amount_placeholder)) },
                modifier = Modifier.width(90.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                )
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.width(86.dp)
            ) {
                TextField(
                    value = ingredient.unit.abbreviation(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    MeasurementUnit.entries.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.abbreviation(), style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onIngredientChange(ingredient.copy(unit = unit))
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            FilledTonalIconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove), modifier = Modifier.size(18.dp))
            }
        }
        
        // Category selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = ingredient.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    com.example.mealplanner.data.IngredientCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    category.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            onClick = {
                                onIngredientChange(ingredient.copy(category = category))
                                categoryExpanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (showSuggestions && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column {
                    suggestions.take(5).forEach { existingIngredient ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${existingIngredient.name} (${existingIngredient.defaultUnit.abbreviation()}, ${existingIngredient.category.name.lowercase()})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            onClick = {
                                onIngredientChange(
                                    ingredient.copy(
                                        name = existingIngredient.name,
                                        unit = existingIngredient.defaultUnit,
                                        category = existingIngredient.category
                                    )
                                )
                                showSuggestions = false
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun MeasurementUnit.abbreviation(): String {
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

@Composable
fun ImportRecipeDialog(
    onDismiss: () -> Unit,
    onRecipeParsed: (ParsedRecipe) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val importService = remember { RecipeImportService() }
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_recipe_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.paste_recipe_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { 
                        inputText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text(stringResource(R.string.paste_recipe_placeholder)) },
                    enabled = !isLoading,
                    maxLines = 10
                )
                
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.parsing_recipe))
                    }
                }
                
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        if (inputText.isBlank()) {
                            errorMessage = context.getString(R.string.please_enter_recipe)
                            return@launch
                        }
                        
                        isLoading = true
                        errorMessage = null
                        
                        importService.parseRecipe(inputText)
                            .onSuccess { parsedRecipe ->
                                onRecipeParsed(parsedRecipe)
                                onDismiss()
                            }
                            .onFailure { exception ->
                                errorMessage = context.getString(
                                    R.string.failed_parse_recipe,
                                    exception.message ?: ""
                                )
                            }
                        
                        isLoading = false
                    }
                },
                enabled = !isLoading && inputText.isNotBlank()
            ) {
                Text(stringResource(R.string.import_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
