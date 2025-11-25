package cc.unitmesh.devins.service

import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.db.DocumentIndexRecord
import cc.unitmesh.devins.db.DocumentIndexRepository
import cc.unitmesh.devins.document.DocumentFile
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
     * Index a single file
     * @param path File path to index
     * @return true if indexing succeeded, false otherwise
     */
    suspend fun indexFile(path: String): Boolean {
        val format = DocumentParserFactory.detectFormat(path) ?: return false
        if (!DocumentParserFactory.isSupported(format)) return false

        // Determine if we need to read as binary
        val isBinary = DocumentParserFactory.isBinaryFormat(format)

        // Read file content based on format type
        val content: String?
        val bytes: ByteArray?
        val size: Long
        val hash: String

        if (isBinary) {
            // Binary formats - read as bytes
            bytes = fileSystem.readFileAsBytes(path) ?: return false
            content = null
            size = bytes.size.toLong()
            hash = "${bytes.contentHashCode()}_$size"
        } else {
            // Text formats - read as string
            content = fileSystem.readFile(path) ?: return false
            bytes = null
            size = content.length.toLong()
            hash = "${content.hashCode()}_$size"
        }

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

                // Parse the document based on format type
                val parsedDoc = if (isBinary && bytes != null) {
                    // Use parseBytes for binary files
                    parser.parseBytes(docFile, bytes)
                } else if (content != null) {
                    // Text format
                    parser.parse(docFile, content)
                } else {
                    return false // Invalid state
                }

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
