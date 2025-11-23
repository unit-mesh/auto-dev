package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Android platform-specific initialization
 * Currently only Markdown is supported on Android platform
 * TODO: Consider adding Android-specific document parsers
 */
actual fun platformInitialize() {
    logger.info { "Initializing Android document parsers (Markdown only)" }
    // Android platform only supports Markdown for now
    // Markdown parser is already registered in DocumentRegistry init block
}

