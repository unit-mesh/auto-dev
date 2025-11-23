package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * WASM platform-specific initialization
 * Currently only Markdown is supported on WASM platform
 */
actual fun platformInitialize() {
    logger.info { "Initializing WASM document parsers (Markdown only)" }
    // WASM platform only supports Markdown for now
    // Markdown parser is already registered in DocumentRegistry init block
}

