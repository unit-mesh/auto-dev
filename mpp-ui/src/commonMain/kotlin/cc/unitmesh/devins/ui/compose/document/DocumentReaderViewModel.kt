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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class DocumentReaderViewModel(private val workspace: Workspace) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val parserService: DocumentParserService = MarkdownDocumentParser()
    val renderer = ComposeRenderer()

    // LLM and Agent
    private var llmService: KoogLLMService? = null
    private var documentAgent: DocumentAgent? = null

    // State
    var selectedDocument by mutableStateOf<DocumentFile?>(null)
        private set

    var documentContent by mutableStateOf<String?>(null)
        private set

    var selectedDocumentIndexStatus by mutableStateOf<cc.unitmesh.devins.db.DocumentIndexRecord?>(null)
        private set

    var documents by mutableStateOf<List<DocumentFile>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isGenerating by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // Indexing Service
    private val indexRepository = cc.unitmesh.devins.db.DocumentIndexDatabaseRepository.getInstance()
    private val indexService =
        cc.unitmesh.devins.service.DocumentIndexService(workspace.fileSystem, indexRepository, scope)
    val indexingStatus = indexService.indexingStatus

    init {
        loadDocuments()
        initializeLLMService()
        indexService.indexWorkspace()
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
                        parserService = parserService,
                        renderer = renderer,
                        fileSystem = toolFileSystem,
                        shellExecutor = null,
                        mcpToolConfigService = mcpConfigService,
                        enableLLMStreaming = true
                    )
                    println("DocumentAgent initialized successfully")
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
                isLoading = true
                error = null

                val fileSystem = workspace.fileSystem
                val rootPath = workspace.rootPath

                if (rootPath == null) {
                    error = "No workspace root path configured"
                    return@launch
                }

                val markdownFiles = fileSystem
                    .searchFiles("*.md", maxDepth = 10, maxResults = 100).toMutableList()
                markdownFiles += fileSystem.searchFiles("*.pdf", maxDepth = 10, maxResults = 100)

                documents = markdownFiles.mapNotNull { relativePath ->
                    val name = relativePath.substringAfterLast('/')

                    try {
                        // Get file metadata
                        val content = fileSystem.readFile(relativePath)
                        val fileSize = content?.length?.toLong() ?: 0L

                        DocumentFile(
                            name = name,
                            path = relativePath,
                            metadata = DocumentMetadata(
                                totalPages = null,
                                chapterCount = 0, // Will be updated after parsing
                                parseStatus = ParseStatus.NOT_PARSED,
                                lastModified = Clock.System.now().toEpochMilliseconds(),
                                fileSize = fileSize,
                                language = "markdown",
                                mimeType = "text/markdown"
                            )
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

            } catch (e: Exception) {
                error = "Failed to load documents: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Load a single document file from absolute path
     */
    fun loadDocumentFromPath(absolutePath: String) {
        scope.launch {
            try {
                isLoading = true
                error = null

                val fileSystem = workspace.fileSystem
                val name = absolutePath.substringAfterLast('/')

                val content = fileSystem.readFile(absolutePath)
                    ?: run {
                        error = "Failed to read file: $absolutePath"
                        println("ERROR: Failed to read file: $absolutePath")
                        return@launch
                    }

                val doc = DocumentFile(
                    name = name,
                    path = absolutePath,
                    metadata = DocumentMetadata(
                        totalPages = null,
                        chapterCount = 0,
                        parseStatus = ParseStatus.NOT_PARSED,
                        lastModified = Clock.System.now().toEpochMilliseconds(),
                        fileSize = content.length.toLong(),
                        language = "markdown",
                        mimeType = "text/markdown"
                    )
                )

                documents = listOf(doc)

                selectDocument(doc)
            } catch (e: Exception) {
                error = "Failed to load document: ${e.message}"
                println("ERROR: Failed to load document: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun selectDocument(doc: DocumentFile) {
        selectedDocument = doc
        selectedDocumentIndexStatus = getDocumentIndexStatus(doc.path)
        scope.launch {
            try {
                isLoading = true
                error = null

                val fileSystem = workspace.fileSystem
                val content = fileSystem.readFile(doc.path)
                    ?: run {
                        error = "Failed to read file content"
                        documentContent = null // Clear stale content
                        return@launch
                    }

                documentContent = content

                // Register document with DocumentRegistry for DocQL queries
                try {
                    val parsedDoc = parserService.parse(doc, content)
                    DocumentRegistry.registerDocument(doc.path, parsedDoc, parserService)
                    println("Document registered with DocumentRegistry: ${doc.path}")
                } catch (e: Exception) {
                    println("Failed to register document: ${e.message}")
                    // Continue even if registration fails
                }

                val parsedDoc = parserService.parse(doc, content)
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
                documentContent = null // Clear stale content
                println("ERROR: Failed to process document: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun sendMessage(text: String) {
        scope.launch {
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
            } catch (e: Exception) {
                renderer.renderError("Error: ${e.message}")
                e.printStackTrace()
            } finally {
                isGenerating = false
            }
        }
    }

    fun stopGeneration() {
        renderer.forceStop()
        isGenerating = false
    }

    /**
     * Get the parser service for DocQL queries
     */
    fun getParserService(): DocumentParserService = parserService

    fun getDocumentIndexStatus(path: String): cc.unitmesh.devins.db.DocumentIndexRecord? {
        return indexService.getIndexStatus(path)
    }
}
