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
                        parserService = MarkdownDocumentParser(), // Default parser, actual parsing uses DocumentRegistry
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

                // Search for all supported document formats in one go
                val pattern = "*.{md,markdown,pdf,doc,docx,ppt,pptx,txt,html,htm}"
                val allDocuments = fileSystem.searchFiles(pattern, maxDepth = 10, maxResults = 1000)

                documents = allDocuments.mapNotNull { relativePath ->
                    val name = relativePath.substringAfterLast('/')
                    val extension = relativePath.substringAfterLast('.', "").lowercase()

                    try {
                        // Detect format type from file extension
                        val formatType = DocumentParserFactory.detectFormat(relativePath)
                            ?: DocumentFormatType.PLAIN_TEXT

                        // Get file metadata
                        val content = fileSystem.readFile(relativePath)
                        val fileSize = content?.length?.toLong() ?: 0L

                        // Determine MIME type based on format
                        val mimeType = when (formatType) {
                            DocumentFormatType.MARKDOWN -> "text/markdown"
                            DocumentFormatType.PDF -> "application/pdf"
                            DocumentFormatType.DOCX -> when (extension) {
                                "doc" -> "application/msword"
                                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                "ppt" -> "application/vnd.ms-powerpoint"
                                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                else -> "application/octet-stream"
                            }
                            DocumentFormatType.HTML -> "text/html"
                            DocumentFormatType.PLAIN_TEXT -> "text/plain"
                        }

                        DocumentFile(
                            name = name,
                            path = relativePath,
                            metadata = DocumentMetadata(
                                totalPages = null,
                                chapterCount = 0, // Will be updated after parsing
                                parseStatus = ParseStatus.NOT_PARSED,
                                lastModified = Clock.System.now().toEpochMilliseconds(),
                                fileSize = fileSize,
                                language = extension,
                                mimeType = mimeType,
                                formatType = formatType
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

                // Get the appropriate parser for this document format
                val parser = DocumentParserFactory.createParserForFile(doc.path)
                    ?: run {
                        error = "No parser available for file format: ${doc.path}"
                        println("ERROR: No parser available for: ${doc.path}")
                        return@launch
                    }

                // Register document with DocumentRegistry for DocQL queries
                try {
                    val parsedDoc = parser.parse(doc, content)
                    DocumentRegistry.registerDocument(doc.path, parsedDoc, parser)
                    println("Document registered with DocumentRegistry: ${doc.path}")
                } catch (e: Exception) {
                    println("Failed to register document: ${e.message}")
                    e.printStackTrace()
                    // Continue even if registration fails
                }

                val parsedDoc = parser.parse(doc, content)
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
