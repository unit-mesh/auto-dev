package cc.unitmesh.devins.document

import cc.unitmesh.devins.document.docql.DocQLExecutor
import cc.unitmesh.devins.document.docql.DocQLResult
import cc.unitmesh.devins.document.docql.parseDocQL
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Document Registry - manages document parsers and parsed documents
 * 
 * This registry provides a centralized way to:
 * 1. Register and retrieve document parsers for different formats
 * 2. Cache parsed documents for efficient DocQL queries
 * 3. Support platform-specific parser initialization
 * 
 * Usage:
 * ```kotlin
 * // Register a parser (done automatically by platform initialization)
 * DocumentRegistry.registerParser(DocumentFormatType.PDF, TikaDocumentParser())
 * 
 * // Parse and register a document
 * val parser = DocumentRegistry.getParser(DocumentFormatType.PDF)
 * val parsedDoc = parser?.parse(file, content)
 * DocumentRegistry.registerDocument(file.path, parsedDoc, parser)
 * 
 * // Query via DocQL
 * val result = DocumentRegistry.queryDocument(filePath, "$.content.heading('Introduction')")
 * ```
 */
object DocumentRegistry {
    
    /**
     * Cache of parsed documents with their parsers
     * Key: document path, Value: Pair(DocumentFile, Parser)
     */
    private val documentCache = mutableMapOf<String, Pair<DocumentTreeNode, DocumentParserService>>()
    
    /**
     * Flag to track if platform-specific parsers have been initialized
     */
    private var initialized = false
    
    init {
        // Register Markdown parser (available on all platforms)
        DocumentParserFactory.registerParser(DocumentFormatType.MARKDOWN) { MarkdownDocumentParser() }
    }
    
    /**
     * Initialize platform-specific document parsers
     * This should be called automatically by platform-specific code
     */
    fun initializePlatformParsers() {
        if (!initialized) {
            initialized = true
            logger.info { "Initializing platform-specific document parsers" }
            // Platform-specific initialization happens via expect/actual
            platformInitialize()
        }
    }
    
    /**
     * Register a document with its parser for future queries
     * 
     * @param path Document path (unique identifier)
     * @param document Parsed document tree node
     * @param parser Parser service used for queries
     */
    fun registerDocument(
        path: String,
        document: DocumentTreeNode,
        parser: DocumentParserService
    ) {
        documentCache[path] = document to parser
        logger.debug { "Registered document: $path" }
    }
    
    /**
     * Get a registered document and its parser
     * 
     * @param path Document path
     * @return Pair of (DocumentTreeNode, Parser) or null if not found
     */
    fun getDocument(path: String): Pair<DocumentTreeNode, DocumentParserService>? {
        return documentCache[path]
    }
    
    /**
     * Get parser for a specific format
     * Ensures platform parsers are initialized
     * 
     * @param formatType Document format type
     * @return Parser service or null if not supported
     */
    fun getParser(formatType: DocumentFormatType): DocumentParserService? {
        initializePlatformParsers()
        return DocumentParserFactory.createParser(formatType)
    }
    
    /**
     * Get parser for a file path (auto-detect format)
     * 
     * @param filePath File path
     * @return Parser service or null if format not supported
     */
    fun getParserForFile(filePath: String): DocumentParserService? {
        initializePlatformParsers()
        return DocumentParserFactory.createParserForFile(filePath)
    }
    
    /**
     * Query a registered document using DocQL
     * 
     * @param documentPath Document path
     * @param docqlQuery DocQL query string (e.g., "$.content.heading('title')")
     * @return Query result or null if document not found
     */
    suspend fun queryDocument(documentPath: String, docqlQuery: String): DocQLResult? {
        val (document, parser) = getDocument(documentPath) ?: return null
        
        if (document !is DocumentFile) {
            logger.warn { "Document at $documentPath is not a file" }
            return null
        }
        
        return try {
            val query = parseDocQL(docqlQuery)
            val executor = DocQLExecutor(document, parser)
            executor.execute(query)
        } catch (e: Exception) {
            logger.error(e) { "Failed to execute DocQL query: $docqlQuery" }
            DocQLResult.Error(e.message ?: "Query execution failed")
        }
    }
    
    /**
     * Clear document cache
     */
    fun clearCache() {
        documentCache.clear()
        logger.info { "Document cache cleared" }
    }
    
    /**
     * Get all registered document paths
     */
    fun getRegisteredPaths(): List<String> {
        return documentCache.keys.toList()
    }
    
    /**
     * Check if a document is registered
     */
    fun isDocumentRegistered(path: String): Boolean {
        return documentCache.containsKey(path)
    }
}

/**
 * Platform-specific initialization function
 * Implemented via expect/actual pattern
 */
expect fun platformInitialize()

