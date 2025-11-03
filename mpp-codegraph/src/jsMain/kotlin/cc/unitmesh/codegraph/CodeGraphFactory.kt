package cc.unitmesh.codegraph

import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.js.JsCodeParser

/**
 * JS implementation of CodeGraphFactory
 */
actual object CodeGraphFactory {
    actual fun createParser(): CodeParser {
        return JsCodeParser()
    }
}

