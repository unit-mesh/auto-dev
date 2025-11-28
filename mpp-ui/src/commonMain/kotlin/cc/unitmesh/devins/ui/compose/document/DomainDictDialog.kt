package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import cc.unitmesh.agent.subagent.DomainDictAgent
import cc.unitmesh.agent.subagent.DomainDictCallbacks
import cc.unitmesh.agent.subagent.DomainDictContext
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

sealed class DomainDictStatus {
    object Idle : DomainDictStatus()
    object Loading : DomainDictStatus()
    data class Scanning(val message: String = "Scanning...") : DomainDictStatus()
    data class Generating(val progress: String = "", val lineCount: Int = 0) : DomainDictStatus()
    
    /**
     * Deep Research status - shows current step and progress (3 steps)
     */
    data class DeepResearch(
        val currentStep: Int = 0,
        val totalSteps: Int = 3,  // Simplified to 3 steps
        val stepName: String = "",
        val progress: String = "",
        val hotFiles: Int = 0,
        val coChangePatterns: Int = 0,
        val concepts: Int = 0
    ) : DomainDictStatus()
    
    data class Success(val message: String) : DomainDictStatus()
    data class Error(val message: String) : DomainDictStatus()
    data class Cancelled(val message: String = "Cancelled") : DomainDictStatus()
}

/**
 * Progress item for timeline display
 */
data class ProgressItem(
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val message: String,
    val type: ProgressType = ProgressType.INFO
)

enum class ProgressType {
    STEP_HEADER,    // ## Step X/7: Name
    SUB_HEADER,     // ### Dimension research
    SUCCESS,        // ✓ or ✅ messages
    ACTION,         // ➕ Added entries
    WARNING,        // ⚠️ messages
    ERROR,          // ❌ messages
    INFO            // Regular info
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
    
    // AI thinking content (streaming)
    var aiThinkingContent by remember { mutableStateOf("") }
    var isAiThinking by remember { mutableStateOf(false) }
    
    // Deep Research progress timeline
    val progressTimeline = remember { mutableStateListOf<ProgressItem>() }
    
    // New entries added during generation (for real-time preview)
    val newEntriesPreview = remember { mutableStateListOf<DictEntry>() }
    
    // Job for cancellation support
    var currentJob by remember { mutableStateOf<Job?>(null) }
    var currentAgent by remember { mutableStateOf<DomainDictAgent?>(null) }

    val scope = rememberCoroutineScope()
    val domainDictService = remember { DomainDictService(workspace.fileSystem) }
    val listState = rememberLazyListState()
    val progressListState = rememberLazyListState()

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

    /**
     * Parse progress message and determine its type
     */
    fun parseProgressType(message: String): ProgressType {
        return when {
            message.startsWith("## Step") -> ProgressType.STEP_HEADER
            message.startsWith("###") -> ProgressType.SUB_HEADER
            message.contains("✓") || message.contains("✅") -> ProgressType.SUCCESS
            message.contains("➕") -> ProgressType.ACTION
            message.contains("⚠️") -> ProgressType.WARNING
            message.contains("❌") -> ProgressType.ERROR
            else -> ProgressType.INFO
        }
    }

    /**
     * Extract step info from progress message (7-step format, legacy)
     */
    fun extractStepInfo(message: String): Pair<Int, String>? {
        val stepMatch = Regex("## Step (\\d+)/7: (.+)").find(message)
        return stepMatch?.let {
            val step = it.groupValues[1].toIntOrNull() ?: 0
            val name = it.groupValues[2]
            step to name
        }
    }
    
    /**
     * Extract step info from progress message (3-step format)
     */
    fun extractStepInfo3(message: String): Pair<Int, String>? {
        val stepMatch = Regex("## Step (\\d+)/3: (.+)").find(message)
        return stepMatch?.let {
            val step = it.groupValues[1].toIntOrNull() ?: 0
            val name = it.groupValues[2]
            step to name
        }
    }

    /**
     * Cancel the current research
     */
    fun cancelResearch() {
        currentAgent?.cancel()
        currentJob?.cancel()
        status = DomainDictStatus.Cancelled("Research cancelled by user")
        isAiThinking = false
        currentJob = null
        currentAgent = null
    }

    /**
     * Deep Research generation using DomainDictAgent
     */
    fun generateDeepResearch() {
        currentJob = scope.launch {
            progressTimeline.clear()
            newEntriesPreview.clear()
            streamingContent = ""
            aiThinkingContent = ""
            isAiThinking = false
            status = DomainDictStatus.DeepResearch(0, 3, "Initializing", "Starting...")
            
            try {
                val config = ConfigManager.load().getActiveModelConfig()
                if (config == null || !config.isValid()) {
                    status = DomainDictStatus.Error("No valid LLM config")
                    return@launch
                }
                
                val projectPath = workspace.rootPath
                if (projectPath == null) {
                    status = DomainDictStatus.Error("No project path set")
                    return@launch
                }
                
                // Create LLM service and agent
                val llmService = KoogLLMService.create(config)
                val agent = DomainDictAgent(
                    llmService = llmService,
                    fileSystem = workspace.fileSystem,
                    domainDictService = domainDictService,
                    maxDefaultIterations = 7,
                    enableStreaming = true
                )
                currentAgent = agent
                
                progressTimeline.add(ProgressItem(
                    message = "🔬 Domain Dictionary Deep Research Agent Started",
                    type = ProgressType.INFO
                ))
                
                // Create callbacks for full progress reporting
                val callbacks = DomainDictCallbacks(
                    onProgress = { message ->
                        // Clear AI thinking when new progress message arrives
                        if (!message.contains("💭") && !message.contains("thinking")) {
                            isAiThinking = false
                            aiThinkingContent = ""
                        } else {
                            isAiThinking = true
                        }
                        
                        // Update progress timeline
                        val type = parseProgressType(message)
                        progressTimeline.add(ProgressItem(message = message, type = type))
                        
                        // Update status based on step headers (now 3 steps)
                        extractStepInfo3(message)?.let { (step, name) ->
                            val currentStatus = status
                            if (currentStatus is DomainDictStatus.DeepResearch) {
                                status = currentStatus.copy(currentStep = step, stepName = name, progress = message)
                            } else {
                                status = DomainDictStatus.DeepResearch(step, 3, name, message)
                            }
                        }
                        
                        // Keep streaming content updated for fallback display
                        streamingContent = progressTimeline.joinToString("\n") { it.message }
                        
                        // Auto-scroll to latest
                        scope.launch {
                            if (progressTimeline.isNotEmpty()) {
                                progressListState.animateScrollToItem(progressTimeline.size - 1)
                            }
                        }
                    },
                    onAIThinking = { chunk ->
                        // Append AI thinking chunk
                        isAiThinking = true
                        aiThinkingContent += chunk
                    },
                    onStepComplete = { step, stepName, summary ->
                        // Update status when step completes
                        val currentStatus = status
                        if (currentStatus is DomainDictStatus.DeepResearch) {
                            status = currentStatus.copy(currentStep = step, stepName = stepName)
                        }
                        progressTimeline.add(ProgressItem(
                            message = "   ✅ Step $step ($stepName): $summary",
                            type = ProgressType.SUCCESS
                        ))
                    },
                    onCodebaseStats = { hotFiles, coChangePatterns, concepts ->
                        // Update status with codebase stats
                        val currentStatus = status
                        if (currentStatus is DomainDictStatus.DeepResearch) {
                            status = currentStatus.copy(
                                hotFiles = hotFiles,
                                coChangePatterns = coChangePatterns,
                                concepts = concepts
                            )
                        }
                    },
                    onEntryAdded = { entry ->
                        // Add to preview list for real-time display
                        newEntriesPreview.add(DictEntry(entry.chinese, entry.codeTranslation, entry.description))
                    }
                )
                
                // Execute with full callbacks
                val result = agent.executeWithCallbacks(
                    input = DomainDictContext(
                        userQuery = "Optimize domain dictionary based on current codebase",
                        maxIterations = 7
                    ),
                    callbacks = callbacks
                )
                
                isAiThinking = false
                currentAgent = null
                currentJob = null
                
                if (result.success) {
                    // Reload the dictionary
                    val content = withContext(Dispatchers.Default) { domainDictService.loadContent() }
                    entries = DictEntry.parseCSV(content ?: "")
                    hasChanges = false
                    
                    val newEntries = result.metadata["newEntries"]?.toIntOrNull() ?: 0
                    status = DomainDictStatus.Success("Deep Research complete! Added $newEntries new entries")
                } else {
                    val reason = result.metadata["reason"]
                    if (reason == "user_cancelled") {
                        status = DomainDictStatus.Cancelled("Research cancelled")
                    } else {
                        status = DomainDictStatus.Error("Deep Research failed: ${result.content.take(100)}")
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                status = DomainDictStatus.Cancelled("Research cancelled")
                isAiThinking = false
            } catch (e: Exception) {
                status = DomainDictStatus.Error("Failed: ${e.message}")
                progressTimeline.add(ProgressItem(
                    message = "❌ Error: ${e.message}",
                    type = ProgressType.ERROR
                ))
                isAiThinking = false
            }
        }
    }

    val isLoading = status is DomainDictStatus.Loading
    val isGenerating = status is DomainDictStatus.Generating || 
                       status is DomainDictStatus.Scanning ||
                       status is DomainDictStatus.DeepResearch
    val isCancellable = status is DomainDictStatus.DeepResearch

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(if (isGenerating) 900.dp else 620.dp).heightIn(max = 650.dp),
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
                        if (newEntriesPreview.isNotEmpty()) {
                            Text("+${newEntriesPreview.size}", fontSize = 11.sp, color = AutoDevColors.Green.c600, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = { if (!isGenerating) onDismiss() }, Modifier.size(28.dp)) {
                        Icon(AutoDevComposeIcons.Close, "Close", Modifier.size(18.dp))
                    }
                }

                // Status bar (only when needed)
                when (val s = status) {
                    is DomainDictStatus.DeepResearch -> {
                        Column(
                            Modifier.fillMaxWidth().background(AutoDevColors.Indigo.c100.copy(0.5f)).padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AutoDevColors.Indigo.c500)
                                    Text(
                                        "🔬 Step ${s.currentStep}/3: ${s.stepName}",
                                        fontSize = 12.sp, color = AutoDevColors.Indigo.c700, fontWeight = FontWeight.Medium
                                    )
                                }
                                // Cancel button
                                TextButton(
                                    onClick = { cancelResearch() },
                                    modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Cancel", fontSize = 11.sp, color = AutoDevColors.Red.c500)
                                }
                            }
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { s.currentStep.toFloat() / s.totalSteps.toFloat() },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = AutoDevColors.Indigo.c500,
                                trackColor = AutoDevColors.Indigo.c200
                            )
                            // Codebase stats (if available)
                            if (s.hotFiles > 0 || s.concepts > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("📊 ${s.hotFiles} hot files", fontSize = 10.sp, color = AutoDevColors.Indigo.c600)
                                    Text("🔗 ${s.coChangePatterns} patterns", fontSize = 10.sp, color = AutoDevColors.Indigo.c600)
                                    Text("💡 ${s.concepts} concepts", fontSize = 10.sp, color = AutoDevColors.Indigo.c600)
                                    if (newEntriesPreview.isNotEmpty()) {
                                        Text("✅ +${newEntriesPreview.size} new", fontSize = 10.sp, color = AutoDevColors.Green.c600)
                                    }
                                }
                            }
                        }
                    }
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
                    is DomainDictStatus.Cancelled -> {
                        Row(Modifier.fillMaxWidth().background(AutoDevColors.Amber.c100.copy(0.5f)).padding(8.dp)) {
                            Icon(AutoDevComposeIcons.Warning, null, Modifier.size(14.dp), AutoDevColors.Amber.c600)
                            Spacer(Modifier.width(6.dp))
                            Text(s.message, fontSize = 12.sp, color = AutoDevColors.Amber.c700)
                        }
                    }
                    else -> {}
                }

                // Content
                Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                    when {
                        // Deep Research progress timeline view with AI thinking + CSV preview
                        status is DomainDictStatus.DeepResearch && progressTimeline.isNotEmpty() -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Left: Progress timeline
                                Column(Modifier.weight(1f)) {
                                    Box(Modifier.weight(1f)) {
                                        DeepResearchProgressView(progressTimeline, progressListState)
                                    }
                                    // AI Thinking block
                                    if (isAiThinking && aiThinkingContent.isNotEmpty()) {
                                        AIThinkingBlock(
                                            content = aiThinkingContent,
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        )
                                    }
                                }
                                // Right: Real-time CSV preview
                                NewEntriesPreview(
                                    entries = entries,
                                    newEntries = newEntriesPreview,
                                    modifier = Modifier.width(280.dp)
                                )
                            }
                        }
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
                        onClick = { generateDeepResearch() },
                        enabled = !isLoading && !isGenerating,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AutoDevColors.Indigo.c600),
                        border = BorderStroke(1.dp, AutoDevColors.Indigo.c300)
                    ) {
                        Icon(AutoDevComposeIcons.Custom.AI, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isGenerating) "Researching..." else "Deep Research", fontSize = 12.sp)
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

/**
 * Real-time CSV preview showing existing + new entries
 */
@Composable
private fun NewEntriesPreview(
    entries: List<DictEntry>,
    newEntries: List<DictEntry>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    
    // Auto-scroll to new entries
    LaunchedEffect(newEntries.size) {
        if (newEntries.isNotEmpty()) {
            // Scroll to first new entry
            val firstNewIndex = entries.size
            if (firstNewIndex > 0) {
                scrollState.animateScrollToItem(maxOf(0, firstNewIndex - 2))
            }
        }
    }
    
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, AutoDevColors.Indigo.c200),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth()
                    .background(AutoDevColors.Indigo.c50)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📋 domain.csv", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${entries.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (newEntries.isNotEmpty()) {
                        Text("+${newEntries.size}", fontSize = 10.sp, color = AutoDevColors.Green.c600, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            HorizontalDivider(thickness = 1.dp, color = AutoDevColors.Indigo.c100)
            
            // CSV content
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Existing entries (dimmed)
                items(entries.size) { index ->
                    val entry = entries[index]
                    CSVEntryRow(entry, isNew = false)
                }
                
                // New entries (highlighted)
                items(newEntries.size) { index ->
                    val entry = newEntries[index]
                    CSVEntryRow(entry, isNew = true)
                }
                
                // Empty state
                if (entries.isEmpty() && newEntries.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            Text("No entries yet", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CSVEntryRow(entry: DictEntry, isNew: Boolean) {
    val bgColor = if (isNew) AutoDevColors.Green.c50.copy(0.6f) else Color.Transparent
    val textColor = if (isNew) AutoDevColors.Green.c800 else MaterialTheme.colorScheme.onSurface.copy(0.7f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isNew) {
            Text("✨", fontSize = 9.sp)
        }
        Text(
            text = entry.chinese,
            fontSize = 10.sp,
            color = textColor,
            fontWeight = if (isNew) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp)
        )
        Text("→", fontSize = 9.sp, color = textColor.copy(0.5f))
        Text(
            text = entry.english.take(30),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = if (isNew) AutoDevColors.Cyan.c700 else AutoDevColors.Cyan.c600.copy(0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * AI Thinking Block - shows streaming AI output in a collapsible block
 * Similar to ThinkingBlockRenderer but simplified for dialog use
 */
@Composable
private fun AIThinkingBlock(
    content: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    
    // Auto-scroll to bottom during streaming
    LaunchedEffect(content) {
        if (isExpanded && content.isNotBlank()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
                
                Text(
                    text = "💭 AI thinking...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                
                // Animated indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.dp,
                    color = AutoDevColors.Indigo.c400
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // Scrollable container with max height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 80.dp)
                        .verticalScroll(scrollState)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(3.dp)
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text = content.takeLast(1000), // Limit to last 1000 chars for performance
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }
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

/**
 * Deep Research progress timeline view
 * Shows structured progress messages with visual indicators
 */
@Composable
private fun DeepResearchProgressView(
    progressTimeline: List<ProgressItem>,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Surface(
        Modifier.fillMaxSize(),
        RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, AutoDevColors.Indigo.c200)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(progressTimeline.size, key = { it }) { index ->
                val item = progressTimeline[index]
                ProgressItemRow(item)
            }
        }
    }
}

@Composable
private fun ProgressItemRow(item: ProgressItem) {
    val (backgroundColor, textColor, fontWeight, leftPadding) = when (item.type) {
        ProgressType.STEP_HEADER -> Quadruple(
            AutoDevColors.Indigo.c100.copy(0.6f),
            AutoDevColors.Indigo.c800,
            FontWeight.Bold,
            0.dp
        )
        ProgressType.SUB_HEADER -> Quadruple(
            AutoDevColors.Cyan.c50.copy(0.4f),
            AutoDevColors.Cyan.c800,
            FontWeight.SemiBold,
            8.dp
        )
        ProgressType.SUCCESS -> Quadruple(
            AutoDevColors.Green.c50.copy(0.3f),
            AutoDevColors.Green.c700,
            FontWeight.Normal,
            16.dp
        )
        ProgressType.ACTION -> Quadruple(
            AutoDevColors.Cyan.c50.copy(0.3f),
            AutoDevColors.Cyan.c700,
            FontWeight.Normal,
            16.dp
        )
        ProgressType.WARNING -> Quadruple(
            AutoDevColors.Amber.c50.copy(0.4f),
            AutoDevColors.Amber.c800,
            FontWeight.Normal,
            16.dp
        )
        ProgressType.ERROR -> Quadruple(
            AutoDevColors.Red.c50.copy(0.4f),
            AutoDevColors.Red.c700,
            FontWeight.Normal,
            8.dp
        )
        ProgressType.INFO -> Quadruple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface.copy(0.8f),
            FontWeight.Normal,
            16.dp
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(start = leftPadding, top = 2.dp, bottom = 2.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.message,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = fontWeight,
            color = textColor,
            lineHeight = 14.sp
        )
    }
}

/**
 * Helper data class for styling quadruple
 */
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
