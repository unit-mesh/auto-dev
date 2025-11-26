package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.indexer.DomainDictGenerator
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.indexer.GenerationResult
import kotlinx.coroutines.launch

/**
 * Domain Dictionary Management Status
 */
sealed class DomainDictStatus {
    object Idle : DomainDictStatus()
    object Loading : DomainDictStatus()
    object Generating : DomainDictStatus()
    data class Success(val message: String) : DomainDictStatus()
    data class Error(val message: String) : DomainDictStatus()
}

/**
 * Domain Dictionary Management Dialog
 *
 * This dialog allows users to:
 * 1. View existing domain dictionary (CSV format)
 * 2. Edit the domain dictionary manually
 * 3. Regenerate the domain dictionary using LLM
 * 4. Save changes to the dictionary file
 *
 * The domain dictionary is used to enhance user prompts with project-specific terminology.
 *
 * @param workspace The current workspace for file operations
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun DomainDictDialog(
    workspace: Workspace,
    onDismiss: () -> Unit
) {
    var dictContent by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<DomainDictStatus>(DomainDictStatus.Idle) }
    var isEditing by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val fileSystem = workspace.fileSystem
    val domainDictService = remember { DomainDictService(fileSystem) }

    // Load existing dictionary on first composition
    LaunchedEffect(Unit) {
        status = DomainDictStatus.Loading
        try {
            val content = domainDictService.loadContent()
            dictContent = content ?: getDefaultDictTemplate()
            status = DomainDictStatus.Idle
        } catch (e: Exception) {
            dictContent = getDefaultDictTemplate()
            status = DomainDictStatus.Error("Failed to load: ${e.message}")
        }
    }

    // Save dictionary function
    fun saveDict() {
        scope.launch {
            status = DomainDictStatus.Loading
            try {
                val saved = domainDictService.saveContent(dictContent)
                if (saved) {
                    hasChanges = false
                    status = DomainDictStatus.Success("Dictionary saved successfully")
                } else {
                    status = DomainDictStatus.Error("Failed to save dictionary")
                }
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Save failed: ${e.message}")
            }
        }
    }

    // Generate dictionary using LLM
    fun generateDict() {
        scope.launch {
            status = DomainDictStatus.Generating
            try {
                val configWrapper = ConfigManager.load()
                val activeConfig = configWrapper.getActiveModelConfig()

                if (activeConfig == null || !activeConfig.isValid()) {
                    status = DomainDictStatus.Error("No valid LLM configuration. Please configure your model in Settings.")
                    return@launch
                }

                val generator = DomainDictGenerator(fileSystem, activeConfig)
                val result = generator.generateAndSave()

                when (result) {
                    is GenerationResult.Success -> {
                        dictContent = result.content
                        hasChanges = false
                        status = DomainDictStatus.Success("Dictionary generated and saved successfully!")
                    }
                    is GenerationResult.Error -> {
                        status = DomainDictStatus.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Generation failed: ${e.message}")
            }
        }
    }

    Dialog(onDismissRequest = {
        if (!hasChanges || status is DomainDictStatus.Generating) {
            onDismiss()
        }
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Domain Dictionary",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "prompts/domain.csv",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "About Domain Dictionary",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "The domain dictionary helps AI understand your project's terminology. " +
                                    "It maps Chinese terms to English code translations with descriptions. " +
                                    "Use Ctrl+P in the chat input to enhance prompts using this dictionary.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status bar
                when (val currentStatus = status) {
                    is DomainDictStatus.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is DomainDictStatus.Generating -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Generating domain dictionary using LLM...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is DomainDictStatus.Success -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentStatus.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is DomainDictStatus.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentStatus.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is DomainDictStatus.Idle -> {
                        if (hasChanges) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AutoDevComposeIcons.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Unsaved changes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Editor area
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Editor header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CSV Format: Chinese, Code Translation, Description",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Copy button
                                IconButton(
                                    onClick = {
                                        // TODO: Copy to clipboard
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // Content editor
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = dictContent,
                                onValueChange = {
                                    dictContent = it
                                    hasChanges = true
                                    if (status is DomainDictStatus.Success || status is DomainDictStatus.Error) {
                                        status = DomainDictStatus.Idle
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                enabled = status !is DomainDictStatus.Loading && status !is DomainDictStatus.Generating,
                                decorationBox = { innerTextField ->
                                    if (dictContent.isEmpty()) {
                                        Text(
                                            text = "Enter domain dictionary in CSV format...",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Generate button
                    OutlinedButton(
                        onClick = { generateDict() },
                        enabled = status !is DomainDictStatus.Loading && status !is DomainDictStatus.Generating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Custom.AI,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (status is DomainDictStatus.Generating) "Generating..." else "Generate with AI"
                        )
                    }

                    // Save button
                    Button(
                        onClick = { saveDict() },
                        enabled = hasChanges && status !is DomainDictStatus.Loading && status !is DomainDictStatus.Generating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Default dictionary template with example entries
 */
private fun getDefaultDictTemplate(): String {
    return """Chinese,Code Translation,Description
用户,User,System user entity
订单,Order,E-commerce order
商品,Product,Product or item
购物车,ShoppingCart,User shopping cart
支付,Payment,Payment processing
登录,Login,User authentication
注册,Register,User registration
""".trimIndent()
}

