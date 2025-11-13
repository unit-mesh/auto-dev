package cc.unitmesh.codegraph

import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.wasm.WasmJsCodeParser

/**
 * WASM-JS implementation of CodeGraphFactory
 */
actual object CodeGraphFactory {
    actual fun createParser(): CodeParser {
        return WasmJsCodeParser()
    }
}

