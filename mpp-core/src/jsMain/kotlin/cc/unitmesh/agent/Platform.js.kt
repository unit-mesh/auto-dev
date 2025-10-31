package cc.unitmesh.agent

actual object Platform {
    actual val name: String = "JavaScript"
    actual val isJvm: Boolean = false
    actual val isJs: Boolean = true
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = false
}
