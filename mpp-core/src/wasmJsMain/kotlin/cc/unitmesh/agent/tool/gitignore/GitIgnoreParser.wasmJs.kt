package cc.unitmesh.agent.tool.gitignore

/**
 * WebAssembly platform GitIgnore parser implementation (Stub)
 * WASM cannot access file system directly, so this is a minimal stub
 */
actual class GitIgnoreParser actual constructor(private val projectRoot: String) {
    
    actual fun isIgnored(filePath: String): Boolean {
        // WASM stub: Always return false
        return false
    }
    
    actual fun reload() {
        // WASM stub: No-op
    }
    
    actual fun getPatterns(): List<String> {
        // WASM stub: Return empty list
        return emptyList()
    }
}
