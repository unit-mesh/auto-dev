package cc.unitmesh.agent

import cc.unitmesh.agent.config.JsToolConfigFile
import cc.unitmesh.agent.document.DocumentAgent
import cc.unitmesh.agent.document.DocumentTask
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.devins.document.DocumentParserService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * JS-friendly version of DocumentTask
 */
@JsExport
data class JsDocumentTask(
    val query: String,
    val documentPath: String? = null
) {
    fun toCommon(): DocumentTask {
        return DocumentTask(
            query = query,
            documentPath = documentPath
        )
    }
}

/**
 * JS-friendly version of DocumentResult
 */
@JsExport
data class JsDocumentResult(
    val success: Boolean,
    val message: String
)

/**
 * JS-friendly Document Agent for CLI usage
 */
@JsExport
class JsDocumentAgent(
    private val llmService: cc.unitmesh.llm.JsKoogLLMService,
    private val parserService: DocumentParserService, // We might need a JS wrapper for this too if it's not JS-friendly
    private val renderer: JsCodingAgentRenderer? = null,
    private val toolConfig: JsToolConfigFile? = null
) {
    // Internal Kotlin DocumentAgent
    private val agent: DocumentAgent = DocumentAgent(
        llmService = llmService.service,
        parserService = parserService,
        renderer = if (renderer != null) JsRendererAdapter(renderer) else DefaultCodingAgentRenderer(),
        mcpToolConfigService = createToolConfigService(toolConfig)
    )

    /**
     * Create tool config service from JS tool config
     */
    private fun createToolConfigService(jsToolConfig: JsToolConfigFile?): cc.unitmesh.agent.config.McpToolConfigService {
        return if (jsToolConfig != null) {
            cc.unitmesh.agent.config.McpToolConfigService(jsToolConfig.toCommon())
        } else {
            cc.unitmesh.agent.config.McpToolConfigService(cc.unitmesh.agent.config.ToolConfigFile())
        }
    }

    /**
     * Execute document query task
     */
    @JsName("executeTask")
    fun executeTask(
        query: String,
        documentPath: String? = null,
        onChunk: ((String) -> Unit)? = null
    ): Promise<JsDocumentResult> {
        return GlobalScope.promise {
            val task = DocumentTask(
                query = query,
                documentPath = documentPath
            )

            val result = agent.execute(task, onProgress = onChunk ?: {})

            JsDocumentResult(
                success = result.success,
                message = result.content
            )
        }
    }

    /**
     * Register a document from content string
     */
    @JsName("registerDocument")
    fun registerDocument(
        path: String,
        content: String
    ): Promise<Boolean> {
        return GlobalScope.promise {
            try {
                // Get parser for the file type
                val parser = cc.unitmesh.devins.document.DocumentRegistry.getParserForFile(path)
                
                if (parser != null) {
                    // Create DocumentFile object
                    val name = path.substringAfterLast("/")
                    val formatType = cc.unitmesh.devins.document.DocumentParserFactory.detectFormat(path) 
                        ?: cc.unitmesh.devins.document.DocumentFormatType.PLAIN_TEXT
                    
                    val metadata = cc.unitmesh.devins.document.DocumentMetadata(
                        lastModified = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                        fileSize = content.length.toLong(),
                        formatType = formatType
                    )
                    
                    val documentFile = cc.unitmesh.devins.document.DocumentFile(
                        name = name,
                        path = path,
                        metadata = metadata
                    )
                    
                    // Parse the document
                    val parsedDoc = parser.parse(documentFile, content)
                    
                    // Register in registry
                    cc.unitmesh.devins.document.DocumentRegistry.registerDocument(path, parsedDoc, parser)
                    true
                } else {
                    // Silently skip files without parsers - this is expected for unknown file types
                    false
                }
            } catch (e: Exception) {
                // Only log actual errors, not expected skips
                console.error("Failed to register document: ${e.message}")
                false
            }
        }
    }
}
