package cc.unitmesh.codegraph

import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.jvm.JvmCodeParser

/**
 * JVM implementation of CodeGraphFactory
 */
actual object CodeGraphFactory {
    actual fun createParser(): CodeParser {
        return JvmCodeParser()
    }
}

