package cc.unitmesh.agent.tool.impl

import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.wasm.WasmJsCodeParser

/**
 * WebAssembly-JS implementation of CodeParser factory
 */
actual fun createCodeParser(): CodeParser {
    return WasmJsCodeParser()
}
