package cc.unitmesh.devins.document

/**
 * 文档解析服务接口
 * 支持文档解析和结构化查询 (ChapterQL/HeadingQL)
 */
interface DocumentParserService {
    /**
     * 解析文档文件，构建文档树
     */
    suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode

    /**
     * 解析二进制文档文件（用于 PDF, DOCX 等格式）
     * 默认实现：将字节转换为字符串后调用 parse
     */
    suspend fun parseBytes(file: DocumentFile, bytes: ByteArray): DocumentTreeNode {
        // Default: decode as UTF-8 and call parse
        val content = bytes.decodeToString()
        return parse(file, content)
    }

    /**
     * HeadingQL: 根据关键字或标题文本查找最匹配的标题节点及其内容
     */
    suspend fun queryHeading(keyword: String): List<DocumentChunk>

    /**
     * ChapterQL: 根据章节编号返回该章节及所有子章节内容
     */
    suspend fun queryChapter(chapterId: String): DocumentChunk?
    
    /**
     * 获取当前解析的文档内容缓存（用于调试或全量显示）
     */
    fun getDocumentContent(): String?
}
