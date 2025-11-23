package cc.unitmesh.devins.ui.compose.document

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
    val fileCount: Int = 0  // 递归统计的文件总数
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
    val mimeType: String? = null        // MIME 类型
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
 * 文档分块（用于向量化和语义搜索）
 */
data class DocumentChunk(
    val documentPath: String,
    val chapterTitle: String?,
    val content: String,
    val anchor: String,
    val page: Int? = null,
    val startLine: Int? = null,
    val endLine: Int? = null
)
