package com.example.mealplanner.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mealplanner.R
import com.example.mealplanner.utils.LocaleHelper

@Composable
fun LanguagePickerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage = remember { LocaleHelper.getLanguage(context) }

    fun selectLanguage(languageCode: String) {
        LocaleHelper.setLocale(context, languageCode)
        onDismiss()
        
        // Restart the app to apply language change
        val activity = context as? Activity
        activity?.let {
            val intent = it.packageManager.getLaunchIntentForPackage(it.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            it.startActivity(intent)
            it.finish()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageOption(
                    label = stringResource(R.string.language_system),
                    selected = currentLanguage == "system",
                    onSelect = { selectLanguage("system") }
                )
                LanguageOption(
                    label = stringResource(R.string.language_english),
                    selected = currentLanguage == "en",
                    onSelect = { selectLanguage("en") }
                )
                LanguageOption(
                    label = stringResource(R.string.language_spanish),
                    selected = currentLanguage == "es",
                    onSelect = { selectLanguage("es") }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.language_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        RadioButton(selected = selected, onClick = onSelect)
    }
}
