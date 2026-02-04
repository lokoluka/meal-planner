package com.example.mealplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mealplanner.R
import com.example.mealplanner.data.DayOfWeek
import com.example.mealplanner.data.MealType
import com.example.mealplanner.data.WeeklyPlan
import com.example.mealplanner.ui.calendar.MealCalendarViewModel
import com.example.mealplanner.ui.LanguagePickerDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealCalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: MealCalendarViewModel = viewModel(),
    onNavigateToFamily: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val weeklyPlans by viewModel.weeklyPlans.collectAsState()
    val currentWeeklyPlanId by viewModel.currentWeeklyPlanId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (currentWeeklyPlanId == null) {
        WeeklyPlanListScreen(
            weeklyPlans = weeklyPlans,
            isLoading = isLoading,
            onPlanClick = { viewModel.selectWeeklyPlan(it.weeklyPlanId) },
            onCreatePlan = { name, date, commensals -> 
                viewModel.createWeeklyPlan(name, date, commensals) 
            },
            onDeletePlan = { viewModel.deleteWeeklyPlan(it) },
            onNavigateToFamily = onNavigateToFamily,
            onOpenDrawer = onOpenDrawer,
            modifier = modifier
        )
    } else {
        WeeklyPlanDetailScreen(
            viewModel = viewModel,
            onBack = { viewModel.deselectWeeklyPlan() },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanListScreen(
    weeklyPlans: List<WeeklyPlan>,
    isLoading: Boolean,
    onPlanClick: (WeeklyPlan) -> Unit,
    onCreatePlan: (String, Long, Int) -> Unit,
    onDeletePlan: (WeeklyPlan) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToFamily: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val syncViewModel: com.example.mealplanner.sync.SyncViewModel = viewModel()
    val isSyncing by syncViewModel.isSyncing.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var manageFamiliesPlanId by remember { mutableStateOf<Long?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weekly_meal_plans_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { syncViewModel.syncNow() },
                        enabled = !isSyncing
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_sync)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_create_plan))
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (weeklyPlans.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_weekly_plans),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(weeklyPlans, key = { it.weeklyPlanId }) { plan ->
                        WeeklyPlanCard(
                            plan = plan,
                            onClick = { onPlanClick(plan) },
                            onDelete = { onDeletePlan(plan) },
                            onManageFamilies = { manageFamiliesPlanId = plan.weeklyPlanId }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateWeeklyPlanDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, date, commensals ->
                    onCreatePlan(name, date, commensals)
                    showCreateDialog = false
                }
            )
        }

        manageFamiliesPlanId?.let { planId ->
            ManageFamiliesDialog(
                weeklyPlanId = planId,
                onDismiss = { manageFamiliesPlanId = null }
            )
        }

        if (showLanguageDialog) {
            LanguagePickerDialog(onDismiss = { showLanguageDialog = false })
        }
    }
}

@Composable
fun WeeklyPlanCard(
    plan: WeeklyPlan,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onManageFamilies: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val viewModel: MealCalendarViewModel = viewModel()
    val familyViewModel: com.example.mealplanner.family.FamilyViewModel = viewModel()
    val families by familyViewModel.families.collectAsState()
    var sharedFamilyNames by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(plan.weeklyPlanId) {
        val familyIds = viewModel.getFamiliesForWeeklyPlan(plan.weeklyPlanId)
        sharedFamilyNames = familyIds.mapNotNull { id ->
            families.find { it.family.familyId == id }?.family?.name
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(
                        R.string.week_of_people,
                        dateFormat.format(Date(plan.startDate)),
                        plan.commensals
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sharedFamilyNames.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.shared_with_families,
                            sharedFamilyNames.joinToString(", ")
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Manage families button
                TextButton(
                    onClick = onManageFamilies,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (sharedFamilyNames.isEmpty()) {
                            stringResource(R.string.share_with_family)
                        } else {
                            stringResource(R.string.manage_sharing)
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_plan),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_plan_title)) },
            text = { Text(stringResource(R.string.delete_plan_message, plan.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWeeklyPlanDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Long, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var commensals by remember { mutableStateOf("2") }
    val currentDate = remember { System.currentTimeMillis() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_weekly_plan_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.plan_name_label)) },
                    placeholder = { Text(stringResource(R.string.plan_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = commensals,
                    onValueChange = { commensals = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.number_of_people_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.share_after_creation_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val parsedCommensals = commensals.toIntOrNull() ?: 2
                    onCreate(name, currentDate, parsedCommensals) 
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFamiliesDialog(
    weeklyPlanId: Long,
    onDismiss: () -> Unit
) {
    val viewModel: MealCalendarViewModel = viewModel()
    val familyViewModel: com.example.mealplanner.family.FamilyViewModel = viewModel()
    val families by familyViewModel.families.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var selectedFamilyIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    LaunchedEffect(weeklyPlanId) {
        val currentFamilyIds = viewModel.getFamiliesForWeeklyPlan(weeklyPlanId)
        selectedFamilyIds = currentFamilyIds.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_with_families_title)) },
        text = {
            Column {
                if (families.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_families_yet_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.select_families_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    families.forEach { familyWithMembers ->
                        val family = familyWithMembers.family
                        val isSelected = selectedFamilyIds.contains(family.familyId)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFamilyIds = if (isSelected) {
                                        selectedFamilyIds - family.familyId
                                    } else {
                                        selectedFamilyIds + family.familyId
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedFamilyIds = if (checked) {
                                        selectedFamilyIds + family.familyId
                                    } else {
                                        selectedFamilyIds - family.familyId
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = family.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.members_count, familyWithMembers.members.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    coroutineScope.launch {
                        val currentFamilyIds = viewModel.getFamiliesForWeeklyPlan(weeklyPlanId)
                        
                        // Remove families that were unchecked
                        currentFamilyIds.forEach { familyId ->
                            if (!selectedFamilyIds.contains(familyId)) {
                                viewModel.removeWeeklyPlanFromFamily(weeklyPlanId, familyId)
                            }
                        }
                        
                        // Add families that were checked
                        val newFamilyIds = selectedFamilyIds.filter { !currentFamilyIds.contains(it) }
                        if (newFamilyIds.isNotEmpty()) {
                            viewModel.addWeeklyPlanToFamilies(weeklyPlanId, newFamilyIds.toList())
                        }
                        
                        onDismiss()
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanDetailScreen(
    viewModel: MealCalendarViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mealPlans by viewModel.mealPlans.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentWeeklyPlanId by viewModel.currentWeeklyPlanId.collectAsState()
    val selectedWeeklyPlan by viewModel.selectedWeeklyPlan.collectAsState()

    var showRecipeSelector by remember { mutableStateOf(false) }
    var showCommensalsDialog by remember { mutableStateOf(false) }
    var showEditPlanDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var selectedMealType by remember { mutableStateOf<MealType?>(null) }
    var selectedRecipeId by remember { mutableStateOf<Long?>(null) }
    var selectedServings by remember { mutableStateOf(1) }
    var showShoppingList by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var mealPlansSnapshot by remember { mutableStateOf<Map<Pair<DayOfWeek, MealType>, List<com.example.mealplanner.data.MealPlanWithRecipe>>?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val canEdit = isEditing

    // Save snapshot when entering edit mode
    LaunchedEffect(isEditing) {
        if (isEditing) {
            mealPlansSnapshot = mealPlans
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weekly_meals_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                // Discard changes - restore snapshot
                                if (mealPlansSnapshot != null) {
                                    viewModel.restoreMealPlans(mealPlansSnapshot!!)
                                }
                                isEditing = false
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_discard_changes),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = { isEditing = !isEditing }
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit, 
                            contentDescription = if (isEditing) {
                                stringResource(R.string.cd_done_editing)
                            } else {
                                stringResource(R.string.cd_edit_plan)
                            },
                            tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!isEditing) {
                        IconButton(
                            onClick = { showShoppingList = true },
                            enabled = mealPlans.isNotEmpty()
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(R.string.cd_shopping_list))
                        }
                    }
                    if (canEdit) {
                        TextButton(
                            onClick = { viewModel.clearAllMealsInWeek() },
                            enabled = mealPlans.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.clear_all))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sticky week header
                    if (selectedWeeklyPlan != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = selectedWeeklyPlan!!.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedWeeklyPlan!!.startDate)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.people_count, selectedWeeklyPlan!!.commensals),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    if (isEditing) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.editing_mode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(DayOfWeek.entries) { day ->
                            DayCard(
                                day = day,
                                mealPlans = mealPlans,
                                canEdit = canEdit,
                                onMealClick = { mealType ->
                                    if (canEdit) {
                                        selectedDay = day
                                        selectedMealType = mealType
                                        showRecipeSelector = true
                                    }
                                },
                                onRemoveMeal = { mealPlanId ->
                                    if (canEdit) {
                                        viewModel.removeMealPlan(mealPlanId)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        if (canEdit && showRecipeSelector && selectedDay != null && selectedMealType != null) {
            RecipeSelectorDialog(
                recipes = recipes,
                onDismiss = {
                    showRecipeSelector = false
                    selectedDay = null
                    selectedMealType = null
                },
                onRecipeSelected = { recipeId, servings ->
                    selectedRecipeId = recipeId
                    selectedServings = servings
                    showRecipeSelector = false
                    showCommensalsDialog = true
                }
            )
        }
        
        if (canEdit && showCommensalsDialog && selectedDay != null && selectedMealType != null && selectedRecipeId != null) {
            CommensalsDialog(
                defaultCommensals = selectedWeeklyPlan?.commensals ?: 2,
                onDismiss = {
                    showCommensalsDialog = false
                    selectedRecipeId = null
                    selectedDay = null
                    selectedMealType = null
                },
                onConfirm = { commensals ->
                    viewModel.assignRecipeToMeal(
                        dayOfWeek = selectedDay!!,
                        mealType = selectedMealType!!,
                        recipeId = selectedRecipeId!!,
                        servings = selectedServings,
                        commensals = commensals
                    )
                    showCommensalsDialog = false
                    selectedRecipeId = null
                    selectedDay = null
                    selectedMealType = null
                }
            )
        }
        
        if (showEditPlanDialog) {
            EditWeeklyPlanDialog(
                currentCommensals = selectedWeeklyPlan?.commensals ?: 2,
                onDismiss = { showEditPlanDialog = false },
                onConfirm = { commensals ->
                    viewModel.updateWeeklyPlanCommensals(commensals)
                    showEditPlanDialog = false
                }
            )
        }

        if (showShoppingList && currentWeeklyPlanId != null) {
            ShoppingListBottomSheet(
                weeklyPlanId = currentWeeklyPlanId!!,
                onDismiss = { showShoppingList = false }
            )
        }

        if (showLanguageDialog) {
            LanguagePickerDialog(onDismiss = { showLanguageDialog = false })
        }
    }
}

@Composable
fun DayCard(
    day: DayOfWeek,
    mealPlans: Map<Pair<DayOfWeek, MealType>, List<com.example.mealplanner.data.MealPlanWithRecipe>>,
    canEdit: Boolean,
    onMealClick: (MealType) -> Unit,
    onRemoveMeal: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = dayLabel(day),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            MealType.entries.forEach { mealType ->
                val mealPlansList = mealPlans[Pair(day, mealType)] ?: emptyList()
                MealSlot(
                    mealType = mealType,
                    mealPlans = mealPlansList,
                    canEdit = canEdit,
                    onClick = { onMealClick(mealType) },
                    onRemove = onRemoveMeal
                )
            }
        }
    }
}

@Composable
fun MealSlot(
    mealType: MealType,
    mealPlans: List<com.example.mealplanner.data.MealPlanWithRecipe>,
    canEdit: Boolean,
    onClick: () -> Unit,
    onRemove: (Long) -> Unit
) {
    val accentColor = mealTypeAccentColor(mealType, MaterialTheme.colorScheme)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mealPlans.isNotEmpty()) {
                accentColor.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 16.dp)
                            .padding(end = 8.dp)
                            .background(accentColor, shape = MaterialTheme.shapes.small)
                    )
                    Text(
                        text = mealTypeLabel(mealType),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (canEdit) {
                    IconButton(onClick = onClick) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_add_recipe),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (mealPlans.isEmpty()) {
                Text(
                    text = stringResource(R.string.tap_to_add_recipes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                mealPlans.forEach { mealPlan ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "• ${mealPlan.recipe.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.people_count, mealPlan.mealPlan.commensals),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        if (canEdit) {
                            IconButton(
                                onClick = { onRemove(mealPlan.mealPlan.mealPlanId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun mealTypeAccentColor(mealType: MealType, colorScheme: ColorScheme) = when (mealType) {
    MealType.LUNCH -> colorScheme.primary
    MealType.DINNER -> colorScheme.secondary
}

@Composable
private fun dayLabel(day: DayOfWeek): String {
    return when (day) {
        DayOfWeek.MONDAY -> stringResource(R.string.day_monday)
        DayOfWeek.TUESDAY -> stringResource(R.string.day_tuesday)
        DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wednesday)
        DayOfWeek.THURSDAY -> stringResource(R.string.day_thursday)
        DayOfWeek.FRIDAY -> stringResource(R.string.day_friday)
        DayOfWeek.SATURDAY -> stringResource(R.string.day_saturday)
        DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday)
    }
}

@Composable
private fun mealTypeLabel(mealType: MealType): String {
    return when (mealType) {
        MealType.LUNCH -> stringResource(R.string.meal_lunch)
        MealType.DINNER -> stringResource(R.string.meal_dinner)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSelectorDialog(
    recipes: List<com.example.mealplanner.data.RecipeWithIngredients>,
    onDismiss: () -> Unit,
    onRecipeSelected: (Long, Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_recipe_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (recipes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_recipes_available),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(recipes, key = { it.recipe.recipeId }) { recipe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onRecipeSelected(recipe.recipe.recipeId, 1)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recipe.recipe.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.recipe_card_subtitle,
                                            recipe.ingredients.size,
                                            recipe.recipe.servings
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.select),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListBottomSheet(
    weeklyPlanId: Long,
    onDismiss: () -> Unit
) {
    val viewModel: com.example.mealplanner.ui.shopping.ShoppingListViewModel = viewModel()
    val shoppingList by viewModel.shoppingList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(weeklyPlanId) {
        viewModel.loadShoppingListForPlan(weeklyPlanId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.shopping_list_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                shoppingList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_ingredients_yet),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(shoppingList, key = { "${it.ingredientName}_${it.unit}" }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.toggleItemChecked(item.ingredientName, item.unit) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                            onCheckedChange = { viewModel.toggleItemChecked(item.ingredientName, item.unit) }
                                        )
                                        Text(
                                            text = item.ingredientName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                            ),
                                            color = if (item.isChecked) 
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                                            else 
                                                MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    Text(
                                        text = "${String.format("%.1f", item.totalAmount)} ${item.unit.name.lowercase()}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (item.isChecked) 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) 
                                        else 
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommensalsDialog(
    defaultCommensals: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var commensalsText by remember { mutableStateOf(defaultCommensals.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.number_of_people_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.commensals_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = commensalsText,
                    onValueChange = { 
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            commensalsText = it
                        }
                    },
                    label = { Text(stringResource(R.string.commensals_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val commensals = commensalsText.toIntOrNull() ?: defaultCommensals
                    if (commensals > 0) {
                        onConfirm(commensals)
                    }
                }
            ) {
                Text(stringResource(R.string.confirm))
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
fun EditWeeklyPlanDialog(
    currentCommensals: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var commensalsText by remember { mutableStateOf(currentCommensals.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_weekly_plan_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.default_commensals_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = commensalsText,
                    onValueChange = { 
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            commensalsText = it
                        }
                    },
                    label = { Text(stringResource(R.string.default_commensals_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val commensals = commensalsText.toIntOrNull() ?: currentCommensals
                    if (commensals > 0) {
                        onConfirm(commensals)
                    }
                }
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
