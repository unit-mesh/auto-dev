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

class DocumentReaderViewModel(
    private val workspace: Workspace
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Services
    private val parserService: DocumentParserService = SimpleMarkdownParser()
    // TODO: Load config properly
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

    init {
        loadDocuments()
    }
    
    private fun loadDocuments() {
        // TODO: Load real documents from workspace
        // For now, use dummy data
        documents = createDummyDocuments()
    }
    
    fun selectDocument(doc: DocumentFile) {
        selectedDocument = doc
        scope.launch {
            isLoading = true
            // TODO: Load real content
            // For now, mock content
            documentContent = "# ${doc.name}\n\nContent of ${doc.name}..."
            
            // Parse document to build index for agent
            parserService.parse(doc, documentContent ?: "")
            
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
            } finally {
                isGenerating = false
            }
        }
    }
    
    fun stopGeneration() {
        // Implement stop logic
        renderer.forceStop()
        isGenerating = false
    }

    // --- Dummy Data Generators ---

    private fun createDummyDocuments(): List<DocumentFile> {
        val meta = DocumentMetadata(10, 5, ParseStatus.PARSED, 1678888888000L, 10240, "Markdown", "text/markdown")
        return listOf(
            DocumentFile("README.md", "README.md", meta),
            DocumentFile("Architecture.md", "docs/Architecture.md", meta),
            DocumentFile("API.md", "docs/API.md", meta)
        )
    }
}
