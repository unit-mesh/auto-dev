package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

sealed class DomainDictStatus {
    object Idle : DomainDictStatus()
    object Loading : DomainDictStatus()
    data class Scanning(val message: String = "Scanning...") : DomainDictStatus()
    data class Generating(val progress: String = "", val lineCount: Int = 0) : DomainDictStatus()
    data class Success(val message: String) : DomainDictStatus()
    data class Error(val message: String) : DomainDictStatus()
}

data class DictEntry(
    val chinese: String,
    val english: String,
    val description: String
) {
    fun getCodeTranslations(): List<String> =
        english.split("|").map { it.trim() }.filter { it.isNotEmpty() }

    fun toCsvLine(): String = "$chinese,$english,$description"

    companion object {
        fun fromCsvLine(line: String): DictEntry? {
            val parts = line.split(",", limit = 3)
            return if (parts.size >= 2) {
                DictEntry(parts[0].trim(), parts[1].trim(), parts.getOrElse(2) { "" }.trim())
            } else null
        }

        fun parseCSV(content: String): List<DictEntry> =
            content.lines().drop(1).filter { it.isNotBlank() }.mapNotNull { fromCsvLine(it) }

        fun toCSV(entries: List<DictEntry>): String {
            val header = "Chinese,Code Translation,Description"
            return (listOf(header) + entries.map { it.toCsvLine() }).joinToString("\n")
        }
    }
}

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
    val domainDictService = remember { DomainDictService(workspace.fileSystem) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        status = DomainDictStatus.Loading
        try {
            val content = withContext(Dispatchers.Default) { domainDictService.loadContent() }
            entries = DictEntry.parseCSV(content ?: "")
            status = DomainDictStatus.Idle
        } catch (e: Exception) {
            status = DomainDictStatus.Error("Load failed: ${e.message}")
        }
    }

    fun saveDict() {
        scope.launch {
            status = DomainDictStatus.Loading
            try {
                val saved = withContext(Dispatchers.Default) {
                    domainDictService.saveContent(DictEntry.toCSV(entries))
                }
                status = if (saved) {
                    hasChanges = false
                    DomainDictStatus.Success("Saved ${entries.size} entries")
                } else DomainDictStatus.Error("Save failed")
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Save failed: ${e.message}")
            }
        }
    }

    fun generateDict() {
        scope.launch {
            status = DomainDictStatus.Scanning()
            streamingContent = ""
            try {
                val config = ConfigManager.load().getActiveModelConfig()
                if (config == null || !config.isValid()) {
                    status = DomainDictStatus.Error("No valid LLM config")
                    return@launch
                }
                val generator = DomainDictGenerator(workspace.fileSystem, config)
                var lineCount = 0
                val result = StringBuilder()

                generator.generateStreaming().collect { chunk ->
                    result.append(chunk)
                    streamingContent = result.toString()
                    val newCount = streamingContent.count { it == '\n' }
                    if (newCount > lineCount) {
                        lineCount = newCount
                        status = DomainDictStatus.Generating("", lineCount)
                    } else if (status is DomainDictStatus.Scanning) {
                        status = DomainDictStatus.Generating("", 0)
                    }
                }

                val saved = withContext(Dispatchers.Default) {
                    domainDictService.saveContent(result.toString())
                }
                if (saved) {
                    entries = DictEntry.parseCSV(result.toString())
                    hasChanges = false
                    streamingContent = ""
                    status = DomainDictStatus.Success("Generated ${entries.size} entries")
                } else {
                    status = DomainDictStatus.Error("Save failed")
                }
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Failed: ${e.message}")
            }
        }
    }

    val isLoading = status is DomainDictStatus.Loading
    val isGenerating = status is DomainDictStatus.Generating || status is DomainDictStatus.Scanning

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(620.dp).heightIn(max = 600.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Compact header
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(AutoDevComposeIcons.MenuBook, null, Modifier.size(20.dp), AutoDevColors.Indigo.c500)
                        Text("Domain Dictionary", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (hasChanges) Text("*", color = AutoDevColors.Amber.c500, fontWeight = FontWeight.Bold)
                        Text("(${entries.size})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { if (!isGenerating) onDismiss() }, Modifier.size(28.dp)) {
                        Icon(AutoDevComposeIcons.Close, "Close", Modifier.size(18.dp))
                    }
                }

                // Status bar (only when needed)
                when (val s = status) {
                    is DomainDictStatus.Generating, is DomainDictStatus.Scanning -> {
                        Row(
                            Modifier.fillMaxWidth().background(AutoDevColors.Indigo.c100.copy(0.5f)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AutoDevColors.Indigo.c500)
                            Text(
                                if (s is DomainDictStatus.Generating) "Generating... (${s.lineCount})" else "Scanning...",
                                fontSize = 12.sp, color = AutoDevColors.Indigo.c700
                            )
                        }
                    }
                    is DomainDictStatus.Success -> {
                        Row(Modifier.fillMaxWidth().background(AutoDevColors.Green.c100.copy(0.5f)).padding(8.dp)) {
                            Icon(AutoDevComposeIcons.CheckCircle, null, Modifier.size(14.dp), AutoDevColors.Green.c600)
                            Spacer(Modifier.width(6.dp))
                            Text(s.message, fontSize = 12.sp, color = AutoDevColors.Green.c700)
                        }
                    }
                    is DomainDictStatus.Error -> {
                        Row(Modifier.fillMaxWidth().background(AutoDevColors.Red.c100.copy(0.5f)).padding(8.dp)) {
                            Icon(AutoDevComposeIcons.Error, null, Modifier.size(14.dp), AutoDevColors.Red.c600)
                            Spacer(Modifier.width(6.dp))
                            Text(s.message, fontSize = 12.sp, color = AutoDevColors.Red.c700)
                        }
                    }
                    else -> {}
                }

                // Content
                Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                    when {
                        isGenerating && streamingContent.isNotEmpty() -> StreamingView(streamingContent)
                        isLoading -> LoadingView()
                        else -> TableView(entries, listState, { i, e ->
                            entries = entries.toMutableList().apply { set(i, e) }
                            hasChanges = true
                        }, { i ->
                            entries = entries.toMutableList().apply { removeAt(i) }
                            hasChanges = true
                        }, !isLoading && !isGenerating)
                    }
                }

                // Actions
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            entries = entries + DictEntry("", "", "")
                            hasChanges = true
                            scope.launch { listState.animateScrollToItem(entries.size - 1) }
                        },
                        enabled = !isLoading && !isGenerating,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(AutoDevComposeIcons.Add, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { generateDict() },
                        enabled = !isLoading && !isGenerating,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AutoDevColors.Indigo.c600),
                        border = BorderStroke(1.dp, AutoDevColors.Indigo.c300)
                    ) {
                        Icon(AutoDevComposeIcons.Custom.AI, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isGenerating) "..." else "Generate", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { saveDict() },
                        enabled = hasChanges && !isLoading && !isGenerating,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.buttonColors(AutoDevColors.Indigo.c600, Color.White)
                    ) {
                        Icon(AutoDevComposeIcons.Save, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), color = AutoDevColors.Indigo.c500)
    }
}

@Composable
private fun StreamingView(content: String) {
    val lines = remember(content) { content.lines() }
    val scrollState = rememberLazyListState()
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) scrollState.animateScrollToItem(lines.size - 1) }

    Surface(Modifier.fillMaxSize(), RoundedCornerShape(6.dp), border = BorderStroke(1.dp, AutoDevColors.Indigo.c200)) {
        LazyColumn(state = scrollState, modifier = Modifier.padding(8.dp)) {
            items(lines.size) { i ->
                Text(lines[i], fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun TableView(
    entries: List<DictEntry>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onUpdate: (Int, DictEntry) -> Unit,
    onDelete: (Int) -> Unit,
    enabled: Boolean
) {
    Surface(Modifier.fillMaxSize(), RoundedCornerShape(6.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            // Header
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Term", Modifier.width(80.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Code Translations", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Description", Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(24.dp))
            }
            HorizontalDivider(thickness = 1.dp)

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No entries. Add or generate.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(entries, key = { i, _ -> i }) { i, entry ->
                        TableRow(entry, i, { onUpdate(i, it) }, { onDelete(i) }, enabled)
                        if (i < entries.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    entry: DictEntry,
    index: Int,
    onUpdate: (DictEntry) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean
) {
    var chinese by remember(entry) { mutableStateOf(entry.chinese) }
    var english by remember(entry) { mutableStateOf(entry.english) }
    var description by remember(entry) { mutableStateOf(entry.description) }
    var editingCode by remember { mutableStateOf(false) }

    val codes = remember(english) { english.split("|").map { it.trim() }.filter { it.isNotEmpty() } }

    Row(
        Modifier.fillMaxWidth()
            .background(if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.3f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chinese term
        MiniField(chinese, { chinese = it; onUpdate(DictEntry(it, english, description)) }, "Term", enabled, Modifier.width(80.dp))

        // Code translations - chips or edit
        Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            if (editingCode) {
                MiniField(
                    english,
                    { english = it; onUpdate(DictEntry(chinese, it, description)) },
                    "Class | Method",
                    enabled,
                    Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) editingCode = false },
                    FontFamily.Monospace
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .clickable(enabled) { editingCode = true }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (codes.isEmpty()) {
                        Text("click to add...", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    } else {
                        codes.forEach { code ->
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = AutoDevColors.Cyan.c100.copy(0.6f),
                                border = BorderStroke(0.5.dp, AutoDevColors.Cyan.c300.copy(0.4f))
                            ) {
                                Text(
                                    code,
                                    Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AutoDevColors.Cyan.c800,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Description
        MiniField(description, { description = it; onUpdate(DictEntry(chinese, english, it)) }, "Desc", enabled, Modifier.weight(0.6f))

        // Delete
        IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.size(20.dp)) {
            Icon(AutoDevComposeIcons.Delete, "Delete", Modifier.size(12.dp), MaterialTheme.colorScheme.error.copy(0.5f))
        }
    }
}

@Composable
private fun MiniField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (enabled) MaterialTheme.colorScheme.surfaceContainerHighest.copy(0.3f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        textStyle = TextStyle(fontSize = 11.sp, fontFamily = fontFamily, color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(AutoDevColors.Indigo.c500),
        enabled = enabled,
        singleLine = true,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                inner()
            }
        }
    )
}
