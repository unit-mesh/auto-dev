package cc.unitmesh.devins.ui.compose.document

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.document.DocumentAgent
import cc.unitmesh.devins.document.*
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class DocumentReaderViewModel(
    private val workspace: Workspace,
    private val documentsPath: String? = null // Optional: specific documents directory
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Services
    private val parserService: DocumentParserService = MarkdownDocumentParser()
    private val llmService = KoogLLMService(ModelConfig.default())
    val renderer = ComposeRenderer()

    private val documentAgent = DocumentAgent(llmService, parserService, renderer)

    // State
    var selectedDocument by mutableStateOf<DocumentFile?>(null)
        private set

    var documentContent by mutableStateOf<String?>(null)
        private set

    var documents by mutableStateOf<List<DocumentFile>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isGenerating by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadDocuments()
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

                // Search for markdown files
                val searchPath = documentsPath ?: rootPath
                val markdownFiles = fileSystem.searchFiles("*.md", maxDepth = 5, maxResults = 100)

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
        println("=== Loading Document from Path ===")
        println("Path: $absolutePath")

        scope.launch {
            try {
                isLoading = true
                error = null

                val fileSystem = workspace.fileSystem
                val name = absolutePath.substringAfterLast('/')

                // Read file content using fileSystem
                val content = fileSystem.readFile(absolutePath)
                    ?: run {
                        error = "Failed to read file: $absolutePath"
                        println("ERROR: Failed to read file: $absolutePath")
                        return@launch
                    }

                println("File read successfully, size: ${content.length} bytes")

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
                println("Document added to list")

                selectDocument(doc)
                println("Document selected and parsing initiated")

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
        scope.launch {
            isLoading = true
            error = null

            // Read file content
            val fileSystem = workspace.fileSystem
            val content = fileSystem.readFile(doc.path)
                ?: run {
                    error = "Failed to read file content"
                    return@launch
                }

            documentContent = content

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

            isLoading = false
        }
    }

    fun sendMessage(text: String) {
        scope.launch {
            isGenerating = true
            renderer.addUserMessage(text)
            try {
                // Create DocumentTask and execute via agent
                val task = cc.unitmesh.agent.document.DocumentTask(
                    query = text,
                    documentPath = selectedDocument?.path
                )

                documentAgent.execute(task) { progress ->
                    // Progress callback (optional)
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
}
