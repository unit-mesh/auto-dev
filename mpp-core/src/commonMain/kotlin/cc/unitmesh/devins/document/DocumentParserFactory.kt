package cc.unitmesh.devins.document

/**
 * Factory for creating document parsers based on file format
 * 
 * This factory provides a unified interface to create appropriate document parsers
 * for different file formats. It abstracts platform-specific parser implementations
 * while maintaining cross-platform compatibility.
 * 
 * Usage:
 * ```kotlin
 * val parser = DocumentParserFactory.createParser(DocumentFormatType.MARKDOWN)
 * val result = parser.parse(file, content)
 * ```
 */
object DocumentParserFactory {
    
    /**
     * Registry of parser providers for each format type
     * Can be extended with platform-specific parsers (e.g., Tika on JVM)
     */
    private val parserProviders = mutableMapOf<DocumentFormatType, () -> DocumentParserService>()
    
    init {
        // Register default Markdown parser (available on all platforms)
        registerParser(DocumentFormatType.MARKDOWN) { MarkdownDocumentParser() }
    }
    
    /**
     * Register a parser provider for a specific format type
     * This allows platform-specific implementations to register themselves
     * 
     * @param formatType The document format type
     * @param provider A function that creates a new parser instance
     */
    fun registerParser(formatType: DocumentFormatType, provider: () -> DocumentParserService) {
        parserProviders[formatType] = provider
    }
    
    /**
     * Create a parser for the given format type
     * 
     * @param formatType The document format type
     * @return A parser instance for the format, or null if not supported
     */
    fun createParser(formatType: DocumentFormatType): DocumentParserService? {
        return parserProviders[formatType]?.invoke()
    }
    
    /**
     * Detect format type from file extension
     * 
     * @param filePath The file path or name
     * @return The detected format type, or null if unknown
     */
    fun detectFormat(filePath: String): DocumentFormatType? {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "md", "markdown" -> DocumentFormatType.MARKDOWN
            "pdf" -> DocumentFormatType.PDF
            "doc", "docx", "ppt", "pptx" -> DocumentFormatType.DOCX  // Tika handles all Office formats
            "txt" -> DocumentFormatType.PLAIN_TEXT
            // Source code files
            "java", "kt", "kts", "js", "ts", "tsx", "py", "go", "rs", "cs" -> DocumentFormatType.SOURCE_CODE
            else -> null
        }
    }
    
    /**
     * Create a parser based on file path (auto-detect format)
     * 
     * @param filePath The file path
     * @return A parser instance, or null if format is not supported
     */
    fun createParserForFile(filePath: String): DocumentParserService? {
        val format = detectFormat(filePath) ?: return null
        return createParser(format)
    }
    
    /**
     * Check if a format is supported
     * 
     * @param formatType The format type to check
     * @return true if the format is supported
     */
    fun isSupported(formatType: DocumentFormatType): Boolean {
        return parserProviders.containsKey(formatType)
    }
    
    /**
     * Check if a document format is binary (requires byte-level reading)
     * 
     * @param formatType The document format type
     * @return true if the format is binary (PDF, DOC, DOCX, PPT, PPTX), false for text formats
     */
    fun isBinaryFormat(formatType: DocumentFormatType): Boolean {
        return when (formatType) {
            DocumentFormatType.PDF,
            DocumentFormatType.DOCX -> true  // DOCX covers DOC, DOCX, PPT, PPTX in Tika
            DocumentFormatType.MARKDOWN,
            DocumentFormatType.PLAIN_TEXT -> false
            else -> false
        }
    }
    
    /**
     * Get list of all supported format types
     *
     * @return List of supported format types
     */
    fun getSupportedFormats(): List<DocumentFormatType> {
        return parserProviders.keys.toList()
    }
    
    /**
     * Get all supported file extensions
     * 
     * @return List of supported file extensions (without dot)
     */
    fun getSupportedExtensions(): List<String> {
        return listOf(
            "md", "markdown",           // Markdown
            "pdf",                      // PDF
            "doc", "docx",              // Word
            "ppt", "pptx",              // PowerPoint
            "txt",                      // Plain text
            "html", "htm",              // HTML
            // Source code files
            "java", "kt", "kts",        // JVM languages
            "js", "ts", "tsx",          // JavaScript/TypeScript
            "py",                       // Python
            "go",                       // Go
            "rs",                       // Rust
            "cs"                        // C#
        )
    }
    
    /**
     * Get file search pattern for all supported document formats
     * 
     * @return Glob pattern like "*.{md,pdf,txt,...}"
     */
    fun getSearchPattern(): String {
        val extensions = getSupportedExtensions().joinToString(",")
        return "*.{$extensions}"
    }
    
    /**
     * Get MIME type for a file based on its extension
     * 
     * @param filePath The file path or name
     * @return MIME type string, or "application/octet-stream" if unknown
     */
    fun getMimeType(filePath: String): String {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "md", "markdown" -> "text/markdown"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            else -> "application/octet-stream"
        }
    }
    
    /**
     * Check if a file extension is supported
     * 
     * @param extension File extension (without dot)
     * @return true if the extension is supported
     */
    fun isSupportedExtension(extension: String): Boolean {
        return getSupportedExtensions().contains(extension.lowercase())
    }
}

