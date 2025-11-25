package cc.unitmesh.devins.service

import cc.unitmesh.devins.db.DocumentIndexRepository
import cc.unitmesh.devins.document.DocumentFormatType
import cc.unitmesh.devins.document.DocumentIndexProvider
import cc.unitmesh.devins.document.DocumentParserFactory

/**
 * Implementation of DocumentIndexProvider that bridges DocumentRegistry with DocumentIndexService
 * 
 * This provider allows DocQLTool to access documents that have been indexed and stored
 * in the database, without creating direct module dependencies between mpp-core and mpp-ui.
 * 
 * Architecture:
 * - mpp-core defines DocumentIndexProvider interface
 * - mpp-ui implements this provider using DocumentIndexRepository
 * - DocumentAgent (in mpp-core) accepts and uses this provider
 * 
 * Usage:
 * ```kotlin
 * // In DocumentReaderViewModel or DocumentAgent initialization
 * val provider = DocumentIndexServiceProvider(repository)
 * DocumentRegistry.setIndexProvider(provider)
 * ```
 */
class DocumentIndexServiceProvider(
    private val repository: DocumentIndexRepository
) : DocumentIndexProvider {
    
    /**
     * Get list of all indexed document paths from the database
     * 
     * @return List of document paths that have been successfully indexed
     */
    override suspend fun getIndexedPaths(): List<String> {
        return try {
            repository.getAll()
                .filter { it.status == "INDEXED" }  // Only include successfully indexed documents
                .map { it.path }
        } catch (e: Exception) {
            println("Error getting indexed paths: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Load content and format type for an indexed document from the database
     * 
     * @param path Document path
     * @return Pair of (content, formatType) or (null, null) if not found or failed
     */
    override suspend fun loadIndexedDocument(path: String): Pair<String?, DocumentFormatType?> {
        return try {
            val record = repository.get(path)
            
            // Only return content if the document was successfully indexed
            if (record != null && record.status == "INDEXED" && record.content != null) {
                val formatType = DocumentParserFactory.detectFormat(path)
                record.content to formatType
            } else {
                null to null
            }
        } catch (e: Exception) {
            println("Error loading indexed document: $path - ${e.message}")
            null to null
        }
    }
    
    /**
     * Check if a document is indexed in the database
     * 
     * @param path Document path
     * @return true if the document is indexed and has content
     */
    override suspend fun isIndexed(path: String): Boolean {
        return try {
            val record = repository.get(path)
            record != null && record.status == "INDEXED" && record.content != null
        } catch (e: Exception) {
            println("Error checking index status: $path - ${e.message}")
            false
        }
    }
}

