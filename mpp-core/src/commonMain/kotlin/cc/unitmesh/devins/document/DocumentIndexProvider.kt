package cc.unitmesh.devins.document

/**
 * Provider interface for accessing indexed documents from persistent storage
 * 
 * This interface allows DocumentRegistry to access documents that have been
 * indexed and stored in a database, bridging the gap between in-memory cache
 * and persistent storage without creating module dependencies.
 * 
 * Implementation notes:
 * - Implementations should be provided by platform-specific modules (e.g., mpp-ui)
 * - The provider is optional - DocumentRegistry works without it
 * - When a provider is available, DocQLTool can query both memory and indexed documents
 * 
 * Usage:
 * ```kotlin
 * // In mpp-ui (implementation)
 * class DocumentIndexServiceProvider(
 *     private val repository: DocumentIndexRepository
 * ) : DocumentIndexProvider {
 *     override suspend fun getIndexedPaths(): List<String> = 
 *         repository.getAll().map { it.path }
 *     
 *     override suspend fun loadIndexedDocument(path: String): Pair<String?, DocumentFormatType?> {
 *         val record = repository.get(path)
 *         return record?.content to record?.let { 
 *             DocumentParserFactory.detectFormat(path) 
 *         }
 *     }
 * }
 * 
 * // Register provider
 * DocumentRegistry.setIndexProvider(provider)
 * ```
 */
interface DocumentIndexProvider {
    
    /**
     * Get list of all indexed document paths
     * 
     * @return List of document paths that have been indexed
     */
    suspend fun getIndexedPaths(): List<String>
    
    /**
     * Load content and format type for an indexed document
     * 
     * @param path Document path
     * @return Pair of (content, formatType) or (null, null) if not found
     */
    suspend fun loadIndexedDocument(path: String): Pair<String?, DocumentFormatType?>
    
    /**
     * Check if a document is indexed
     * 
     * @param path Document path
     * @return true if the document is indexed
     */
    suspend fun isIndexed(path: String): Boolean {
        val (content, _) = loadIndexedDocument(path)
        return content != null
    }
}

