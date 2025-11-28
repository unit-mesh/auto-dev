package cc.unitmesh.agent.tool.impl

import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.ios.IosCodeParser

/**
 * Android implementation of CodeParser factory
 * Uses the same JVM-based implementation as regular JVM
 */
actual fun createCodeParser(): CodeParser {
    // Android uses JVM backend, but IosCodeParser is a fallback
    // In practice, we should use JvmCodeParser but it's not accessible from androidMain
    // For now, use the simplified iOS implementation
    return IosCodeParser()
}
