package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * JS platform-specific initialization
 * Currently only Markdown is supported on JS platform
 */
actual fun platformInitialize() {
    logger.info { "Initializing JS document parsers (Markdown only)" }
    // JS platform only supports Markdown for now
    // Markdown parser is already registered in DocumentRegistry init block
}

