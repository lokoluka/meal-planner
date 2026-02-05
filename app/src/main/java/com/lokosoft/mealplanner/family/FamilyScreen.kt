package com.lokosoft.mealplanner.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokosoft.mealplanner.R
import com.lokosoft.mealplanner.ui.LanguagePickerDialog
import com.lokosoft.mealplanner.auth.AuthViewModel
import com.lokosoft.mealplanner.data.Family
import com.lokosoft.mealplanner.data.FamilyMember
import com.lokosoft.mealplanner.data.FamilyWithMembers
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamilyViewModel = viewModel()
) {
    val families by viewModel.families.collectAsState()
    val selectedFamily by viewModel.selectedFamily.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authViewModel: AuthViewModel = viewModel()
    
    if (selectedFamily == null) {
        var showPlans by remember { mutableStateOf(false) }
        val familyViewModel: FamilyViewModel = viewModel()
        val familyPlans by familyViewModel.familyPlans.collectAsState()
        FamilyListScreen(
            families = families,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onFamilyClick = { viewModel.selectFamily(it.family.familyId) },
            onCreateFamily = { viewModel.createFamily(it) },
            onDeleteFamily = { viewModel.deleteFamily(it) },
            onBack = onBack,
            onSignOut = { authViewModel.signOut() },
            modifier = modifier
        )
    } else {
        FamilyDetailScreen(
            family = selectedFamily!!,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onBack = { viewModel.deselectFamily() },
            onAddMember = { viewModel.addMember(selectedFamily!!.family.familyId, it) },
            onRemoveMember = { viewModel.removeMember(it) },
            onSignOut = { authViewModel.signOut() },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyListScreen(
    families: List<FamilyWithMembers>,
    isLoading: Boolean,
    errorMessage: String?,
    onFamilyClick: (FamilyWithMembers) -> Unit,
    onCreateFamily: (String) -> Unit,
    onDeleteFamily: (Family) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.families_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                }
            )

        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_create_family))
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (families.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.no_families_yet),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.no_families_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(families) { familyWithMembers ->
                        FamilyCard(
                            family = familyWithMembers,
                            isOwner = familyWithMembers.family.ownerId == currentUserId,
                            onClick = { onFamilyClick(familyWithMembers) },
                            onDelete = { onDeleteFamily(familyWithMembers.family) }
                        )
                    }
                }

                if (showSignOutDialog) {
                    AlertDialog(
                        onDismissRequest = { showSignOutDialog = false },
                        title = { Text(stringResource(R.string.sign_out_title)) },
                        text = { Text(stringResource(R.string.sign_out_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onSignOut()
                                    showSignOutDialog = false
                                }
                            ) { Text(stringResource(R.string.sign_out)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSignOutDialog = false }) { Text(stringResource(R.string.cancel)) }
                        }
                    )
                }
            }
            
            errorMessage?.let {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(it)
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateFamilyDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreateFamily(name)
                showCreateDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(onDismiss = { showLanguageDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyCard(
    family: FamilyWithMembers,
    isOwner: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                    text = family.family.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.members_count, family.members.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (isOwner) {
                    Text(
                        text = stringResource(R.string.owner),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (isOwner) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_family))
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_family_title)) },
            text = { Text(stringResource(R.string.delete_family_message)) },
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

@Composable
fun CreateFamilyDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var familyName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_family_title)) },
        text = {
            Column {
                Text(stringResource(R.string.create_family_prompt))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    label = { Text(stringResource(R.string.family_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(familyName) },
                enabled = familyName.isNotBlank()
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
fun FamilyDetailScreen(
    family: FamilyWithMembers,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (FamilyMember) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showPlans by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val familyViewModel: FamilyViewModel = viewModel()
    val familyPlans by familyViewModel.familyPlans.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isOwner = family.family.ownerId == currentUserId
    val dateFormat = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(family.family.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                }
            )
        },
        floatingActionButton = {
            if (isOwner) {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_member))
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.family_information),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.members_count_line, family.members.size))
                    Text(
                        stringResource(
                            R.string.created_on,
                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                .format(family.family.createdDate)
                        )
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.weekly_plans),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showPlans = !showPlans }) {
                            Text(if (showPlans) stringResource(R.string.hide) else stringResource(R.string.show))
                        }
                    }

                    if (showPlans) {
                        if (familyPlans.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_shared_plans),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                familyPlans.forEach { plan ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = dateFormat.format(java.util.Date(plan.startDate)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                if (showSignOutDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showSignOutDialog = false },
                                        title = { Text(stringResource(R.string.sign_out_title)) },
                                        text = { Text(stringResource(R.string.sign_out_message)) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    onSignOut()
                                                    showSignOutDialog = false
                                                }
                                            ) { Text(stringResource(R.string.sign_out)) }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showSignOutDialog = false }) { Text(stringResource(R.string.cancel)) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Text(
                stringResource(R.string.members_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(family.members) { member ->
                    MemberCard(
                        member = member,
                        isOwner = isOwner && member.userId != family.family.ownerId,
                        onRemove = { onRemoveMember(member) }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddMemberDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { email ->
                onAddMember(email)
                showAddDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(onDismiss = { showLanguageDialog = false })
    }
}

@Composable
fun MemberCard(
    member: FamilyMember,
    isOwner: Boolean,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = member.displayName ?: stringResource(R.string.invited),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            if (isOwner) {
                IconButton(onClick = { showRemoveDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_remove_member),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.remove_member_title)) },
            text = { Text(stringResource(R.string.remove_member_message, member.email)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    }
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_member_title)) },
        text = {
            Column {
                Text(stringResource(R.string.add_member_prompt))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(email) },
                enabled = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
            }
        }
    )
}
