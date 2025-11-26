package cc.unitmesh.agent.scoring

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Rich metadata for a document, extracted during indexing.
 * This metadata is used by LLMMetadataReranker for intelligent relevance scoring.
 * 
 * ## Storage
 * 
 * This metadata can be serialized to JSON and stored alongside the document content
 * in the document index, or computed on-demand during query time.
 * 
 * ## Extraction
 * 
 * Use [DocumentRichMetadataExtractor] to extract this metadata from parsed documents.
 */
@Serializable
data class DocumentRichMetadata(
    /** Document path (unique identifier) */
    val path: String,
    /** File name without path */
    val fileName: String,
    /** File extension */
    val extension: String,
    /** Parent directory */
    val directory: String,
    /** Document format type ("markdown", "kotlin", "java", etc.) */
    val formatType: String,
    
    // === Document Structure ===
    
    /** Primary heading (H1) of the document */
    val h1Heading: String? = null,
    /** All headings in document (for TOC) */
    val headings: List<HeadingInfo> = emptyList(),
    /** Total number of headings */
    val headingCount: Int = 0,
    
    // === Content Statistics ===
    
    /** Total content length in characters */
    val contentLength: Int = 0,
    /** Number of lines */
    val lineCount: Int = 0,
    /** Number of code blocks (for markdown) */
    val codeBlockCount: Int = 0,
    /** Number of links/references */
    val linkCount: Int = 0,
    
    // === Entities ===
    
    /** Classes defined in document */
    val classes: List<String> = emptyList(),
    /** Functions defined in document */
    val functions: List<String> = emptyList(),
    /** Terms/definitions in document */
    val terms: List<String> = emptyList(),
    
    // === Timestamps ===
    
    /** Last modification timestamp */
    val lastModified: Long = 0,
    /** File size in bytes */
    val fileSize: Long = 0,
    /** When this metadata was extracted */
    val extractedAt: Long = 0,
    
    // === Relationships ===
    
    /** Documents this document references */
    val outgoingRefs: List<String> = emptyList(),
    /** Documents that reference this document */
    val incomingRefs: List<String> = emptyList(),
    
    // === Keywords ===
    
    /** Extracted keywords/tags */
    val keywords: List<String> = emptyList(),
    /** Language of content (e.g., "en", "zh") */
    val language: String? = null
) {
    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true 
            encodeDefaults = true
        }
        
        /**
         * Serialize metadata to JSON string.
         */
        fun toJson(metadata: DocumentRichMetadata): String {
            return json.encodeToString(serializer(), metadata)
        }
        
        /**
         * Deserialize metadata from JSON string.
         */
        fun fromJson(jsonStr: String): DocumentRichMetadata? {
            return try {
                json.decodeFromString(serializer(), jsonStr)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Create from file path with minimal info.
         */
        fun fromPath(path: String): DocumentRichMetadata {
            val fileName = path.substringAfterLast('/')
            val extension = fileName.substringAfterLast('.', "")
            val directory = path.substringBeforeLast('/', "")
            val formatType = when (extension.lowercase()) {
                "md", "markdown" -> "markdown"
                "kt" -> "kotlin"
                "java" -> "java"
                "py" -> "python"
                "ts" -> "typescript"
                "js" -> "javascript"
                "go" -> "go"
                "rs" -> "rust"
                "cs" -> "csharp"
                "pdf" -> "pdf"
                "docx" -> "docx"
                else -> "text"
            }
            
            return DocumentRichMetadata(
                path = path,
                fileName = fileName,
                extension = extension,
                directory = directory,
                formatType = formatType
            )
        }
    }
    
    /**
     * Convert to DocumentMetadataItem for reranking.
     */
    fun toMetadataItem(
        contentType: String = "document",
        name: String = fileName,
        preview: String = "",
        heuristicScore: Double = 0.0
    ): DocumentMetadataItem {
        return DocumentMetadataItem(
            id = path,
            filePath = path,
            fileName = fileName,
            extension = extension,
            directory = directory,
            contentType = contentType,
            name = name,
            preview = preview,
            h1Heading = h1Heading,
            parentHeading = headings.firstOrNull()?.title,
            lastModified = lastModified,
            fileSize = fileSize,
            formatType = formatType,
            references = outgoingRefs,
            tags = keywords,
            heuristicScore = heuristicScore
        )
    }
}

/**
 * Heading information extracted from document.
 */
@Serializable
data class HeadingInfo(
    /** Heading level (1 = H1, 2 = H2, etc.) */
    val level: Int,
    /** Heading text */
    val title: String,
    /** Anchor/ID for navigation */
    val anchor: String? = null,
    /** Line number in document */
    val lineNumber: Int? = null
)

/**
 * Extracts rich metadata from documents.
 */
object DocumentRichMetadataExtractor {
    
    /**
     * Extract metadata from markdown content.
     */
    fun extractFromMarkdown(
        path: String,
        content: String,
        lastModified: Long = 0
    ): DocumentRichMetadata {
        val base = DocumentRichMetadata.fromPath(path)
        
        val lines = content.lines()
        val headings = mutableListOf<HeadingInfo>()
        var codeBlockCount = 0
        var linkCount = 0
        var inCodeBlock = false
        
        lines.forEachIndexed { index, line ->
            // Track code blocks
            if (line.trim().startsWith("```")) {
                if (!inCodeBlock) {
                    codeBlockCount++
                }
                inCodeBlock = !inCodeBlock
            }
            
            // Extract headings (outside code blocks)
            if (!inCodeBlock && line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length
                val title = line.drop(level).trim()
                if (title.isNotEmpty()) {
                    val anchor = title.lowercase()
                        .replace(Regex("[^a-z0-9\\s-]"), "")
                        .replace(Regex("\\s+"), "-")
                    headings.add(HeadingInfo(level, title, anchor, index + 1))
                }
            }
            
            // Count links
            linkCount += Regex("\\[.*?\\]\\(.*?\\)").findAll(line).count()
        }
        
        // Extract keywords from headings and content
        val keywords = extractKeywords(headings.map { it.title }, content)
        
        return base.copy(
            h1Heading = headings.firstOrNull { it.level == 1 }?.title,
            headings = headings,
            headingCount = headings.size,
            contentLength = content.length,
            lineCount = lines.size,
            codeBlockCount = codeBlockCount,
            linkCount = linkCount,
            lastModified = lastModified,
            fileSize = content.length.toLong(),
            extractedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            keywords = keywords,
            outgoingRefs = extractOutgoingRefs(content)
        )
    }
    
    /**
     * Extract metadata from source code.
     */
    fun extractFromCode(
        path: String,
        content: String,
        classes: List<String> = emptyList(),
        functions: List<String> = emptyList(),
        lastModified: Long = 0
    ): DocumentRichMetadata {
        val base = DocumentRichMetadata.fromPath(path)
        val lines = content.lines()
        
        // Extract package/module as primary heading for code files
        val packageLine = lines.firstOrNull { 
            it.trim().startsWith("package ") || it.trim().startsWith("module ")
        }
        val packageName = packageLine?.substringAfter("package ")
            ?.substringAfter("module ")
            ?.trim()
            ?.removeSuffix(";")
        
        return base.copy(
            h1Heading = packageName,
            contentLength = content.length,
            lineCount = lines.size,
            classes = classes,
            functions = functions,
            lastModified = lastModified,
            fileSize = content.length.toLong(),
            extractedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            keywords = (classes + functions).take(20)
        )
    }
    
    /**
     * Extract keywords from document.
     */
    private fun extractKeywords(headings: List<String>, content: String): List<String> {
        val keywords = mutableSetOf<String>()
        
        // Add heading words (excluding common words)
        val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by")
        headings.forEach { heading ->
            heading.split(Regex("[\\s,.-]+"))
                .filter { it.length > 2 && it.lowercase() !in stopWords }
                .forEach { keywords.add(it) }
        }
        
        // Look for code-like identifiers (CamelCase, snake_case)
        val identifierPattern = Regex("\\b([A-Z][a-z]+(?:[A-Z][a-z]+)+|[a-z]+_[a-z]+(?:_[a-z]+)*)\\b")
        identifierPattern.findAll(content).take(50).forEach { match ->
            keywords.add(match.value)
        }
        
        return keywords.take(30).toList()
    }
    
    /**
     * Extract outgoing references from content.
     */
    private fun extractOutgoingRefs(content: String): List<String> {
        val refs = mutableListOf<String>()
        
        // Markdown links: [text](path)
        val linkPattern = Regex("\\[.*?\\]\\((.*?)\\)")
        linkPattern.findAll(content).forEach { match ->
            val href = match.groupValues[1]
            if (href.isNotBlank() && !href.startsWith("http") && !href.startsWith("#")) {
                refs.add(href.substringBefore("#").substringBefore("?"))
            }
        }
        
        // Import statements
        val importPattern = Regex("import\\s+[\"']?(.*?)[\"']?[;\\s]")
        importPattern.findAll(content).forEach { match ->
            refs.add(match.groupValues[1])
        }
        
        return refs.distinct().take(20)
    }
}

