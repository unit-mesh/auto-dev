package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*

/**
 * File information for $.files queries
 *
 * @property path Full path of the file
 * @property name File name only
 * @property directory Directory path
 * @property extension File extension
 * @property content File content (loaded from DocumentRegistry/IndexProvider)
 * @property size Content size in characters
 */
data class FileInfo(
    val path: String,
    val name: String = path.substringAfterLast('/'),
    val directory: String = if (path.contains('/')) path.substringBeforeLast('/') else "",
    val extension: String = if (path.contains('.')) path.substringAfterLast('.') else "",
    val content: String? = null,
    val size: Int = content?.length ?: 0
)

/**
 * DocQL Executor - Factory/Facade for executing DocQL queries
 * 
 * This class maintains backward compatibility by delegating to specialized executors
 * based on the document format type:
 * - MarkdownDocQLExecutor for markdown/documentation files
 * - CodeDocQLExecutor for source code files
 */
class DocQLExecutor(
    private val documentFile: DocumentFile?,
    private val parserService: DocumentParserService?
) {
    private val delegate: BaseDocQLExecutor = createExecutor(documentFile, parserService)

    /**
     * Execute a DocQL query
     */
    suspend fun execute(query: DocQLQuery): DocQLResult {
        return delegate.execute(query)
    }

    companion object {
        /**
         * Create appropriate executor based on document format type
         */
        private fun createExecutor(
            documentFile: DocumentFile?,
            parserService: DocumentParserService?
        ): BaseDocQLExecutor {
            return when (documentFile?.metadata?.formatType) {
                DocumentFormatType.SOURCE_CODE -> CodeDocQLExecutor(documentFile, parserService)
                else -> MarkdownDocQLExecutor(documentFile, parserService)
            }
        }
    }
}

/**
 * Convenience function to parse and execute DocQL query
 */
suspend fun executeDocQL(
    queryString: String,
    documentFile: DocumentFile?,
    parserService: DocumentParserService?
): DocQLResult {
    return try {
        val query = parseDocQL(queryString)
        val executor = DocQLExecutor(documentFile, parserService)
        executor.execute(query)
    } catch (e: DocQLException) {
        DocQLResult.Error(e.message ?: "Parse error")
    } catch (e: Exception) {
        DocQLResult.Error(e.message ?: "Execution error")
    }
}
