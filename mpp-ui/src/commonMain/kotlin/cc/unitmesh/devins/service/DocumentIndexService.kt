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

    fun indexWorkspace() {
        scope.launch {
            try {
                _indexingStatus.value = IndexingStatus.Indexing(0, 0)
                // Search for all files, we will filter by supported types later
                val files = fileSystem.searchFiles("**/*")
                var indexedCount = 0
                val totalFiles = files.size
                
                files.forEach { path ->
                    indexFile(path)
                    indexedCount++
                    _indexingStatus.value = IndexingStatus.Indexing(indexedCount, totalFiles)
                }
            } finally {
                _indexingStatus.value = IndexingStatus.Idle
            }
        }
    }

    suspend fun indexFile(path: String) {
        val format = DocumentParserFactory.detectFormat(path) ?: return
        if (!DocumentParserFactory.isSupported(format)) return

        // Read file content
        // Note: ProjectFileSystem.readFile currently returns String.
        // For binary files (PDF, etc.), this might be problematic if not handled correctly by FS implementation.
        // However, TikaDocumentParser on JVM expects "raw bytes as string" (ISO-8859-1),
        // so if FS reads as UTF-8, it might corrupt binary data.
        // For now, we proceed with readFile.
        val content = fileSystem.readFile(path) ?: return
        
        // Calculate hash
        // Using content hash + size as a simple check
        val size = content.length.toLong()
        val hash = "${content.hashCode()}_$size"
        
        val existing = repository.get(path)
        if (existing != null && existing.hash == hash && existing.status == "INDEXED") {
            return
        }

            try {
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
        }
    }
    
    fun getIndexStatus(path: String): DocumentIndexRecord? {
        return repository.get(path)
    }
}

sealed class IndexingStatus {
    object Idle : IndexingStatus()
    data class Indexing(val current: Int, val total: Int) : IndexingStatus()
}
