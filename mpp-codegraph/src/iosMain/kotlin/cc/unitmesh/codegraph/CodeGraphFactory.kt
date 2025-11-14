package cc.unitmesh.codegraph

import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.ios.IosCodeParser

/**
 * iOS implementation of CodeGraphFactory
 */
actual object CodeGraphFactory {
    actual fun createParser(): CodeParser {
        return IosCodeParser()
    }
}

