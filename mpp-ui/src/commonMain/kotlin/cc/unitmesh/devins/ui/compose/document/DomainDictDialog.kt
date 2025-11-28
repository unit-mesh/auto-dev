package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
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
    data class Generating(val progress: String = "") : DomainDictStatus()
    data class Success(val message: String) : DomainDictStatus()
    data class Error(val message: String) : DomainDictStatus()
}

/**
 * Represents a single row in the domain dictionary CSV
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
                .drop(1) // Skip header
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
 * View mode for the dictionary
 */
enum class DictViewMode {
    TABLE,  // Table view for editing
    RAW     // Raw CSV text view
}

/**
 * Domain Dictionary Management Dialog
 *
 * This dialog allows users to:
 * 1. View existing domain dictionary in table format
 * 2. Edit entries directly in the table
 * 3. Regenerate the domain dictionary using LLM with streaming output
 * 4. Save changes to the dictionary file
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
    var entries by remember { mutableStateOf<List<DictEntry>>(emptyList()) }
    var status by remember { mutableStateOf<DomainDictStatus>(DomainDictStatus.Idle) }
    var hasChanges by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(DictViewMode.TABLE) }
    var streamingOutput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val fileSystem = workspace.fileSystem
    val domainDictService = remember { DomainDictService(fileSystem) }

    // Sync entries to content when entries change
    LaunchedEffect(entries) {
        if (entries.isNotEmpty()) {
            dictContent = DictEntry.toCSV(entries)
        }
    }

    // Sync content to entries when in raw mode
    fun syncContentToEntries() {
        entries = DictEntry.parseCSV(dictContent)
    }

    // Load existing dictionary on first composition
    LaunchedEffect(Unit) {
        status = DomainDictStatus.Loading
        try {
            val content = domainDictService.loadContent()
            dictContent = content ?: ""
            entries = DictEntry.parseCSV(dictContent)
            status = DomainDictStatus.Idle
            println("[DomainDict] Loaded ${entries.size} entries")
        } catch (e: Exception) {
            dictContent = ""
            entries = DictEntry.parseCSV(dictContent)
            status = DomainDictStatus.Error("Failed to load: ${e.message}")
            println("[DomainDict] Error loading: ${e.message}")
        }
    }

    fun saveDict() {
        scope.launch {
            status = DomainDictStatus.Loading
            try {
                val contentToSave = if (viewMode == DictViewMode.TABLE) {
                    DictEntry.toCSV(entries)
                } else {
                    dictContent
                }
                val saved = domainDictService.saveContent(contentToSave)
                if (saved) {
                    hasChanges = false
                    status = DomainDictStatus.Success("Dictionary saved successfully (${entries.size} entries)")
                    println("[DomainDict] Saved ${entries.size} entries")
                } else {
                    status = DomainDictStatus.Error("Failed to save dictionary")
                }
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Save failed: ${e.message}")
                println("[DomainDict] Save error: ${e.message}")
            }
        }
    }

    fun generateDict() {
        scope.launch {
            status = DomainDictStatus.Generating("Preparing...")
            streamingOutput = ""

            try {
                val configWrapper = ConfigManager.load()
                val activeConfig = configWrapper.getActiveModelConfig()

                if (activeConfig == null || !activeConfig.isValid()) {
                    status = DomainDictStatus.Error("No valid LLM configuration. Please configure your model in Settings.")
                    return@launch
                }

                status = DomainDictStatus.Generating("Analyzing project files...")

                val generator = DomainDictGenerator(fileSystem, activeConfig)

                // Use streaming generation
                var lineCount = 0
                val resultBuilder = StringBuilder()

                generator.generateStreaming().collect { chunk ->
                    resultBuilder.append(chunk)
                    streamingOutput = resultBuilder.toString()

                    // Update progress based on lines received
                    val newLineCount = streamingOutput.count { it == '\n' }
                    if (newLineCount > lineCount) {
                        lineCount = newLineCount
                        status = DomainDictStatus.Generating("Generating... ($lineCount entries)")
                    }
                }

                // Save the generated content
                val generatedContent = resultBuilder.toString()
                val saved = domainDictService.saveContent(generatedContent)

                if (saved) {
                    dictContent = generatedContent
                    entries = DictEntry.parseCSV(generatedContent)
                    hasChanges = false
                    streamingOutput = ""
                    status = DomainDictStatus.Success("Generated ${entries.size} entries successfully!")
                    println("[DomainDict] Generated ${entries.size} entries")
                } else {
                    status = DomainDictStatus.Error("Generation completed but failed to save")
                }
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Generation failed: ${e.message}")
                println("[DomainDict] Generation error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    Dialog(onDismissRequest = {
        if (status !is DomainDictStatus.Generating) {
            onDismiss()
        }
    }) {
        Surface(
            modifier = Modifier
                .widthIn(min = 800.dp, max = 1200.dp)
                .heightIn(min = 600.dp, max = 900.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                DictDialogHeader(onDismiss = onDismiss)

                Spacer(modifier = Modifier.height(20.dp))

                // Status bar
                DictStatusBar(
                    status = status,
                    hasChanges = hasChanges,
                    entriesCount = entries.size
                )

                Spacer(modifier = Modifier.height(16.dp))

                // View mode toggle
                DictViewModeToggle(
                    viewMode = viewMode,
                    onModeChange = { newMode ->
                        if (newMode != viewMode) {
                            if (viewMode == DictViewMode.RAW) {
                                syncContentToEntries()
                            }
                            viewMode = newMode
                        }
                    },
                    enabled = status !is DomainDictStatus.Loading && status !is DomainDictStatus.Generating
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Main content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        status is DomainDictStatus.Generating && streamingOutput.isNotEmpty() -> {
                            // Show streaming output during generation
                            StreamingOutputView(
                                content = streamingOutput,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        viewMode == DictViewMode.TABLE -> {
                            DictTableView(
                                entries = entries,
                                onEntriesChange = { newEntries ->
                                    entries = newEntries
                                    hasChanges = true
                                    if (status is DomainDictStatus.Success || status is DomainDictStatus.Error) {
                                        status = DomainDictStatus.Idle
                                    }
                                },
                                enabled = status !is DomainDictStatus.Loading && status !is DomainDictStatus.Generating,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            DictRawView(
                                content = dictContent,
                                onContentChange = { newContent ->
                                    dictContent = newContent
                                    hasChanges = true
                                    if (status is DomainDictStatus.Success || status is DomainDictStatus.Error) {
                                        status = DomainDictStatus.Idle
                                    }
                                },
                                enabled = status !is DomainDictStatus.Loading && status !is DomainDictStatus.Generating,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                DictActionButtons(
                    onGenerate = { generateDict() },
                    onSave = { saveDict() },
                    onAddEntry = {
                        entries = entries + DictEntry("", "", "")
                        hasChanges = true
                    },
                    hasChanges = hasChanges,
                    isGenerating = status is DomainDictStatus.Generating,
                    isLoading = status is DomainDictStatus.Loading,
                    showAddEntry = viewMode == DictViewMode.TABLE
                )
            }
        }
    }
}

@Composable
private fun DictDialogHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.MenuBook,
                contentDescription = null,
                tint = AutoDevColors.Indigo.c500,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = "Domain Dictionary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "prompts/domain.csv",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Close,
                contentDescription = "Close",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DictStatusBar(
    status: DomainDictStatus,
    hasChanges: Boolean,
    entriesCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when (status) {
                    is DomainDictStatus.Generating -> AutoDevColors.Indigo.c100.copy(alpha = 0.5f)
                    is DomainDictStatus.Success -> AutoDevColors.Green.c100.copy(alpha = 0.5f)
                    is DomainDictStatus.Error -> AutoDevColors.Red.c100.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (val currentStatus = status) {
            is DomainDictStatus.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AutoDevColors.Indigo.c500
                )
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            is DomainDictStatus.Generating -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AutoDevColors.Indigo.c500
                )
                Text(
                    text = currentStatus.progress.ifEmpty { "Generating domain dictionary..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AutoDevColors.Indigo.c700,
                    fontSize = 14.sp
                )
            }

            is DomainDictStatus.Success -> {
                Icon(
                    imageVector = AutoDevComposeIcons.CheckCircle,
                    contentDescription = null,
                    tint = AutoDevColors.Green.c600,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = currentStatus.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AutoDevColors.Green.c700,
                    fontSize = 14.sp
                )
            }

            is DomainDictStatus.Error -> {
                Icon(
                    imageVector = AutoDevComposeIcons.Error,
                    contentDescription = null,
                    tint = AutoDevColors.Red.c600,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = currentStatus.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AutoDevColors.Red.c700,
                    fontSize = 14.sp
                )
            }

            is DomainDictStatus.Idle -> {
                if (hasChanges) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Edit,
                        contentDescription = null,
                        tint = AutoDevColors.Amber.c600,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Unsaved changes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AutoDevColors.Amber.c700,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "$entriesCount entries loaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DictViewModeToggle(
    viewMode: DictViewMode,
    onModeChange: (DictViewMode) -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "View:",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 14.sp
        )

        FilterChip(
            selected = viewMode == DictViewMode.TABLE,
            onClick = { onModeChange(DictViewMode.TABLE) },
            label = {
                Text("Table", fontSize = 13.sp)
            },
            leadingIcon = if (viewMode == DictViewMode.TABLE) {
                {
                    Icon(
                        imageVector = AutoDevComposeIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else null,
            enabled = enabled
        )

        FilterChip(
            selected = viewMode == DictViewMode.RAW,
            onClick = { onModeChange(DictViewMode.RAW) },
            label = {
                Text("Raw CSV", fontSize = 13.sp)
            },
            leadingIcon = if (viewMode == DictViewMode.RAW) {
                {
                    Icon(
                        imageVector = AutoDevComposeIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else null,
            enabled = enabled
        )
    }
}

@Composable
private fun DictTableView(
    entries: List<DictEntry>,
    onEntriesChange: (List<DictEntry>) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderCell("Chinese", Modifier.weight(1f))
                TableHeaderCell("Code Translation", Modifier.weight(1f))
                TableHeaderCell("Description", Modifier.weight(1.5f))
                Spacer(modifier = Modifier.width(48.dp)) // For delete button
            }

            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Table Content
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No entries yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Click 'Generate with AI' or add entries manually",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = entries,
                        key = { index, _ -> index }
                    ) { index, entry ->
                        DictTableRow(
                            entry = entry,
                            index = index,
                            onEntryChange = { newEntry ->
                                val newList = entries.toMutableList()
                                newList[index] = newEntry
                                onEntriesChange(newList)
                            },
                            onDelete = {
                                val newList = entries.toMutableList()
                                newList.removeAt(index)
                                onEntriesChange(newList)
                            },
                            enabled = enabled
                        )

                        if (index < entries.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 14.sp,
        modifier = modifier
    )
}

@Composable
private fun DictTableRow(
    entry: DictEntry,
    index: Int,
    onEntryChange: (DictEntry) -> Unit,
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
                if (index % 2 == 0) Color.Transparent
                else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.3f)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditableTableCell(
            value = chinese,
            onValueChange = {
                chinese = it
                onEntryChange(DictEntry(it, english, description))
            },
            placeholder = "Chinese term",
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )

        EditableTableCell(
            value = english,
            onValueChange = {
                english = it
                onEntryChange(DictEntry(chinese, it, description))
            },
            placeholder = "Code translation",
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )

        EditableTableCell(
            value = description,
            onValueChange = {
                description = it
                onEntryChange(DictEntry(chinese, english, it))
            },
            placeholder = "Description",
            enabled = enabled,
            modifier = Modifier.weight(1.5f)
        )

        IconButton(
            onClick = onDelete,
            enabled = enabled,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Delete,
                contentDescription = "Delete entry",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EditableTableCell(
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
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                else Color.Transparent
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(AutoDevColors.Indigo.c500),
        enabled = enabled,
        singleLine = true,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun DictRawView(
    content: String,
    onContentChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CSV Format: Chinese, Code Translation, Description",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = "${content.lines().size} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            HorizontalDivider()

            // Content editor
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(AutoDevColors.Indigo.c500),
                    enabled = enabled,
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                text = "Enter domain dictionary in CSV format...",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
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
}

@Composable
private fun StreamingOutputView(
    content: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AutoDevColors.Indigo.c300),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AutoDevColors.Indigo.c100.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AutoDevColors.Indigo.c500
                )
                Text(
                    text = "Generating domain dictionary...",
                    style = MaterialTheme.typography.labelMedium,
                    color = AutoDevColors.Indigo.c700,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${content.lines().size} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = AutoDevColors.Indigo.c600,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = AutoDevColors.Indigo.c200)

            // Streaming content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = content,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun DictActionButtons(
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onAddEntry: () -> Unit,
    hasChanges: Boolean,
    isGenerating: Boolean,
    isLoading: Boolean,
    showAddEntry: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add Entry button (only in table mode)
        if (showAddEntry) {
            OutlinedButton(
                onClick = onAddEntry,
                enabled = !isLoading && !isGenerating,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Entry", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Generate button
        OutlinedButton(
            onClick = onGenerate,
            enabled = !isLoading && !isGenerating,
            modifier = Modifier.height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AutoDevColors.Indigo.c600
            ),
            border = BorderStroke(1.dp, AutoDevColors.Indigo.c300)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Custom.AI,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGenerating) "Generating..." else "Generate with AI",
                fontSize = 14.sp
            )
        }

        // Save button
        Button(
            onClick = onSave,
            enabled = hasChanges && !isLoading && !isGenerating,
            modifier = Modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AutoDevColors.Indigo.c600,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save", fontSize = 14.sp)
        }
    }
}
