package cc.unitmesh.agent.tool.impl

import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.jvm.JvmCodeParser

/**
 * JVM implementation of CodeParser factory
 */
actual fun createCodeParser(): CodeParser {
    return JvmCodeParser()
}
