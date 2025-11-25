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
        DocumentFormatType.DOCX,
        DocumentFormatType.PLAIN_TEXT
    )
    
    tikaFormats.forEach { format ->
        DocumentParserFactory.registerParser(format) { TikaDocumentParser() }
        logger.debug { "Registered TikaDocumentParser for $format" }
    }

    // Register PDFBox parser for PDF
    DocumentParserFactory.registerParser(DocumentFormatType.PDF) { PdfDocumentParser() }
    logger.debug { "Registered PdfDocumentParser for PDF" }
    
    // Register Jsoup parser for HTML
    DocumentParserFactory.registerParser(DocumentFormatType.HTML) { JsoupDocumentParser() }
    logger.debug { "Registered JsoupDocumentParser for HTML" }
    
    // Register CodeDocumentParser for source code
    DocumentParserFactory.registerParser(DocumentFormatType.SOURCE_CODE) { CodeDocumentParser() }
    logger.debug { "Registered CodeDocumentParser for SOURCE_CODE" }
    
    logger.info { "JVM parsers initialized: ${tikaFormats.size + 3} formats supported (Tika + Jsoup + Code)" }
}

