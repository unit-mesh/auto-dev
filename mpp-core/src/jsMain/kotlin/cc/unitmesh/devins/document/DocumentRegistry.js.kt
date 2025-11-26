package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * JS platform-specific initialization
 * Supports Markdown and Source Code parsing on JS platform
 */
actual fun platformInitialize() {
    logger.debug { "Initializing JS document parsers (Markdown + Source Code)" }
    // Markdown parser is already registered in DocumentRegistry init block
    
    // Register Source Code parser for JS platform
    DocumentParserFactory.registerParser(DocumentFormatType.SOURCE_CODE) { CodeDocumentParser() }
    logger.debug { "Registered SOURCE_CODE parser for JS platform" }
}

