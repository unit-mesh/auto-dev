package cc.unitmesh.agent

actual object Platform {
    actual val name: String = "WebAssembly"
    actual val isJvm: Boolean = false
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = true
    actual val isAndroid: Boolean = false

    actual fun getOSName(): String {
        return "WebAssembly"
    }

    actual fun getDefaultShell(): String {
        return "bash"  // Default to bash for WASM
    }
}
