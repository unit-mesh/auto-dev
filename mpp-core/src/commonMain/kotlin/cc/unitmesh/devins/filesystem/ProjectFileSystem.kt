package cc.unitmesh.devins.filesystem

/**
 * 跨平台文件系统抽象接口
 * 提供项目文件读写和路径操作能力
 */
interface ProjectFileSystem {
    /**
     * 获取项目根路径
     */
    fun getProjectPath(): String?
    
    /**
     * 读取文件内容
     * @param path 文件路径（相对于项目根目录或绝对路径）
     * @return 文件内容，如果文件不存在返回 null
     */
    fun readFile(path: String): String?

    /**
     * 读取文件内容为字节数组（用于二进制文件）
     * @param path 文件路径（相对于项目根目录或绝对路径）
     * @return 文件内容的字节数组，如果文件不存在返回 null
     */
    fun readFileAsBytes(path: String): ByteArray?

    /**
     * 写入文件内容
     * @param path 文件路径（相对于项目根目录或绝对路径）
     * @param content 要写入的内容
     * @return 是否写入成功
     */
    fun writeFile(path: String, content: String): Boolean

    /**
     * 检查文件或目录是否存在
     * @param path 文件或目录路径
     */
    fun exists(path: String): Boolean

    /**
     * 检查路径是否为目录
     * @param path 路径
     * @return 如果是目录返回 true，否则返回 false
     */
    fun isDirectory(path: String): Boolean
    
    /**
     * 列出目录下的文件
     * @param path 目录路径
     * @param pattern 文件名模式（支持通配符，如 "*.md"）
     * @return 匹配的文件路径列表
     */
    fun listFiles(path: String, pattern: String? = null): List<String>
    
    /**
     * 递归搜索项目中的文件
     * @param pattern 文件名模式（支持通配符，如 "*controller*"）
     * @param maxDepth 最大搜索深度，默认为 10
     * @param maxResults 最大结果数量，默认为 100
     * @return 匹配的文件相对路径列表
     */
    fun searchFiles(pattern: String, maxDepth: Int = 10, maxResults: Int = 100): List<String>
    
    /**
     * 解析相对路径为绝对路径
     * @param relativePath 相对于项目根目录的路径
     * @return 绝对路径
     */
    fun resolvePath(relativePath: String): String

    /**
     * 创建目录
     * @param path 目录路径
     * @return 是否创建成功
     */
    fun createDirectory(path: String): Boolean = true
}

/**
 * 空实现，用于测试或没有文件系统的场景
 */
class EmptyFileSystem : ProjectFileSystem {
    override fun getProjectPath(): String? = null
    override fun readFile(path: String): String? = null
    override fun readFileAsBytes(path: String): ByteArray? = null
    override fun writeFile(path: String, content: String): Boolean = false
    override fun exists(path: String): Boolean = false
    override fun isDirectory(path: String): Boolean = false
    override fun listFiles(path: String, pattern: String?): List<String> = emptyList()
    override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> = emptyList()
    override fun resolvePath(relativePath: String): String = relativePath
    override fun createDirectory(path: String): Boolean = false
}

/**
 * 基于路径的简单文件系统实现
 * 使用 expect/actual 实现跨平台文件操作
 */
expect class DefaultFileSystem(projectPath: String) : ProjectFileSystem {
    override fun getProjectPath(): String?
    override fun readFile(path: String): String?
    override fun readFileAsBytes(path: String): ByteArray?
    override fun writeFile(path: String, content: String): Boolean
    override fun exists(path: String): Boolean
    override fun isDirectory(path: String): Boolean
    override fun listFiles(path: String, pattern: String?): List<String>
    override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String>
    override fun resolvePath(relativePath: String): String
    override fun createDirectory(path: String): Boolean
}

/**
 * Type alias for backward compatibility
 */
typealias DefaultProjectFileSystem = DefaultFileSystem

