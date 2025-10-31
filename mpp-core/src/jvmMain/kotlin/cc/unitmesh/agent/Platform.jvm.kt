package cc.unitmesh.agent

actual object Platform {
    actual val name: String = "JVM"
    actual val isJvm: Boolean = true
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = false
}
