package cc.unitmesh.devins.completion

import cc.unitmesh.devins.filesystem.ProjectFileSystem

/**
 * 文件搜索接口
 * 基于 gemini-cli 的 FileSearch 实现
 */
interface FileSearch {
    /**
     * 初始化搜索引擎（爬取文件列表等）
     */
    suspend fun initialize()
    
    /**
     * 搜索匹配的文件
     * @param pattern 搜索模式（支持通配符和模糊匹配）
     * @param maxResults 最大结果数量
     * @return 匹配的文件路径列表
     */
    suspend fun search(pattern: String, maxResults: Int = 100): List<String>
}

/**
 * 搜索选项
 */
data class FileSearchOptions(
    val projectRoot: String,
    val enableRecursiveSearch: Boolean = true,
    val enableFuzzyMatch: Boolean = true,
    val maxDepth: Int = 10,
    val cache: Boolean = true,
    val ignoreDirs: List<String> = listOf(
        "node_modules", ".git", ".idea", "build", "out", "target",
        "dist", ".gradle", "venv", "__pycache__", "bin", ".next",
        "coverage", ".cache", "tmp", "temp"
    )
)

/**
 * 递归文件搜索实现
 * 支持全局搜索、模糊匹配和结果缓存
 */
class RecursiveFileSearch(
    private val fileSystem: ProjectFileSystem,
    private val options: FileSearchOptions
) : FileSearch {
    
    private var allFiles: List<String> = emptyList()
    private val resultCache = mutableMapOf<String, List<String>>()
    
    override suspend fun initialize() {
        // 爬取所有文件
        allFiles = crawlFiles()
    }
    
    override suspend fun search(pattern: String, maxResults: Int): List<String> {
        if (allFiles.isEmpty()) {
            throw IllegalStateException("FileSearch not initialized. Call initialize() first.")
        }
        
        val normalizedPattern = pattern.trim().ifEmpty { "*" }
        
        // 检查缓存
        resultCache[normalizedPattern]?.let { cached ->
            return cached.take(maxResults)
        }
        
        // 执行搜索
        val results = if (normalizedPattern.contains('*') || normalizedPattern.contains('?')) {
            // 通配符匹配
            filterByWildcard(allFiles, normalizedPattern)
        } else if (options.enableFuzzyMatch) {
            // 模糊匹配
            fuzzyMatch(allFiles, normalizedPattern)
        } else {
            // 简单包含匹配
            filterByContains(allFiles, normalizedPattern)
        }
        
        // 排序：目录优先，然后按路径长度，最后按字母顺序
        val sorted = results.sortedWith(compareBy(
            { !it.endsWith("/") },  // 目录优先
            { it.count { c -> c == '/' } },  // 路径深度
            { it.lowercase() }  // 字母顺序
        ))
        
        // 缓存结果
        val limited = sorted.take(maxResults)
        if (options.cache && results.size < 1000) {
            resultCache[normalizedPattern] = limited
        }
        
        return limited
    }
    
    /**
     * 爬取所有文件
     */
    private fun crawlFiles(): List<String> {
        return try {
            fileSystem.searchFiles(
                pattern = "*",
                maxDepth = options.maxDepth,
                maxResults = 10000
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 通配符过滤
     */
    private fun filterByWildcard(files: List<String>, pattern: String): List<String> {
        val regex = wildcardToRegex(pattern)
        return files.filter { file ->
            regex.matches(file) || regex.matches(file.substringAfterLast('/'))
        }
    }
    
    /**
     * 简单包含过滤
     */
    private fun filterByContains(files: List<String>, pattern: String): List<String> {
        val lowerPattern = pattern.lowercase()
        return files.filter { file ->
            file.lowercase().contains(lowerPattern) ||
            file.substringAfterLast('/').lowercase().contains(lowerPattern)
        }
    }
    
    /**
     * 模糊匹配（简化版 fzf）
     */
    private fun fuzzyMatch(files: List<String>, pattern: String): List<String> {
        val lowerPattern = pattern.lowercase()
        val patternChars = lowerPattern.toCharArray()
        
        return files.mapNotNull { file ->
            val fileName = file.substringAfterLast('/')
            val score = calculateFuzzyScore(fileName.lowercase(), patternChars)
            if (score > 0) file to score else null
        }
        .sortedByDescending { it.second }
        .map { it.first }
    }
    
    /**
     * 计算模糊匹配分数
     */
    private fun calculateFuzzyScore(text: String, patternChars: CharArray): Int {
        if (patternChars.isEmpty()) return 0
        
        var score = 0
        var lastMatchIndex = -1
        var consecutiveMatches = 0
        
        for (patternChar in patternChars) {
            val matchIndex = text.indexOf(patternChar, lastMatchIndex + 1)
            if (matchIndex == -1) {
                return 0  // 不匹配
            }
            
            // 基础分数
            score += 100
            
            // 连续匹配加分
            if (matchIndex == lastMatchIndex + 1) {
                consecutiveMatches++
                score += consecutiveMatches * 10
            } else {
                consecutiveMatches = 0
            }
            
            // 开头匹配加分
            if (matchIndex == 0) {
                score += 50
            }
            
            // 靠近开头加分
            score -= matchIndex
            
            lastMatchIndex = matchIndex
        }
        
        return score
    }
    
    /**
     * 将通配符模式转换为正则表达式
     */
    private fun wildcardToRegex(pattern: String): Regex {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return Regex(regexPattern, RegexOption.IGNORE_CASE)
    }
}

/**
 * 目录文件搜索实现
 * 只搜索指定目录（非递归）
 */
class DirectoryFileSearch(
    private val fileSystem: ProjectFileSystem,
    private val options: FileSearchOptions
) : FileSearch {
    
    override suspend fun initialize() {
        // 目录搜索不需要预加载
    }
    
    override suspend fun search(pattern: String, maxResults: Int): List<String> {
        val normalizedPattern = pattern.trim().ifEmpty { "*" }
        
        // 确定搜索目录
        val searchDir = if (normalizedPattern.contains('/')) {
            normalizedPattern.substringBeforeLast('/')
        } else {
            ""
        }
        
        // 列出目录内容
        val files = try {
            fileSystem.listFiles(searchDir, null)
        } catch (e: Exception) {
            emptyList()
        }
        
        // 过滤匹配的文件
        val regex = wildcardToRegex(normalizedPattern)
        val filtered = files.filter { file ->
            regex.matches(file) || regex.matches(file.substringAfterLast('/'))
        }
        
        // 排序
        return filtered.sortedWith(compareBy(
            { !it.endsWith("/") },
            { it.lowercase() }
        )).take(maxResults)
    }
    
    private fun wildcardToRegex(pattern: String): Regex {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return Regex(regexPattern, RegexOption.IGNORE_CASE)
    }
}

/**
 * 文件搜索工厂
 */
object FileSearchFactory {
    fun create(
        fileSystem: ProjectFileSystem,
        options: FileSearchOptions
    ): FileSearch {
        return if (options.enableRecursiveSearch) {
            RecursiveFileSearch(fileSystem, options)
        } else {
            DirectoryFileSearch(fileSystem, options)
        }
    }
}

