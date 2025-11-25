package cc.unitmesh.devins.service

import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.db.DocumentIndexRecord
import cc.unitmesh.devins.db.DocumentIndexRepository
import cc.unitmesh.devins.document.DocumentFile
import cc.unitmesh.devins.document.DocumentFormatType
import cc.unitmesh.devins.document.DocumentMetadata
import cc.unitmesh.devins.document.DocumentParserFactory
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DocumentIndexService(
    private val fileSystem: ProjectFileSystem,
    private val repository: DocumentIndexRepository,
    private val scope: CoroutineScope
) {
    private val _indexingStatus = MutableStateFlow<IndexingStatus>(IndexingStatus.Idle)
    val indexingStatus: StateFlow<IndexingStatus> = _indexingStatus

    /**
     * Index documents from a provided list
     * @param documents List of DocumentFile to index
     */
    fun indexDocuments(documents: List<DocumentFile>) {
        scope.launch {
            try {
                val totalFiles = documents.size
                var indexedCount = 0
                var succeededCount = 0
                var failedCount = 0
                
                _indexingStatus.value = IndexingStatus.Indexing(0, totalFiles, 0, 0)
                
                documents.forEach { doc ->
                    val success = indexFile(doc.path)
                    indexedCount++
                    if (success) {
                        succeededCount++
                    } else {
                        failedCount++
                    }
                    _indexingStatus.value = IndexingStatus.Indexing(
                        indexedCount, 
                        totalFiles, 
                        succeededCount, 
                        failedCount
                    )
                }
                
                _indexingStatus.value = IndexingStatus.Completed(
                    totalFiles,
                    succeededCount,
                    failedCount
                )
            } catch (e: Exception) {
                println("Error during indexing: ${e.message}")
                _indexingStatus.value = IndexingStatus.Idle
            }
        }
    }
    
    /**
     * Reset indexing status to Idle
     */
    fun resetStatus() {
        _indexingStatus.value = IndexingStatus.Idle
    }
    
    /**
     * Legacy method - index all files in workspace
     * Deprecated: Use indexDocuments(documents) instead
     */
    @Deprecated("Use indexDocuments(documents) instead", ReplaceWith("indexDocuments(documents)"))
    fun indexWorkspace() {
        scope.launch {
            try {
                _indexingStatus.value = IndexingStatus.Indexing(0, 0)
                // Search for all supported document formats
                val pattern = "**/*.{md,markdown,pdf,doc,docx,ppt,pptx,txt,html,htm}"
                val files = fileSystem.searchFiles(pattern)
                var indexedCount = 0
                var succeededCount = 0
                var failedCount = 0
                val totalFiles = files.size
                
                files.forEach { path ->
                    val success = indexFile(path)
                    indexedCount++
                    if (success) {
                        succeededCount++
                    } else {
                        failedCount++
                    }
                    _indexingStatus.value = IndexingStatus.Indexing(
                        indexedCount,
                        totalFiles,
                        succeededCount,
                        failedCount
                    )
                }
                
                _indexingStatus.value = IndexingStatus.Completed(
                    totalFiles,
                    succeededCount,
                    failedCount
                )
            } catch (e: Exception) {
                println("Error during indexing: ${e.message}")
                _indexingStatus.value = IndexingStatus.Idle
            }
        }
    }

    /**
     * Index a single file
     * @param path File path to index
     * @return true if indexing succeeded, false otherwise
     */
    suspend fun indexFile(path: String): Boolean {
        val format = DocumentParserFactory.detectFormat(path) ?: return false
        if (!DocumentParserFactory.isSupported(format)) return false

        // Read file content
        // Note: ProjectFileSystem.readFile currently returns String.
        // For binary files (PDF, etc.), this might be problematic if not handled correctly by FS implementation.
        // However, TikaDocumentParser on JVM expects "raw bytes as string" (ISO-8859-1),
        // so if FS reads as UTF-8, it might corrupt binary data.
        // For now, we proceed with readFile.
        val content = fileSystem.readFile(path) ?: return false
        
        // Calculate hash
        // Using content hash + size as a simple check
        val size = content.length.toLong()
        val hash = "${content.hashCode()}_$size"
        
        val existing = repository.get(path)
        if (existing != null && existing.hash == hash && existing.status == "INDEXED") {
            return true // Already indexed
        }

        return try {
            val parser = DocumentParserFactory.createParser(format)
            if (parser != null) {
                val docFile = DocumentFile(
                    name = path.substringAfterLast('/'),
                    path = path,
                    metadata = DocumentMetadata(
                        lastModified = 0, // Unknown from FS
                        fileSize = size,
                        formatType = format
                    )
                )
                
                // Parse the document
                val parsedDoc = parser.parse(docFile, content)
                
                // Store extracted text content (not original binary)
                // For Tika: extracted text from PDF/PPTX
                // For Markdown: original markdown text
                val extractedContent = parser.getDocumentContent() ?: content
                
                repository.save(DocumentIndexRecord(
                    path = path,
                    hash = hash,
                    lastModified = 0,
                    status = "INDEXED",
                    content = extractedContent,  // Store extracted text (not binary)
                    error = null,
                    indexedAt = Platform.getCurrentTimestamp()
                ))
                
                println("✓ Indexed: $path")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            repository.save(DocumentIndexRecord(
                path = path,
                hash = hash,
                lastModified = 0,
                status = "FAILED",
                content = null,
                error = e.message ?: "Unknown error",
                indexedAt = Platform.getCurrentTimestamp()
            ))
            println("✗ Failed to index: $path - ${e.message}")
            false
        }
    }
    
    fun getIndexStatus(path: String): DocumentIndexRecord? {
        return repository.get(path)
    }
}

sealed class IndexingStatus {
    object Idle : IndexingStatus()
    data class Indexing(
        val current: Int,
        val total: Int,
        val succeeded: Int = 0,
        val failed: Int = 0
    ) : IndexingStatus()
    data class Completed(
        val total: Int,
        val succeeded: Int,
        val failed: Int
    ) : IndexingStatus()
}
