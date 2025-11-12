package cc.unitmesh.devins.ui.compose.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.i18n.Language
import cc.unitmesh.devins.ui.i18n.LanguageManager
import kotlinx.coroutines.launch

/**
 * Language Switcher Component
 *
 * Provides a dropdown menu for switching between supported languages
 */
@Composable
fun LanguageSwitcher(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Language,
                contentDescription = "Language",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(currentLanguage.displayName)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(language.displayName)
                            if (language == currentLanguage) {
                                Icon(
                                    imageVector = AutoDevComposeIcons.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        scope.launch {
                            LanguageManager.setLanguage(language)
                            expanded = false
                        }
                    }
                )
            }
        }
    }
}
