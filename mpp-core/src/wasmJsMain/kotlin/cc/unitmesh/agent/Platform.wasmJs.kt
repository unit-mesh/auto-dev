package cc.unitmesh.agent

import kotlinx.datetime.Clock

actual object Platform {
    actual val name: String = "WebAssembly"
    actual val isJvm: Boolean = false
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = true
    actual val isAndroid: Boolean = false
    actual val isIOS: Boolean = false

    actual fun getOSName(): String {
        return "WebAssembly"
    }

    actual fun getDefaultShell(): String {
        return "/bin/bash"  // Default to bash for WASM
    }

    actual fun getCurrentTimestamp(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }

    actual fun getOSInfo(): String {
        // In WASM environment, we can't reliably access browser info
        // Return a generic description
        return "WebAssembly Runtime"
    }

    actual fun getOSVersion(): String {
        return "Unknown"
    }

    actual fun getUserHomeDir(): String {
        // WASM runs in browser or minimal runtime, no concept of home directory
        return "~"
    }

    actual fun getLogDir(): String {
        // WASM typically runs in browser, use a virtual path
        return "~/.autodev/logs"
    }
}
