package cc.unitmesh.devins.ui.compose.document

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.document.DocumentAgent
import cc.unitmesh.devins.document.*
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

/**
 * Document load state machine
 */
sealed class DocumentLoadState {
    object Initial : DocumentLoadState()
    object Loading : DocumentLoadState()
    data class Success(val documents: List<DocumentFile>) : DocumentLoadState()
    object Empty : DocumentLoadState()
    data class Error(val message: String) : DocumentLoadState()
}

class DocumentReaderViewModel(private val workspace: Workspace) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val renderer = ComposeRenderer()

    // LLM and Agent
    private var llmService: KoogLLMService? = null
    private var documentAgent: DocumentAgent? = null
    private var currentExecutionJob: Job? = null

    // State
    var selectedDocument by mutableStateOf<DocumentFile?>(null)
        private set

    var documentContent by mutableStateOf<String?>(null)
        private set

    // Parsed content (for PDF, Word, etc.) - extracted text from Tika
    var parsedContent by mutableStateOf<String?>(null)
        private set

    var selectedDocumentIndexStatus by mutableStateOf<cc.unitmesh.devins.db.DocumentIndexRecord?>(null)
        private set

    // Document load state
    var documentLoadState by mutableStateOf<DocumentLoadState>(DocumentLoadState.Initial)
        private set

    var documents by mutableStateOf<List<DocumentFile>>(emptyList())
        private set

    // Search state
    var searchQuery by mutableStateOf("")
        private set

    var filteredDocuments by mutableStateOf<List<DocumentFile>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isGenerating by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // Navigation state
    var targetLineNumber by mutableStateOf<Int?>(null)
        private set

    var highlightedText by mutableStateOf<String?>(null)
        private set

    // Indexing Service
    private val indexRepository = cc.unitmesh.devins.db.DocumentIndexDatabaseRepository.getInstance()
    private val indexService =
        cc.unitmesh.devins.service.DocumentIndexService(workspace.fileSystem, indexRepository, scope)

    val indexingStatus = indexService.indexingStatus

    init {
        // Initialize platform-specific parsers (Tika on JVM, etc.)
        DocumentRegistry.initializePlatformParsers()

        // Register index provider to bridge DocumentRegistry with DocumentIndexService
        val provider = cc.unitmesh.devins.service.DocumentIndexServiceProvider(indexRepository)
        DocumentRegistry.setIndexProvider(provider)

        loadDocuments()
        initializeLLMService()

        // 自动开始索引文档（延迟一点以确保文档加载完成）
        scope.launch {
            kotlinx.coroutines.delay(500) // 等待 UI 初始化
            if (documents.isNotEmpty()) {
                println("🚀 Auto-indexing ${documents.size} documents...")
                startIndexing()
            }
        }
    }

    /**
     * Create a DocumentFile from a relative path
     * Centralizes the logic for DocumentFile creation
     */
    private fun createDocumentFile(relativePath: String): DocumentFile {
        val name = relativePath.substringAfterLast('/')
        val extension = relativePath.substringAfterLast('.', "").lowercase()

        // Detect format type from file extension
        val formatType = DocumentParserFactory.detectFormat(relativePath)
            ?: DocumentFormatType.PLAIN_TEXT

        // Get MIME type from factory
        val mimeType = DocumentParserFactory.getMimeType(relativePath)

        return DocumentFile(
            name = name,
            path = relativePath,
            metadata = DocumentMetadata(
                totalPages = null,
                chapterCount = 0, // Will be updated when document is opened
                parseStatus = ParseStatus.NOT_PARSED,
                lastModified = Clock.System.now().toEpochMilliseconds(),
                fileSize = 0L, // Load lazily when needed
                language = extension,
                mimeType = mimeType,
                formatType = formatType
            )
        )
    }

    /**
     * Initialize LLM service and DocumentAgent
     */
    private fun initializeLLMService() {
        scope.launch {
            try {
                val configWrapper = ConfigManager.load()
                val activeConfig = configWrapper.getActiveModelConfig()
                if (activeConfig != null && activeConfig.isValid()) {
                    llmService = KoogLLMService.create(activeConfig)

                    // Create DocumentAgent
                    val toolConfigFile = cc.unitmesh.agent.config.ToolConfigFile.default()
                    val mcpConfigService = McpToolConfigService(toolConfigFile)

                    // Use null for fileSystem - DocumentAgent doesn't require filesystem operations
                    // Document queries work via the registry and parser, not filesystem tools
                    val toolFileSystem: cc.unitmesh.agent.tool.filesystem.ToolFileSystem? = null

                    documentAgent = DocumentAgent(
                        llmService = llmService!!,
                        parserService = MarkdownDocumentParser(), // Default parser, actual parsing uses DocumentRegistry
                        renderer = renderer,
                        fileSystem = toolFileSystem,
                        shellExecutor = null,
                        mcpToolConfigService = mcpConfigService,
                        enableLLMStreaming = true
                    )
                } else {
                    println("No valid LLM configuration found")
                }
            } catch (e: Exception) {
                println("Failed to initialize LLM service: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Load documents from workspace or specified path
     */
    private fun loadDocuments() {
        scope.launch {
            try {
                documentLoadState = DocumentLoadState.Loading
                isLoading = true
                error = null

                val fileSystem = workspace.fileSystem
                val rootPath = workspace.rootPath

                if (rootPath == null) {
                    val errorMsg = "No workspace root path configured"
                    error = errorMsg
                    documentLoadState = DocumentLoadState.Error(errorMsg)
                    return@launch
                }

                // Search for all supported document formats
                val pattern = DocumentParserFactory.getSearchPattern()
                val allDocuments = fileSystem.searchFiles(pattern, maxDepth = 20, maxResults = 10000)

                val loadedDocuments = allDocuments.mapNotNull { relativePath ->
                    try {
                        createDocumentFile(relativePath)
                    } catch (e: Exception) {
                        println("Failed to create DocumentFile for $relativePath: ${e.message}")
                        null
                    }
                }

                documents = loadedDocuments
                filteredDocuments = loadedDocuments

                // Update state based on results
                documentLoadState = if (loadedDocuments.isEmpty()) {
                    DocumentLoadState.Empty
                } else {
                    DocumentLoadState.Success(loadedDocuments)
                }

            } catch (e: Exception) {
                val errorMsg = "Failed to load documents: ${e.message}"
                error = errorMsg
                documentLoadState = DocumentLoadState.Error(errorMsg)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Update search query and filter documents
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        filterDocuments()
    }

    /**
     * Filter documents based on search query
     * Supports searching by file name and indexed content
     */
    private fun filterDocuments() {
        filteredDocuments = if (searchQuery.isBlank()) {
            documents
        } else {
            val query = searchQuery.lowercase()
            documents.filter { doc ->
                // Search by file name
                val nameMatch = doc.name.lowercase().contains(query) ||
                    doc.path.lowercase().contains(query)

                // Search by indexed content
                val contentMatch = indexService.getIndexStatus(doc.path)?.let { record ->
                    record.status == "INDEXED" &&
                        record.content?.lowercase()?.contains(query) == true
                } ?: false

                nameMatch || contentMatch
            }
        }
    }

    /**
     * Manually trigger indexing for all loaded documents
     */
    fun startIndexing() {
        if (documents.isEmpty()) {
            println("No documents to index")
            return
        }
        indexService.indexDocuments(documents)
    }

    /**
     * Refresh documents list (without re-indexing)
     */
    fun refreshDocuments() {
        loadDocuments()
    }

    /**
     * Reset indexing status to Idle
     */
    fun resetIndexingStatus() {
        indexService.resetStatus()
    }

    fun selectDocument(doc: DocumentFile) {
        selectedDocument = doc
        selectedDocumentIndexStatus = getDocumentIndexStatus(doc.path)
        scope.launch {
            try {
                isLoading = true
                error = null

                val fileSystem = workspace.fileSystem
                val formatType = DocumentParserFactory.detectFormat(doc.path)

                // Determine if we need to read as binary
                val isBinary = formatType?.let { DocumentParserFactory.isBinaryFormat(it) } ?: false

                // Get the appropriate parser for this document format
                val parser = DocumentParserFactory.createParserForFile(doc.path)
                    ?: run {
                        error = "No parser available for file format: ${doc.path}"
                        println("ERROR: No parser available for: ${doc.path}")
                        parsedContent = null
                        return@launch
                    }

                // Parse the document based on format type
                val parsedDoc = if (isBinary) {
                    // Binary formats (PDF, DOC, DOCX, PPT, PPTX) - read as bytes
                    val bytes = fileSystem.readFileAsBytes(doc.path)
                        ?: run {
                            error = "Failed to read binary file content"
                            documentContent = null
                            parsedContent = null
                            return@launch
                        }

                    documentContent = null // Binary files don't have text content to display

                    // Use parseBytes method for binary files
                    parser.parseBytes(doc, bytes)
                } else {
                    // Text formats (Markdown, TXT) - read as string
                    val content = fileSystem.readFile(doc.path)
                        ?: run {
                            error = "Failed to read file content"
                            documentContent = null
                            parsedContent = null
                            return@launch
                        }

                    // Store original content (for Markdown/Text)
                    documentContent = content
                    parser.parse(doc, content)
                }

                // Get parsed content (for PDF, Word, etc. - Tika extracts text)
                // For Markdown, this will be null (we use original content)
                parsedContent = parser.getDocumentContent()

                // Register document with DocumentRegistry for DocQL queries
                try {
                    DocumentRegistry.registerDocument(doc.path, parsedDoc, parser)
                    println("Document registered with DocumentRegistry: ${doc.path}")
                } catch (e: Exception) {
                    println("Failed to register document: ${e.message}")
                    e.printStackTrace()
                    // Continue even if registration fails
                }

                // Update document with TOC and metadata
                if (parsedDoc is DocumentFile && parsedDoc.toc.isNotEmpty()) {
                    println("ViewModel: Received parsed TOC with ${parsedDoc.toc.size} items")
                    val updatedDoc = doc.copy(
                        toc = parsedDoc.toc,
                        metadata = doc.metadata.copy(
                            chapterCount = parsedDoc.toc.size,
                            parseStatus = ParseStatus.PARSED
                        )
                    )
                    selectedDocument = updatedDoc

                    // Update in documents list
                    documents = documents.map { if (it.path == doc.path) updatedDoc else it }
                }
            } catch (e: Exception) {
                error = "Failed to process document: ${e.message}"
                documentContent = null
                parsedContent = null
                println("ERROR: Failed to process document: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun sendMessage(text: String) {
        if (isGenerating) {
            return
        }

        currentExecutionJob = scope.launch {
            isGenerating = true
            renderer.addUserMessage(text)

            try {
                val agent = documentAgent
                if (agent == null) {
                    renderer.renderError("LLM service not initialized. Please configure your model settings in the Config tab.")
                    isGenerating = false
                    return@launch
                }

                val task = cc.unitmesh.agent.document.DocumentTask(
                    query = text,
                    documentPath = selectedDocument?.path
                )

                agent.execute(task) { _ ->
                }
            } catch (e: CancellationException) {
                renderer.forceStop()
                renderer.renderError("Generation cancelled by user")
            } catch (e: Exception) {
                renderer.renderError("Error: ${e.message}")
                e.printStackTrace()
            } finally {
                isGenerating = false
                currentExecutionJob = null
            }
        }
    }

    fun stopGeneration() {
        currentExecutionJob?.cancel()
        currentExecutionJob = null
        renderer.forceStop()
        isGenerating = false
    }

    fun clearChatHistory() {
        renderer.clearMessages()
        currentExecutionJob?.cancel()
        currentExecutionJob = null
        isGenerating = false
    }


    /**
     * Navigate to a specific line number in the document
     */
    fun navigateToLine(lineNumber: Int, highlightText: String? = null) {
        targetLineNumber = lineNumber
        highlightedText = highlightText
    }

    /**
     * Navigate to a TOC item
     */
    fun navigateToTocItem(tocItem: TOCItem) {
        // Use line number if available
        val lineNum = tocItem.lineNumber
        if (lineNum != null) {
            navigateToLine(lineNum, tocItem.title)
        } else {
            // Fallback: search for the heading text in content
            val content = documentContent ?: return
            val headingPattern = Regex("^#{1,6}\\s+${Regex.escape(tocItem.title)}\\s*$", RegexOption.MULTILINE)
            val match = headingPattern.find(content)
            if (match != null) {
                val lineNumber = content.substring(0, match.range.first).count { it == '\n' } + 1
                navigateToLine(lineNumber, tocItem.title)
            }
        }
    }

    /**
     * Navigate to an entity location
     */
    fun navigateToEntity(entity: Entity) {
        val location = entity.location

        // Use line number if available
        val lineNum = location.line
        if (lineNum != null) {
            navigateToLine(lineNum, entity.name)
        } else {
            // Fallback: search for entity name in content
            val content = documentContent ?: return
            val lines = content.lines()
            val lineIndex = lines.indexOfFirst { it.contains(entity.name) }
            if (lineIndex >= 0) {
                navigateToLine(lineIndex + 1, entity.name)
            }
        }
    }

    /**
     * Clear navigation state
     */
    fun clearNavigation() {
        targetLineNumber = null
        highlightedText = null
    }

    fun getDocumentIndexStatus(path: String): cc.unitmesh.devins.db.DocumentIndexRecord? {
        return indexService.getIndexStatus(path)
    }
}
