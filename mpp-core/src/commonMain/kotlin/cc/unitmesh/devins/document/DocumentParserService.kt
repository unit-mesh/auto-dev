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

/**
 * 简单的 Markdown 解析器实现
 */
class SimpleMarkdownParser : DocumentParserService {
    private var currentContent: String? = null
    private var currentChunks: List<DocumentChunk> = emptyList()
    private var currentToc: List<TOCItem> = emptyList()

    override fun getDocumentContent(): String? = currentContent

    override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode {
        currentContent = content
        val lines = content.lines()
        val tocItems = mutableListOf<TOCItem>()
        val chunks = mutableListOf<DocumentChunk>()
        
        var currentChapterId = mutableListOf<Int>()
        var lastHeaderLine = -1
        
        // 简单的 Markdown 解析逻辑
        lines.forEachIndexed { index, line ->
            if (line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length
                val title = line.drop(level).trim()
                
                // 更新章节编号
                if (level > currentChapterId.size) {
                    while (level > currentChapterId.size) currentChapterId.add(1)
                } else {
                    while (level < currentChapterId.size) currentChapterId.removeLast()
                    if (currentChapterId.isNotEmpty()) {
                        currentChapterId[currentChapterId.lastIndex] = currentChapterId.last() + 1
                    } else {
                        currentChapterId.add(1)
                    }
                }
                
                val chapterId = currentChapterId.joinToString(".")
                val anchor = "#${title.lowercase().replace(" ", "-")}"
                
                // 构建 TOC Item
                // 注意：这里简化了层级构建，实际应该递归构建树
                // 为了演示，我们先创建一个扁平列表，后续可以转换为树
                // 但 DocumentFile 需要树形 TOC，这里简化处理
                
                // 提取上一章节的内容块
                if (lastHeaderLine != -1 && chunks.isNotEmpty()) {
                    val lastChunk = chunks.last()
                    val chunkContent = lines.subList(lastChunk.startLine ?: 0, index).joinToString("\n")
                    chunks[chunks.lastIndex] = lastChunk.copy(content = chunkContent, endLine = index - 1)
                }
                
                // 创建新块
                chunks.add(DocumentChunk(
                    documentPath = file.path,
                    chapterTitle = title,
                    content = "", // 稍后填充
                    anchor = anchor,
                    startLine = index + 1, // 内容从标题下一行开始
                    page = null // Markdown 无页码
                ))
                
                lastHeaderLine = index
            }
        }
        
        // 处理最后一个块
        if (lastHeaderLine != -1 && chunks.isNotEmpty()) {
            val lastChunk = chunks.last()
            val chunkContent = lines.subList(lastChunk.startLine ?: 0, lines.size).joinToString("\n")
            chunks[chunks.lastIndex] = lastChunk.copy(content = chunkContent, endLine = lines.size)
        }
        
        currentChunks = chunks
        // TODO: 构建真正的 TOC 树
        
        return file // 返回更新后的 file (实际应该返回包含 TOC 的 file)
    }

    override suspend fun queryHeading(keyword: String): List<DocumentChunk> {
        return currentChunks.filter { 
            it.chapterTitle?.contains(keyword, ignoreCase = true) == true ||
            it.content.contains(keyword, ignoreCase = true)
        }.sortedByDescending { 
            // 简单的相关性排序：标题匹配优先
            if (it.chapterTitle?.contains(keyword, ignoreCase = true) == true) 2 else 1
        }
    }

    override suspend fun queryChapter(chapterId: String): DocumentChunk? {
        // 这里的 chapterId 假设是 "1.2.3" 格式
        // 但我们的 Chunk 目前没有存储 chapterId，需要改进
        // 暂时简单实现：根据标题匹配（假设 chapterId 实际上是标题的一部分或者是索引）
        
        // 如果 chapterId 是数字索引 (e.g. "1", "2")
        chapterId.toIntOrNull()?.let { index ->
            if (index > 0 && index <= currentChunks.size) {
                return currentChunks[index - 1]
            }
        }
        
        return currentChunks.find { it.anchor == chapterId || it.anchor == "#$chapterId" }
    }
}
