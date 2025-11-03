package cc.unitmesh.agent

import kotlin.js.Date

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
        return "/bin/bash"  // Default to bash for WASM
    }

    actual fun getCurrentTimestamp(): String {
        val date = Date()
        return date.toISOString()
    }

    actual fun getOSInfo(): String {
        // In WASM environment, try to get browser info
        return try {
            val userAgent = js("navigator.userAgent || 'Unknown Browser'") as String
            "WebAssembly in Browser: $userAgent"
        } catch (e: Exception) {
            "WebAssembly Runtime"
        }
    }

    actual fun getOSVersion(): String {
        return try {
            js("navigator.appVersion || 'Unknown'") as String
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
