package cc.unitmesh.agent

actual object Platform {
    actual val name: String = "JVM"
    actual val isJvm: Boolean = true
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = false

    actual fun getOSName(): String {
        return System.getProperty("os.name", "Unknown")
    }

    actual fun getDefaultShell(): String {
        val osName = System.getProperty("os.name", "")
        return if (osName.contains("Windows", ignoreCase = true)) "cmd" else "bash"
    }
}
