package cc.unitmesh.agent

actual object Platform {
    actual val name: String = "JavaScript"
    actual val isJvm: Boolean = false
    actual val isJs: Boolean = true
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = false

    actual fun getOSName(): String {
        return js("typeof process !== 'undefined' ? process.platform : 'Browser'") as String
    }

    actual fun getDefaultShell(): String {
        val platform = js("typeof process !== 'undefined' ? process.platform : 'unknown'") as String
        return when (platform) {
            "win32" -> "cmd"
            else -> "bash"
        }
    }
}
