package com.lokosoft.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lokosoft.mealplanner.repository.DataMigrationManager

@Composable
fun DataMigrationDialog(
    migrationState: DataMigrationManager.MigrationState,
    migrationProgress: Float,
    onMigrateClick: (clearLocal: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { 
            if (migrationState !is DataMigrationManager.MigrationState.InProgress) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = when (migrationState) {
                    is DataMigrationManager.MigrationState.InProgress -> "Migrating Data..."
                    is DataMigrationManager.MigrationState.Completed -> "Migration Complete"
                    is DataMigrationManager.MigrationState.Failed -> "Migration Failed"
                    else -> "Migrate Local Data?"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (migrationState) {
                    is DataMigrationManager.MigrationState.Idle -> {
                        Text(
                            text = "You have recipes saved locally. Would you like to sync them to your account?",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This will make them available across all your devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    is DataMigrationManager.MigrationState.InProgress -> {
                        LinearProgressIndicator(
                            progress = { migrationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${(migrationProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    is DataMigrationManager.MigrationState.Completed -> {
                        Text(
                            text = "✓ Your recipes have been successfully synced to your account!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    is DataMigrationManager.MigrationState.Failed -> {
                        Text(
                            text = "Failed to migrate data: ${migrationState.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (migrationState) {
                is DataMigrationManager.MigrationState.Idle -> {
                    Button(onClick = { onMigrateClick(false) }) {
                        Text("Sync Now")
                    }
                }
                is DataMigrationManager.MigrationState.Completed -> {
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
                is DataMigrationManager.MigrationState.Failed -> {
                    Button(onClick = { onMigrateClick(false) }) {
                        Text("Retry")
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (migrationState is DataMigrationManager.MigrationState.Idle) {
                TextButton(onClick = onDismiss) {
                    Text("Skip")
                }
            } else if (migrationState is DataMigrationManager.MigrationState.Failed) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
