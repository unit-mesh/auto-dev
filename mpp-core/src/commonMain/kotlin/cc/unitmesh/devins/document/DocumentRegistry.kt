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
    
    /**
     * Optional provider for accessing indexed documents from persistent storage
     * This allows querying documents that were indexed but not in memory cache
     */
    private var indexProvider: DocumentIndexProvider? = null
    
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
     * If document is not in memory but available in index, it will be loaded first
     * 
     * @param documentPath Document path
     * @param docqlQuery DocQL query string (e.g., "$.content.heading('title')")
     * @return Query result or null if document not found
     */
    suspend fun queryDocument(documentPath: String, docqlQuery: String): DocQLResult? {
        // Try to get from memory first
        var docPair = getDocument(documentPath)
        
        // If not in memory, try loading from index
        if (docPair == null && indexProvider != null) {
            val loaded = loadFromIndex(documentPath)
            if (loaded) {
                docPair = getDocument(documentPath)
            }
        }
        
        val (document, parser) = docPair ?: return null
        
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
     * Set the document index provider
     * This allows access to documents stored in persistent storage
     * 
     * @param provider Document index provider or null to clear
     */
    fun setIndexProvider(provider: DocumentIndexProvider?) {
        indexProvider = provider
        logger.info { "Document index provider ${if (provider != null) "set" else "cleared"}" }
    }
    
    /**
     * Get the current document index provider
     */
    fun getIndexProvider(): DocumentIndexProvider? {
        return indexProvider
    }
    
    /**
     * Get all registered document paths (in-memory only)
     * For all available paths including indexed documents, use getAllAvailablePaths()
     */
    fun getRegisteredPaths(): List<String> {
        return documentCache.keys.toList()
    }
    
    /**
     * Get all available document paths (both in-memory and indexed)
     * 
     * @return Combined list of paths from memory cache and index provider
     */
    suspend fun getAllAvailablePaths(): List<String> {
        val memoryPaths = documentCache.keys.toSet()
        val indexedPaths = indexProvider?.getIndexedPaths()?.toSet() ?: emptySet()
        return (memoryPaths + indexedPaths).sorted()
    }
    
    /**
     * Check if a document is registered in memory
     */
    fun isDocumentRegistered(path: String): Boolean {
        return documentCache.containsKey(path)
    }
    
    /**
     * Check if a document is available (either in memory or indexed)
     * 
     * @param path Document path
     * @return true if document is in memory or indexed
     */
    suspend fun isDocumentAvailable(path: String): Boolean {
        if (documentCache.containsKey(path)) {
            return true
        }
        return indexProvider?.isIndexed(path) ?: false
    }
    
    /**
     * Load a document from index provider and register it in memory
     * This is automatically called by queryDocument when needed
     * 
     * @param path Document path
     * @return true if successfully loaded and registered
     */
    suspend fun loadFromIndex(path: String): Boolean {
        if (documentCache.containsKey(path)) {
            return true // Already in memory
        }
        
        val provider = indexProvider ?: return false
        val (content, formatType) = provider.loadIndexedDocument(path)
        
        if (content == null || formatType == null) {
            logger.warn { "Document not found in index: $path" }
            return false
        }
        
        return try {
            val parser = DocumentParserFactory.createParser(formatType)
            if (parser == null) {
                logger.warn { "No parser available for format: $formatType" }
                return false
            }
            
            val docFile = DocumentFile(
                name = path.substringAfterLast('/'),
                path = path,
                metadata = DocumentMetadata(
                    lastModified = 0,
                    fileSize = content.length.toLong(),
                    formatType = formatType
                )
            )
            
            val parsedDoc = parser.parse(docFile, content)
            registerDocument(path, parsedDoc, parser)
            logger.info { "Loaded document from index: $path" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to load document from index: $path" }
            false
        }
    }
}

/**
 * Platform-specific initialization function
 * Implemented via expect/actual pattern
 */
expect fun platformInitialize()

