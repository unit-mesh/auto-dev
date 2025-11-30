package cc.unitmesh.devins.idea.toolwindow.knowledge

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.document.DocumentAgent
import cc.unitmesh.agent.document.DocumentTask
import cc.unitmesh.devins.document.*
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ViewModel for Knowledge Agent in IntelliJ IDEA plugin.
 * Adapted from mpp-ui's DocumentReaderViewModel for IntelliJ platform.
 *
 * Uses mpp-core's DocumentAgent for document queries and
 * JewelRenderer for UI rendering.
 */
class IdeaKnowledgeViewModel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) : Disposable {

    private val projectPath: String = project.basePath ?: ""

    // Renderer for agent output
    val renderer = JewelRenderer()

    // State
    private val _state = MutableStateFlow(IdeaKnowledgeState())
    val state: StateFlow<IdeaKnowledgeState> = _state.asStateFlow()

    // Control execution
    private var initJob: Job? = null
    private var currentJob: Job? = null
    private var documentAgent: DocumentAgent? = null
    private var llmService: KoogLLMService? = null
    private var agentInitialized = false

    init {
        // Initialize platform-specific parsers (Tika on JVM)
        DocumentRegistry.initializePlatformParsers()

        if (projectPath.isEmpty()) {
            updateState { it.copy(error = "No project path available") }
        } else {
            // Launch initialization on IO dispatcher to avoid EDT violations
            initJob = coroutineScope.launch(Dispatchers.IO) {
                try {
                    initializeLLMService()
                    loadDocuments()
                } catch (e: CancellationException) {
                    // Intentional cancellation - no error message needed
                } catch (e: Exception) {
                    updateState { it.copy(error = "Failed to initialize: ${e.message}") }
                }
            }
        }
    }

    /**
     * Initialize LLM service and DocumentAgent
     */
    private suspend fun initializeLLMService() {
        try {
            val configWrapper = ConfigManager.load()
            val activeConfig = configWrapper.getActiveModelConfig()
            if (activeConfig != null && activeConfig.isValid()) {
                llmService = KoogLLMService.create(activeConfig)

                // Create DocumentAgent
                val toolConfigFile = ToolConfigFile.default()
                val mcpConfigService = McpToolConfigService(toolConfigFile)

                documentAgent = DocumentAgent(
                    llmService = llmService!!,
                    parserService = MarkdownDocumentParser(),
                    renderer = renderer,
                    fileSystem = null,
                    shellExecutor = null,
                    mcpToolConfigService = mcpConfigService,
                    enableLLMStreaming = true
                )
                agentInitialized = true
            }
        } catch (e: Exception) {
            updateState { it.copy(error = "Failed to initialize LLM service: ${e.message}") }
        }
    }

    /**
     * Load documents from project
     */
    private suspend fun loadDocuments() {
        updateState { it.copy(isLoading = true, error = null) }

        try {
            val documents = scanProjectDocuments()
            updateState {
                it.copy(
                    isLoading = false,
                    documents = documents,
                    filteredDocuments = documents,
                    error = null
                )
            }
        } catch (e: Exception) {
            updateState {
                it.copy(isLoading = false, error = "Failed to load documents: ${e.message}")
            }
        }
    }

    /**
     * Scan project for supported documents
     */
    private fun scanProjectDocuments(): List<IdeaDocumentFile> {
        val projectDir = File(projectPath)
        if (!projectDir.exists()) return emptyList()

        val supportedExtensions = setOf(
            "md", "markdown", "txt", "pdf", "doc", "docx",
            "kt", "java", "py", "js", "ts", "html", "xml"
        )

        return projectDir.walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.extension.lowercase() in supportedExtensions &&
                    !file.path.contains(".git") &&
                    !file.path.contains("node_modules") &&
                    !file.path.contains("build") &&
                    !file.path.contains("target")
            }
            .take(1000) // Limit to first 1000 files
            .map { file ->
                val relativePath = file.relativeTo(projectDir).path
                val formatType = DocumentParserFactory.detectFormat(file.name)
                    ?: DocumentFormatType.PLAIN_TEXT

                IdeaDocumentFile(
                    name = file.name,
                    path = relativePath,
                    metadata = IdeaDocumentMetadata(
                        lastModified = file.lastModified(),
                        fileSize = file.length(),
                        language = file.extension,
                        mimeType = DocumentParserFactory.getMimeType(file.name),
                        formatType = formatType.name
                    )
                )
            }
            .toList()
    }

    /**
     * Update search query and filter documents
     */
    fun updateSearchQuery(query: String) {
        val currentState = _state.value
        val filtered = if (query.isBlank()) {
            currentState.documents
        } else {
            val lowerQuery = query.lowercase()
            currentState.documents.filter { doc ->
                doc.name.lowercase().contains(lowerQuery) ||
                    doc.path.lowercase().contains(lowerQuery)
            }
        }

        updateState {
            it.copy(
                searchQuery = query,
                filteredDocuments = filtered
            )
        }
    }

    /**
     * Select a document
     */
    fun selectDocument(document: IdeaDocumentFile) {
        // Launch on IO dispatcher to avoid EDT violations
        coroutineScope.launch(Dispatchers.IO) {
            loadDocumentContent(document)
        }
    }

    /**
     * Load document content - runs on IO dispatcher
     */
    private suspend fun loadDocumentContent(document: IdeaDocumentFile) {
        updateState { it.copy(isLoading = true, error = null) }

        try {
            val file = withContext(Dispatchers.IO) { File(projectPath, document.path) }
            if (!file.exists()) {
                updateState {
                    it.copy(isLoading = false, error = "File not found: ${document.path}")
                }
                return
            }

            val formatType = DocumentParserFactory.detectFormat(document.path)
            val isBinary = formatType?.let { DocumentParserFactory.isBinaryFormat(it) } ?: false

            val parser = DocumentParserFactory.createParserForFile(document.path)
            if (parser == null) {
                updateState {
                    it.copy(isLoading = false, error = "No parser available for: ${document.path}")
                }
                return
            }

            val docFile = DocumentFile(
                name = document.name,
                path = document.path,
                metadata = DocumentMetadata(
                    lastModified = file.lastModified(),
                    fileSize = file.length(),
                    formatType = formatType ?: DocumentFormatType.PLAIN_TEXT
                )
            )

            // Read and parse file content on IO dispatcher
            val (content, parsedDoc) = withContext(Dispatchers.IO) {
                if (isBinary) {
                    val bytes = file.readBytes()
                    val parsed = parser.parseBytes(docFile, bytes)
                    null to parsed
                } else {
                    val textContent = file.readText()
                    val parsed = parser.parse(docFile, textContent)
                    textContent to parsed
                }
            }

            // Register document with DocumentRegistry for DocQL queries
            DocumentRegistry.registerDocument(document.path, parsedDoc, parser)

            val parsedContent = parser.getDocumentContent()

            // Update state with loaded content
            val updatedDoc = if (parsedDoc is DocumentFile) {
                document.copy(
                    toc = parsedDoc.toc,
                    entities = parsedDoc.entities
                )
            } else {
                document
            }

            updateState {
                it.copy(
                    isLoading = false,
                    selectedDocument = updatedDoc,
                    documentContent = content,
                    parsedContent = parsedContent,
                    error = null
                )
            }
        } catch (e: Exception) {
            updateState {
                it.copy(
                    isLoading = false,
                    documentContent = null,
                    parsedContent = null,
                    error = "Failed to load document: ${e.message}"
                )
            }
        }
    }

    /**
     * Send a message to the DocumentAgent
     */
    fun sendMessage(text: String) {
        if (_state.value.isGenerating) return

        currentJob?.cancel()
        currentJob = coroutineScope.launch {
            try {
                updateState { it.copy(isGenerating = true, error = null) }
                renderer.addUserMessage(text)

                val agent = documentAgent
                if (agent == null) {
                    renderer.renderError("LLM service not initialized. Please configure your model settings.")
                    updateState { it.copy(isGenerating = false) }
                    return@launch
                }

                val task = DocumentTask(
                    query = text,
                    documentPath = _state.value.selectedDocument?.path
                )

                agent.execute(task) { _ -> }
            } catch (e: CancellationException) {
                renderer.forceStop()
                // Intentional cancellation - no error message needed
            } catch (e: Exception) {
                renderer.renderError("Error: ${e.message}")
            } finally {
                updateState { it.copy(isGenerating = false) }
                currentJob = null
            }
        }
    }

    /**
     * Stop current generation
     */
    fun stopGeneration() {
        currentJob?.cancel()
        currentJob = null
        renderer.forceStop()
        updateState { it.copy(isGenerating = false) }
    }

    /**
     * Clear chat history
     */
    fun clearChatHistory() {
        renderer.clearTimeline()
        currentJob?.cancel()
        currentJob = null
        updateState { it.copy(isGenerating = false) }
    }

    /**
     * Navigate to a specific line number
     */
    fun navigateToLine(lineNumber: Int, highlightText: String? = null) {
        updateState {
            it.copy(
                targetLineNumber = lineNumber,
                highlightedText = highlightText
            )
        }
    }

    /**
     * Navigate to a TOC item
     */
    fun navigateToTocItem(tocItem: TOCItem) {
        val lineNum = tocItem.lineNumber
        if (lineNum != null) {
            navigateToLine(lineNum, tocItem.title)
        } else {
            // Fallback: search for the heading text in content
            val content = _state.value.documentContent ?: return
            val headingPattern = Regex("^#{1,6}\\s+${Regex.escape(tocItem.title)}\\s*$", RegexOption.MULTILINE)
            val match = headingPattern.find(content)
            if (match != null) {
                val lineNumber = content.substring(0, match.range.first).count { it == '\n' } + 1
                navigateToLine(lineNumber, tocItem.title)
            }
        }
    }

    /**
     * Clear navigation state
     */
    fun clearNavigation() {
        updateState {
            it.copy(
                targetLineNumber = null,
                highlightedText = null
            )
        }
    }

    /**
     * Refresh documents list
     */
    fun refreshDocuments() {
        // Launch on IO dispatcher to avoid EDT violations
        coroutineScope.launch(Dispatchers.IO) {
            loadDocuments()
        }
    }

    private fun updateState(update: (IdeaKnowledgeState) -> IdeaKnowledgeState) {
        _state.value = update(_state.value)
    }

    override fun dispose() {
        initJob?.cancel()
        currentJob?.cancel()
    }
}

