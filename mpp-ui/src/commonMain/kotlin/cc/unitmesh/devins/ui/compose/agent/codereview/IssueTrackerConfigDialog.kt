package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.config.IssueTrackerConfig
import kotlinx.coroutines.launch

/**
 * Dialog for configuring issue tracker settings
 * 
 * Allows users to configure:
 * - Issue tracker type (GitHub, GitLab, Jira, etc.)
 * - API token
 * - Repository information (owner, name)
 * - Server URL (for self-hosted instances)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueTrackerConfigDialog(
    onDismiss: () -> Unit,
    onConfigured: () -> Unit = {},
    initialConfig: IssueTrackerConfig? = null
) {
    var type by remember { mutableStateOf(initialConfig?.type ?: "github") }
    var token by remember { mutableStateOf(initialConfig?.token ?: "") }
    var repoOwner by remember { mutableStateOf(initialConfig?.repoOwner ?: "") }
    var repoName by remember { mutableStateOf(initialConfig?.repoName ?: "") }
    var serverUrl by remember { mutableStateOf(initialConfig?.serverUrl ?: "") }
    var enabled by remember { mutableStateOf(initialConfig?.enabled ?: true) }
    var showToken by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Issue Tracker Configuration",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Enable/Disable toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Issue Tracker",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // Type selector
                    Text(
                        text = "Issue Tracker Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = type == "github",
                            onClick = { type = "github" },
                            label = { Text("GitHub") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = type == "gitlab",
                            onClick = { type = "gitlab" },
                            label = { Text("GitLab") },
                            modifier = Modifier.weight(1f),
                            enabled = false // TODO: Implement GitLab support
                        )
                        FilterChip(
                            selected = type == "jira",
                            onClick = { type = "jira" },
                            label = { Text("Jira") },
                            modifier = Modifier.weight(1f),
                            enabled = false // TODO: Implement Jira support
                        )
                    }
                    
                    // Token field
                    Text(
                        text = "API Token",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your API token") },
                        visualTransformation = if (showToken) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) {
                                        AutoDevComposeIcons.VisibilityOff
                                    } else {
                                        AutoDevComposeIcons.Visibility
                                    },
                                    contentDescription = if (showToken) "Hide token" else "Show token"
                                )
                            }
                        },
                        singleLine = true
                    )
                    
                    Text(
                        text = when (type) {
                            "github" -> "Create a personal access token at: https://github.com/settings/tokens"
                            "gitlab" -> "Create a personal access token in your GitLab settings"
                            "jira" -> "Create an API token in your Atlassian account settings"
                            else -> "API token for authentication"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Repository fields (for GitHub/GitLab)
                    if (type == "github" || type == "gitlab") {
                        Text(
                            text = "Repository Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = repoOwner,
                            onValueChange = { repoOwner = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Repository Owner") },
                            placeholder = { Text("e.g., unitmesh") },
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = repoName,
                            onValueChange = { repoName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Repository Name") },
                            placeholder = { Text("e.g., auto-dev") },
                            singleLine = true
                        )
                        
                        Text(
                            text = "Leave empty to auto-detect from Git remote URL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Server URL (for GitLab/Jira)
                    if (type == "gitlab" || type == "jira") {
                        Text(
                            text = "Server URL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Server URL") },
                            placeholder = { 
                                Text(
                                    when (type) {
                                        "gitlab" -> "https://gitlab.com"
                                        "jira" -> "https://your-domain.atlassian.net"
                                        else -> "Server URL"
                                    }
                                )
                            },
                            singleLine = true
                        )
                    }
                    
                    // Error message
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = AutoDevComposeIcons.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                
                                try {
                                    val config = IssueTrackerConfig(
                                        type = type,
                                        token = token,
                                        repoOwner = repoOwner.trim(),
                                        repoName = repoName.trim(),
                                        serverUrl = serverUrl.trim(),
                                        enabled = enabled
                                    )
                                    
                                    // Validate configuration
                                    if (enabled) {
                                        if (token.isBlank()) {
                                            errorMessage = "API token is required"
                                            isSaving = false
                                            return@launch
                                        }
                                        
                                        if (type == "github" || type == "gitlab") {
                                            // Allow empty repo fields for auto-detection
                                            // Validation will happen in IssueService
                                        }
                                        
                                        if (type == "gitlab" || type == "jira") {
                                            if (serverUrl.isBlank()) {
                                                errorMessage = "Server URL is required for ${type.uppercase()}"
                                                isSaving = false
                                                return@launch
                                            }
                                        }
                                    }
                                    
                                    // Save configuration
                                    ConfigManager.saveIssueTracker(config)
                                    
                                    isSaving = false
                                    onConfigured()
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorMessage = "Failed to save configuration: ${e.message}"
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isSaving) "Saving..." else "Save")
                    }
                }
            }
        }
    }
}

