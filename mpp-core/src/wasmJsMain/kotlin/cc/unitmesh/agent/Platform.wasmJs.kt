package cc.unitmesh.agent

actual object Platform {
    actual val name: String = "WebAssembly"
    actual val isJvm: Boolean = false
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = true
    actual val isAndroid: Boolean = false
}
