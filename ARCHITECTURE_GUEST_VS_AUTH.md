# Guest vs Authenticated User Architecture

## Overview

This app implements a **dual-storage strategy** that provides a seamless experience for both guest and authenticated users:

- **Guest Users (Anonymous)**: Data stored locally using Room database
- **Authenticated Users**: Data synced to Firebase Firestore with local caching

## Architecture Components

### 1. Repository Pattern

#### `RecipeRepository` Interface
Defines common operations for recipe data management, abstracting away the storage implementation.

#### `LocalRecipeRepository`
- **Used by**: Guest/Anonymous users
- **Storage**: Room database only
- **Benefits**: 
  - Works completely offline
  - Fast access
  - No authentication required
- **Limitation**: Data is device-specific and can be lost

#### `FirebaseRecipeRepository`
- **Used by**: Authenticated users (Google Sign-In)
- **Storage**: Firebase Firestore + Room cache
- **Benefits**:
  - Data synced across devices
  - Cloud backup
  - Offline-first with local cache
- **Process**:
  1. Writes go to both local and cloud
  2. Reads prioritize cloud, fall back to local cache
  3. Local cache updated when syncing from cloud

### 2. Data Migration System

#### `DataMigrationManager`
Handles the transition when a guest user signs in:

**Flow:**
1. User starts as anonymous guest
2. Creates recipes locally (Room)
3. User signs in with Google
4. System detects local data exists
5. Shows migration dialog
6. User chooses to sync or skip
7. If synced, data copied to Firebase
8. Optionally clears local data after successful migration

**Migration States:**
- `Idle`: No migration in progress
- `InProgress`: Currently migrating (shows progress %)
- `Completed`: Migration successful
- `Failed`: Migration failed with error message

### 3. ViewModel Integration

#### `RecipeViewModel`
- Automatically switches repositories based on auth state
- Call `onAuthStateChanged()` when user signs in/out
- Transparently uses correct storage backend

```kotlin
// Repository selection logic
private fun getRepository(): RecipeRepository {
    val user = auth.currentUser
    return if (user != null && !user.isAnonymous) {
        FirebaseRecipeRepository(recipeDao)  // Authenticated
    } else {
        LocalRecipeRepository(recipeDao)     // Guest
    }
}
```

#### `AuthViewModel`
- Manages authentication state
- Triggers migration check after sign-in
- Provides migration controls:
  - `migrateData(clearLocal)`: Start migration
  - `dismissMigrationDialog()`: Skip migration
  - `migrationState`: Observable migration status
  - `migrationProgress`: Observable progress (0.0 - 1.0)

## Usage in UI

### 1. Auth Screen Integration

```kotlin
@Composable
fun YourAuthScreen(authViewModel: AuthViewModel = viewModel()) {
    val showMigrationDialog by authViewModel.showMigrationDialog.collectAsState()
    val migrationState by authViewModel.migrationState.collectAsState()
    val migrationProgress by authViewModel.migrationProgress.collectAsState()
    
    // Show migration dialog when needed
    if (showMigrationDialog) {
        DataMigrationDialog(
            migrationState = migrationState,
            migrationProgress = migrationProgress,
            onMigrateClick = { clearLocal ->
                authViewModel.migrateData(clearLocal)
            },
            onDismiss = { authViewModel.dismissMigrationDialog() }
        )
    }
}
```

### 2. Recipe Screen Integration

```kotlin
@Composable
fun RecipeScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    
    // Update repository when auth state changes
    LaunchedEffect(user) {
        recipeViewModel.onAuthStateChanged()
    }
    
    // Use recipes normally - repository handles storage
    val recipes by recipeViewModel.recipes.collectAsState()
}
```

## User Experience Flow

### Scenario 1: Guest → Authenticated User

1. **User opens app**: Anonymous auth, uses `LocalRecipeRepository`
2. **User adds recipes**: Saved to Room database
3. **User signs in with Google**: 
   - Auth state changes from anonymous → authenticated
   - System checks for local data
   - Migration dialog appears
4. **User chooses "Sync Now"**:
   - Shows progress bar
   - Each recipe uploaded to Firestore
   - Shows "Migration Complete"
5. **Future operations**: Automatically use `FirebaseRecipeRepository`
6. **Data available on other devices**: User can access from any device

### Scenario 2: Authenticated User from Start

1. **User opens app**: Signs in immediately
2. **System detects no local data**: No migration needed
3. **All operations**: Use `FirebaseRecipeRepository` from start
4. **Seamless multi-device**: Data synced across all devices

### Scenario 3: Guest Stays Guest

1. **User opens app**: Anonymous auth
2. **User never signs in**: Continues using `LocalRecipeRepository`
3. **Data persists**: Available on same device until app is uninstalled
4. **No cloud sync**: Data remains local only

## Benefits of This Architecture

✅ **Low barrier to entry**: Users can try the app without creating an account
✅ **Seamless transition**: Guest data preserved when upgrading to account
✅ **Offline-first**: Works without internet for all users
✅ **Multi-device sync**: Authenticated users get cloud benefits
✅ **Clean separation**: Repository pattern keeps storage logic separate
✅ **Testable**: Easy to mock repositories for testing
✅ **Flexible**: Can add more storage backends (e.g., other cloud providers)

## Implementation Checklist

- [x] Create `RecipeRepository` interface
- [x] Implement `LocalRecipeRepository` (Room)
- [x] Implement `FirebaseRecipeRepository` (Firestore + Room)
- [x] Create `DataMigrationManager`
- [x] Update `RecipeViewModel` to use repositories
- [x] Add migration triggers in `AuthViewModel`
- [x] Create `DataMigrationDialog` UI component
- [ ] Integrate dialog in auth flow UI
- [ ] Test migration flow
- [ ] Add similar pattern for meal plans (optional)
- [ ] Add migration for family data (optional)

## Future Enhancements

1. **Conflict Resolution**: Handle cases where user has data on multiple devices
2. **Selective Sync**: Let users choose which recipes to sync
3. **Background Sync**: Sync data when app is in background
4. **Sync Indicator**: Show sync status in UI
5. **Merge Strategy**: Smart merging when both local and cloud data exist
6. **Export/Import**: Allow manual data export for backup

## Best Practices

1. **Always check auth state** before choosing repository
2. **Handle offline gracefully** - both repositories work offline
3. **Show migration progress** - keep users informed
4. **Don't force migration** - let users decide
5. **Preserve local data** - only clear after successful migration
6. **Log errors** - help debug migration issues
7. **Test edge cases** - network failures, partial migrations, etc.

## Testing Strategy

### Unit Tests
- Test each repository independently
- Mock Firebase for `FirebaseRecipeRepository` tests
- Test migration logic in isolation

### Integration Tests
- Test repository switching
- Test full migration flow
- Test offline behavior

### UI Tests
- Test migration dialog interactions
- Test auth state changes
- Test data visibility after migration

## Troubleshooting

**Migration fails:**
- Check Firebase auth token is valid
- Check Firestore permissions
- Check network connectivity
- Review error logs

**Data not syncing:**
- Verify user is authenticated (not anonymous)
- Check `FirebaseRecipeRepository` is being used
- Verify Firestore rules allow write access
- Check for any error logs

**Recipes missing after sign-in:**
- Check if migration was completed
- Verify data exists in Firestore console
- Check local cache is being updated
- Review sync logs
