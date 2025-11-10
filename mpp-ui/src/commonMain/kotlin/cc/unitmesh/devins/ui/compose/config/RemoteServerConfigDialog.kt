package cc.unitmesh.devins.ui.compose.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Remote Server Configuration Data
 */
data class RemoteServerConfig(
    val serverUrl: String = "http://localhost:8080",
    val useServerConfig: Boolean = false,
    val selectedProjectId: String = "",
    val defaultGitUrl: String = "" // 可选：用户可以预设一个 Git URL
)

/**
 * Dialog for configuring remote server connection
 * 
 * This allows users to:
 * - Enter remote server URL
 * - Choose whether to use server's LLM config or client's
 * - Select a project (will be loaded after connection)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteServerConfigDialog(
    currentConfig: RemoteServerConfig = RemoteServerConfig(),
    onDismiss: () -> Unit,
    onSave: (RemoteServerConfig) -> Unit
) {
    var serverUrl by remember { mutableStateOf(currentConfig.serverUrl) }
    var useServerConfig by remember { mutableStateOf(currentConfig.useServerConfig) }
    var defaultGitUrl by remember { mutableStateOf(currentConfig.defaultGitUrl) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var showAdvanced by remember { mutableStateOf(currentConfig.defaultGitUrl.isNotBlank()) }

    // Validate URL
    fun validateUrl(url: String): Boolean {
        if (url.isBlank()) {
            urlError = "Server URL cannot be empty"
            return false
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            urlError = "URL must start with http:// or https://"
            return false
        }
        
        urlError = null
        return true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = "🌐 Remote Server Configuration",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connect to a remote mpp-server to run coding agents",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (currentConfig.serverUrl.isNotBlank() && currentConfig.serverUrl != "http://localhost:8080") {
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "Hide Advanced" else "Show Advanced")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Server URL
                Text(
                    text = "Server URL",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { 
                        serverUrl = it
                        urlError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("http://localhost:8080") },
                    supportingText = {
                        Text(
                            urlError ?: "Enter the base URL of your mpp-server instance",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (urlError != null) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    isError = urlError != null,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // LLM Config Source
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Server's LLM Configuration",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (useServerConfig) {
                                    "Using LLM config from server"
                                } else {
                                    "Using your local LLM config"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = useServerConfig,
                            onCheckedChange = { useServerConfig = it }
                        )
                    }
                }

                // Advanced Options
                if (showAdvanced) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Advanced Options",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Default Git URL (optional)
                    OutlinedTextField(
                        value = defaultGitUrl,
                        onValueChange = { defaultGitUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://github.com/user/repo.git (optional)") },
                        label = { Text("Default Git URL") },
                        supportingText = {
                            Text(
                                "Pre-fill a Git repository URL. You can still change it later or select from available projects.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Git URL"
                            )
                        },
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ℹ️",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Column {
                            Text(
                                text = "After connecting, you can:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Select from existing projects on the server",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Paste any Git URL directly in the project selector",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• The server will auto-clone the repository",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (validateUrl(serverUrl)) {
                                onSave(
                                    RemoteServerConfig(
                                        serverUrl = serverUrl.trimEnd('/'),
                                        useServerConfig = useServerConfig,
                                        selectedProjectId = defaultGitUrl.takeIf { it.isNotBlank() } ?: "",
                                        defaultGitUrl = defaultGitUrl
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}

