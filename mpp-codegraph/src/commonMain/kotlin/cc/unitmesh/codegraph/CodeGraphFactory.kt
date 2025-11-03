package cc.unitmesh.codegraph

import cc.unitmesh.codegraph.parser.CodeParser

/**
 * Factory to create platform-specific CodeParser instances.
 * This uses expect/actual pattern for platform-specific implementations.
 */
expect object CodeGraphFactory {
    /**
     * Create a platform-specific CodeParser instance
     */
    fun createParser(): CodeParser
}

