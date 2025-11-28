package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.indexer.DomainDictGenerator
import cc.unitmesh.indexer.DomainDictService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Domain Dictionary Status
 */
sealed class DomainDictStatus {
    object Idle : DomainDictStatus()
    object Loading : DomainDictStatus()
    data class Scanning(val message: String = "Scanning project files...") : DomainDictStatus()
    data class Generating(val progress: String = "", val lineCount: Int = 0) : DomainDictStatus()
    data class Success(val message: String) : DomainDictStatus()
    data class Error(val message: String) : DomainDictStatus()
}

/**
 * Dictionary entry model
 */
data class DictEntry(
    val chinese: String,
    val english: String,
    val description: String
) {
    fun toCsvLine(): String = "$chinese,$english,$description"

    companion object {
        fun fromCsvLine(line: String): DictEntry? {
            val parts = line.split(",", limit = 3)
            return if (parts.size >= 2) {
                DictEntry(
                    chinese = parts[0].trim(),
                    english = parts[1].trim(),
                    description = parts.getOrElse(2) { "" }.trim()
                )
            } else null
        }

        fun parseCSV(content: String): List<DictEntry> {
            return content.lines()
                .drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { fromCsvLine(it) }
        }

        fun toCSV(entries: List<DictEntry>): String {
            val header = "Chinese,Code Translation,Description"
            val rows = entries.map { it.toCsvLine() }
            return (listOf(header) + rows).joinToString("\n")
        }
    }
}

/**
 * Compact Domain Dictionary Dialog for Desktop
 *
 * Features:
 * - Focused table view for editing entries
 * - AI-powered dictionary generation with streaming
 * - Non-blocking async operations
 */
@Composable
fun DomainDictDialog(
    workspace: Workspace,
    onDismiss: () -> Unit
) {
    var entries by remember { mutableStateOf<List<DictEntry>>(emptyList()) }
    var status by remember { mutableStateOf<DomainDictStatus>(DomainDictStatus.Idle) }
    var hasChanges by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val fileSystem = workspace.fileSystem
    val domainDictService = remember { DomainDictService(fileSystem) }
    val listState = rememberLazyListState()

    // Load existing dictionary
    LaunchedEffect(Unit) {
        status = DomainDictStatus.Loading
        try {
            val content = withContext(Dispatchers.Default) {
                domainDictService.loadContent()
            }
            entries = DictEntry.parseCSV(content ?: "")
            status = DomainDictStatus.Idle
        } catch (e: Exception) {
            entries = emptyList()
            status = DomainDictStatus.Error("Failed to load: ${e.message}")
        }
    }

    fun saveDict() {
        scope.launch {
            status = DomainDictStatus.Loading
            try {
                val content = DictEntry.toCSV(entries)
                val saved = withContext(Dispatchers.Default) {
                    domainDictService.saveContent(content)
                }
                if (saved) {
                    hasChanges = false
                    status = DomainDictStatus.Success("Saved ${entries.size} entries")
                } else {
                    status = DomainDictStatus.Error("Failed to save")
                }
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Save failed: ${e.message}")
            }
        }
    }

    fun generateDict() {
        scope.launch {
            status = DomainDictStatus.Scanning("Scanning project files...")
            streamingContent = ""

            try {
                val configWrapper = ConfigManager.load()
                val activeConfig = configWrapper.getActiveModelConfig()

                if (activeConfig == null || !activeConfig.isValid()) {
                    status = DomainDictStatus.Error("No valid LLM config. Configure in Settings.")
                    return@launch
                }

                val generator = DomainDictGenerator(fileSystem, activeConfig)

                var lineCount = 0
                val resultBuilder = StringBuilder()

                // The flow now handles prompt building internally (non-blocking)
                generator.generateStreaming().collect { chunk ->
                    resultBuilder.append(chunk)
                    streamingContent = resultBuilder.toString()

                    val newLineCount = streamingContent.count { it == '\n' }
                    if (newLineCount > lineCount) {
                        lineCount = newLineCount
                        status = DomainDictStatus.Generating("Generating...", lineCount)
                    } else if (status is DomainDictStatus.Scanning) {
                        status = DomainDictStatus.Generating("Generating...", 0)
                    }
                }

                val generatedContent = resultBuilder.toString()
                val saved = withContext(Dispatchers.Default) {
                    domainDictService.saveContent(generatedContent)
                }

                if (saved) {
                    entries = DictEntry.parseCSV(generatedContent)
                    hasChanges = false
                    streamingContent = ""
                    status = DomainDictStatus.Success("Generated ${entries.size} entries")
                } else {
                    status = DomainDictStatus.Error("Failed to save generated content")
                }
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Generation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    val isLoading = status is DomainDictStatus.Loading
    val isGenerating = status is DomainDictStatus.Generating || status is DomainDictStatus.Scanning

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isGenerating,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(640.dp)
                .heightIn(min = 480.dp, max = 720.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                DialogHeader(
                    entriesCount = entries.size,
                    hasChanges = hasChanges,
                    onClose = { if (!isGenerating) onDismiss() }
                )

                // Status indicator
                StatusBar(status)

                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    when {
                        isGenerating && streamingContent.isNotEmpty() -> {
                            StreamingView(content = streamingContent)
                        }
                        isLoading || (isGenerating && streamingContent.isEmpty()) -> {
                            LoadingView(
                                message = when (status) {
                                    is DomainDictStatus.Loading -> "Loading..."
                                    is DomainDictStatus.Scanning -> (status as DomainDictStatus.Scanning).message
                                    else -> "Please wait..."
                                }
                            )
                        }
                        else -> {
                            DictTableView(
                                entries = entries,
                                listState = listState,
                                onUpdateEntry = { index, entry ->
                                    entries = entries.toMutableList().apply { set(index, entry) }
                                    hasChanges = true
                                    if (status is DomainDictStatus.Success || status is DomainDictStatus.Error) {
                                        status = DomainDictStatus.Idle
                                    }
                                },
                                onDeleteEntry = { index ->
                                    entries = entries.toMutableList().apply { removeAt(index) }
                                    hasChanges = true
                                },
                                enabled = !isLoading && !isGenerating
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action bar
                ActionBar(
                    onAddEntry = {
                        entries = entries + DictEntry("", "", "")
                        hasChanges = true
                        scope.launch {
                            listState.animateScrollToItem(entries.size - 1)
                        }
                    },
                    onGenerate = { generateDict() },
                    onSave = { saveDict() },
                    hasChanges = hasChanges,
                    isGenerating = isGenerating,
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
private fun DialogHeader(
    entriesCount: Int,
    hasChanges: Boolean,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.MenuBook,
                contentDescription = null,
                tint = AutoDevColors.Indigo.c500,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Domain Dictionary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (hasChanges) {
                        Text(
                            text = "*",
                            color = AutoDevColors.Amber.c500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Text(
                    text = "prompts/domain.csv - $entriesCount entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = AutoDevComposeIcons.Close,
                contentDescription = "Close",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatusBar(status: DomainDictStatus) {
    val (bgColor, textColor, icon, message) = when (status) {
        is DomainDictStatus.Generating -> Quadruple(
            AutoDevColors.Indigo.c100.copy(alpha = 0.6f),
            AutoDevColors.Indigo.c700,
            null,
            "${status.progress} (${status.lineCount} entries)"
        )
        is DomainDictStatus.Scanning -> Quadruple(
            AutoDevColors.Indigo.c100.copy(alpha = 0.6f),
            AutoDevColors.Indigo.c700,
            null,
            status.message
        )
        is DomainDictStatus.Success -> Quadruple(
            AutoDevColors.Green.c100.copy(alpha = 0.6f),
            AutoDevColors.Green.c700,
            AutoDevComposeIcons.CheckCircle,
            status.message
        )
        is DomainDictStatus.Error -> Quadruple(
            AutoDevColors.Red.c100.copy(alpha = 0.6f),
            AutoDevColors.Red.c700,
            AutoDevComposeIcons.Error,
            status.message
        )
        else -> return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (status is DomainDictStatus.Generating || status is DomainDictStatus.Scanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AutoDevColors.Indigo.c500
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun LoadingView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = AutoDevColors.Indigo.c500
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreamingView(content: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AutoDevColors.Indigo.c200),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        val scrollState = rememberLazyListState()
        val lines = remember(content) { content.lines() }

        // Auto-scroll to bottom
        LaunchedEffect(lines.size) {
            if (lines.isNotEmpty()) {
                scrollState.animateScrollToItem(lines.size - 1)
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            items(lines.size) { index ->
                Text(
                    text = lines[index],
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun DictTableView(
    entries: List<DictEntry>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onUpdateEntry: (Int, DictEntry) -> Unit,
    onDeleteEntry: (Int) -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chinese",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.25f)
                )
                Text(
                    text = "Code Translation",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.35f)
                )
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.35f)
                )
                Spacer(modifier = Modifier.width(32.dp))
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (entries.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(entries, key = { index, _ -> index }) { index, entry ->
                        DictRow(
                            entry = entry,
                            isOdd = index % 2 == 1,
                            onUpdate = { onUpdateEntry(index, it) },
                            onDelete = { onDeleteEntry(index) },
                            enabled = enabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No entries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add entries or generate with AI",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DictRow(
    entry: DictEntry,
    isOdd: Boolean,
    onUpdate: (DictEntry) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean
) {
    var chinese by remember(entry) { mutableStateOf(entry.chinese) }
    var english by remember(entry) { mutableStateOf(entry.english) }
    var description by remember(entry) { mutableStateOf(entry.description) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isOdd) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactTextField(
            value = chinese,
            onValueChange = {
                chinese = it
                onUpdate(DictEntry(it, english, description))
            },
            placeholder = "Term",
            enabled = enabled,
            modifier = Modifier.weight(0.25f)
        )
        CompactTextField(
            value = english,
            onValueChange = {
                english = it
                onUpdate(DictEntry(chinese, it, description))
            },
            placeholder = "Code",
            enabled = enabled,
            modifier = Modifier.weight(0.35f)
        )
        CompactTextField(
            value = description,
            onValueChange = {
                description = it
                onUpdate(DictEntry(chinese, english, it))
            },
            placeholder = "Desc",
            enabled = enabled,
            modifier = Modifier.weight(0.35f)
        )
        IconButton(
            onClick = onDelete,
            enabled = enabled,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        textStyle = TextStyle(
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(AutoDevColors.Indigo.c500),
        enabled = enabled,
        singleLine = true,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun ActionBar(
    onAddEntry: () -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    hasChanges: Boolean,
    isGenerating: Boolean,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add entry
        OutlinedButton(
            onClick = onAddEntry,
            enabled = !isLoading && !isGenerating,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Generate
        OutlinedButton(
            onClick = onGenerate,
            enabled = !isLoading && !isGenerating,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AutoDevColors.Indigo.c600
            ),
            border = BorderStroke(1.dp, AutoDevColors.Indigo.c300)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Custom.AI,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isGenerating) "Generating..." else "Generate",
                fontSize = 13.sp
            )
        }

        // Save
        Button(
            onClick = onSave,
            enabled = hasChanges && !isLoading && !isGenerating,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AutoDevColors.Indigo.c600,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Save,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save", fontSize = 13.sp)
        }
    }
}
