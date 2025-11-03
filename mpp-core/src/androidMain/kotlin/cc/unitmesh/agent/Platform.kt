package cc.unitmesh.agent

actual object Platform {
    actual val name: String = "Android ${android.os.Build.VERSION.RELEASE}"
    actual val isJvm: Boolean = true  // Android uses JVM
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = true

    actual fun getOSName(): String {
        return "Android ${android.os.Build.VERSION.RELEASE}"
    }

    actual fun getDefaultShell(): String {
        return "sh"  // Android uses sh shell
    }
}

