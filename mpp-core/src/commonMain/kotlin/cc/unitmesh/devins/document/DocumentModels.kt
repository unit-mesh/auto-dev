package cc.unitmesh.devins.document

/**
 * 文档树节点（复用 FileTreeNode 架构）
 * 用于在文档导航面板中展示文档和文件夹的树形结构
 */
sealed class DocumentTreeNode(open val name: String, open val path: String)

/**
 * 文档文件夹节点
 * 可包含子文件夹和文档文件
 */
data class DocumentFolder(
    override val name: String,
    override val path: String,
    val children: MutableList<DocumentTreeNode> = mutableListOf(),
    var fileCount: Int = 0  // 递归统计的文件总数
) : DocumentTreeNode(name, path)

/**
 * 文档文件节点
 * 包含文档的元数据、目录结构和提取的实体
 */
data class DocumentFile(
    override val name: String,
    override val path: String,
    val metadata: DocumentMetadata,
    val toc: List<TOCItem> = emptyList(),
    val entities: List<Entity> = emptyList()
) : DocumentTreeNode(name, path)

/**
 * 文档元数据
 * 包含文档的基本信息和解析状态
 */
data class DocumentMetadata(
    val totalPages: Int? = null,        // PDF 文档的页数
    val chapterCount: Int = 0,          // 章节总数
    val parseStatus: ParseStatus = ParseStatus.NOT_PARSED,
    val lastModified: Long,             // 最后修改时间戳
    val fileSize: Long,                 // 文件大小（字节）
    val language: String? = null,       // 文档语言（如 "markdown", "kotlin"）
    val mimeType: String? = null,       // MIME 类型
    val formatType: DocumentFormatType = DocumentFormatType.PLAIN_TEXT  // 文档格式类型
)

/**
 * 文档解析状态
 */
enum class ParseStatus {
    NOT_PARSED,      // 未解析
    PARSING,         // 解析中
    PARSED,          // 已解析
    PARSE_FAILED     // 解析失败
}

/**
 * 目录项（Table of Contents Item）
 * 表示文档的章节结构
 */
data class TOCItem(
    val level: Int,                     // 层级（1=H1, 2=H2, 等）
    val title: String,                  // 章节标题
    val anchor: String,                 // 锚点 ID（如 "#section-id"）
    val page: Int? = null,              // 页码（PDF 文档）
    val lineNumber: Int? = null,        // 行号（源代码文档）
    val content: String? = null,        // 章节内容摘要
    val children: List<TOCItem> = emptyList()  // 子章节
)

/**
 * 提取的实体（术语、API、类、函数等）
 * 用于在结构化信息面板中展示文档中的关键信息
 */
sealed class Entity {
    abstract val name: String
    abstract val location: Location
    
    /**
     * 术语定义
     */
    data class Term(
        override val name: String,
        val definition: String?,
        override val location: Location
    ) : Entity()
    
    /**
     * API 接口
     */
    data class API(
        override val name: String,
        val signature: String?,
        override val location: Location
    ) : Entity()
    
    /**
     * 类定义
     */
    data class ClassEntity(
        override val name: String,
        val packageName: String?,
        override val location: Location
    ) : Entity()
    
    /**
     * 函数/方法定义
     */
    data class FunctionEntity(
        override val name: String,
        val signature: String?,
        override val location: Location
    ) : Entity()
    
    /**
     * 构造函数定义
     * 用于表示类的构造器（Java/Kotlin: constructor, Python: __init__）
     */
    data class ConstructorEntity(
        override val name: String,
        val className: String,
        val signature: String?,
        override val location: Location
    ) : Entity()
}

/**
 * 实体在文档中的位置信息
 */
data class Location(
    val anchor: String,         // 锚点 ID
    val page: Int? = null,      // 页码（PDF）
    val line: Int? = null       // 行号（代码）
)

/**
 * AI 章节引用
 * 当 AI 在回复中引用某个章节时使用
 */
data class ChapterReference(
    val chapter: String,        // 章节名称
    val page: Int? = null,      // 页码
    val anchor: String,         // 锚点
    val excerpt: String? = null // 引用的摘录内容
)

/**
 * Document format types for position tracking
 */
enum class DocumentFormatType {
    MARKDOWN,
    PDF,
    DOCX,
    HTML,
    PLAIN_TEXT,
    SOURCE_CODE  // For code files (Java, Kotlin, Python, etc.)
}

/**
 * Abstract position in a document
 * Supports multiple document formats (Markdown, PDF, DOCX, etc.)
 */
sealed class DocumentPosition {
    /**
     * Line-based position for text documents (Markdown, code, plain text)
     */
    data class LineRange(
        val startLine: Int,
        val endLine: Int,
        val startOffset: Int? = null,  // Character offset in document
        val endOffset: Int? = null     // Character offset in document
    ) : DocumentPosition() {
        /**
         * Check if this position contains a given line number
         */
        fun contains(lineNumber: Int): Boolean {
            return lineNumber in startLine..endLine
        }
        
        /**
         * Check if this position overlaps with another LineRange
         */
        fun overlaps(other: LineRange): Boolean {
            return startLine <= other.endLine && endLine >= other.startLine
        }
    }
    
    /**
     * Page-based position for paginated documents (PDF)
     */
    data class PageRange(
        val startPage: Int,
        val endPage: Int
    ) : DocumentPosition()
    
    /**
     * Section-based position for structured documents (DOCX)
     */
    data class SectionRange(
        val sectionId: String,
        val paragraphIndex: Int? = null
    ) : DocumentPosition()
}

/**
 * Position metadata for document chunks
 * Provides rich context for source attribution in Knowledge Base
 */
data class PositionMetadata(
    val documentPath: String,
    val formatType: DocumentFormatType,
    val position: DocumentPosition
) {
    /**
     * Format position as a human-readable location string
     * Examples:
     * - "/path/to/doc.md:10-15" for line range
     * - "/path/to/doc.md:10" for single line
     * - "/path/to/doc.pdf:page 5-7" for page range
     */
    fun toLocationString(): String {
        return when (position) {
            is DocumentPosition.LineRange -> {
                if (position.startLine == position.endLine) {
                    "$documentPath:${position.startLine}"
                } else {
                    "$documentPath:${position.startLine}-${position.endLine}"
                }
            }
            is DocumentPosition.PageRange -> {
                if (position.startPage == position.endPage) {
                    "$documentPath:page ${position.startPage}"
                } else {
                    "$documentPath:page ${position.startPage}-${position.endPage}"
                }
            }
            is DocumentPosition.SectionRange -> {
                if (position.paragraphIndex != null) {
                    "$documentPath:${position.sectionId}[${position.paragraphIndex}]"
                } else {
                    "$documentPath:${position.sectionId}"
                }
            }
        }
    }
}

/**
 * Document chunks (for vectorization and semantic search)
 * Now includes position metadata for source attribution
 */
data class DocumentChunk(
    val documentPath: String,
    val chapterTitle: String?,
    val content: String,
    val anchor: String,
    val page: Int? = null,
    val startLine: Int? = null,
    val endLine: Int? = null,
    val position: PositionMetadata? = null  // New: rich position metadata

)
