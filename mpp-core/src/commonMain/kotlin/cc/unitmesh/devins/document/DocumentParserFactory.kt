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
     * @param formatType The document format type
     * @return true if the format is supported
     */
    fun isSupported(formatType: DocumentFormatType): Boolean {
        return parserProviders.containsKey(formatType)
    }
    
    /**
     * Get all supported format types
     * 
     * @return List of supported format types
     */
    fun getSupportedFormats(): List<DocumentFormatType> {
        return parserProviders.keys.toList()
    }
}

