package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * iOS platform-specific initialization
 * Currently only Markdown is supported on iOS platform
 * TODO: Consider adding iOS-specific document parsers
 */
actual fun platformInitialize() {
    logger.info { "Initializing iOS document parsers (Markdown only)" }
    // iOS platform only supports Markdown for now
    // Markdown parser is already registered in DocumentRegistry init block
}

