package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * JVM platform-specific initialization
 * Automatically registers Tika parser for various document formats
 */
actual fun platformInitialize() {
    logger.info { "Initializing JVM document parsers (Tika)" }
    
    // Register Tika parser for multiple formats
    val tikaFormats = listOf(
        DocumentFormatType.PDF,
        DocumentFormatType.DOCX,
        DocumentFormatType.PLAIN_TEXT
    )
    
    tikaFormats.forEach { format ->
        DocumentParserFactory.registerParser(format) { TikaDocumentParser() }
        logger.debug { "Registered TikaDocumentParser for $format" }
    }
    
    // Register Jsoup parser for HTML
    DocumentParserFactory.registerParser(DocumentFormatType.HTML) { JsoupDocumentParser() }
    logger.debug { "Registered JsoupDocumentParser for HTML" }
    
    logger.info { "JVM parsers initialized: ${tikaFormats.size + 1} formats supported (Tika + Jsoup)" }
}

