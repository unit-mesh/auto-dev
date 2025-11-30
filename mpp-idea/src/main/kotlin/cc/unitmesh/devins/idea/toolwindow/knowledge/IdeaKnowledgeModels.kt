package cc.unitmesh.devins.idea.toolwindow.knowledge

import cc.unitmesh.devins.document.DocumentFile
import cc.unitmesh.devins.document.DocumentMetadata
import cc.unitmesh.devins.document.TOCItem
import cc.unitmesh.devins.document.Entity

/**
 * State for Knowledge Agent in IntelliJ IDEA plugin.
 * Aligned with DocumentReaderViewModel from mpp-ui.
 */
data class IdeaKnowledgeState(
    // Document list
    val documents: List<IdeaDocumentFile> = emptyList(),
    val filteredDocuments: List<IdeaDocumentFile> = emptyList(),
    val selectedDocument: IdeaDocumentFile? = null,
    
    // Document content
    val documentContent: String? = null,
    val parsedContent: String? = null,
    
    // Search
    val searchQuery: String = "",
    
    // Loading states
    val isLoading: Boolean = false,
    val isIndexing: Boolean = false,
    val isGenerating: Boolean = false,
    
    // Index status
    val indexingProgress: IndexingProgress = IndexingProgress(),
    
    // Navigation
    val targetLineNumber: Int? = null,
    val highlightedText: String? = null,
    
    // Error
    val error: String? = null
)

/**
 * Document file representation for IDEA plugin.
 * Simplified version of DocumentFile from mpp-core.
 */
data class IdeaDocumentFile(
    val name: String,
    val path: String,
    val metadata: IdeaDocumentMetadata,
    val toc: List<TOCItem> = emptyList(),
    val entities: List<Entity> = emptyList()
) {
    companion object {
        fun fromDocumentFile(doc: DocumentFile): IdeaDocumentFile {
            return IdeaDocumentFile(
                name = doc.name,
                path = doc.path,
                metadata = IdeaDocumentMetadata(
                    totalPages = doc.metadata.totalPages,
                    chapterCount = doc.metadata.chapterCount,
                    lastModified = doc.metadata.lastModified,
                    fileSize = doc.metadata.fileSize,
                    language = doc.metadata.language,
                    mimeType = doc.metadata.mimeType,
                    formatType = doc.metadata.formatType.name
                ),
                toc = doc.toc,
                entities = doc.entities
            )
        }
    }
}

/**
 * Document metadata for IDEA plugin.
 */
data class IdeaDocumentMetadata(
    val totalPages: Int? = null,
    val chapterCount: Int = 0,
    val lastModified: Long = 0,
    val fileSize: Long = 0,
    val language: String? = null,
    val mimeType: String? = null,
    val formatType: String = "PLAIN_TEXT"
)

/**
 * Indexing progress information.
 */
data class IndexingProgress(
    val status: IndexingStatus = IndexingStatus.IDLE,
    val totalDocuments: Int = 0,
    val processedDocuments: Int = 0,
    val currentDocument: String? = null,
    val message: String? = null
)

/**
 * Indexing status enum.
 */
enum class IndexingStatus {
    IDLE,
    SCANNING,
    INDEXING,
    COMPLETED,
    ERROR
}

// TODO: Implement document indexing feature using IdeaDocumentIndexRecord
// /**
//  * Document index record for tracking indexed documents.
//  * Reserved for future indexing features.
//  */
// data class IdeaDocumentIndexRecord(
//     val path: String,
//     val hash: String,
//     val lastModified: Long,
//     val status: String,
//     val content: String? = null,
//     val error: String? = null,
//     val indexedAt: Long
// )

