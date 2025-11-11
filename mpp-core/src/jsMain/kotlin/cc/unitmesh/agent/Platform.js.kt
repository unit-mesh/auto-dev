package cc.unitmesh.agent

import kotlin.js.Date

actual object Platform {
    actual val name: String = "JavaScript"
    actual val isJvm: Boolean = false
    actual val isJs: Boolean = true
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = false

    actual val isIOS: Boolean = false
    actual fun getOSName(): String {
        return js("typeof process !== 'undefined' ? process.platform : 'Browser'") as String
    }

    actual fun getDefaultShell(): String {
        val platform = js("typeof process !== 'undefined' ? process.platform : 'unknown'") as String
        return when (platform) {
            "win32" -> "cmd.exe"
            "darwin" -> "/bin/zsh"
            else -> "/bin/bash"
        }
    }

    actual fun getCurrentTimestamp(): String {
        val date = Date()
        return date.toISOString()
    }

    actual fun getOSInfo(): String {
        val isNode = js("typeof process !== 'undefined'") as Boolean
        return if (isNode) {
            val platform = js("process.platform") as String
            val arch = js("process.arch") as String
            val version = js("process.version") as String
            "Node.js $version on $platform ($arch)"
        } else {
            val userAgent = js("navigator.userAgent || 'Unknown Browser'") as String
            "Browser: $userAgent"
        }
    }

    actual fun getOSVersion(): String {
        val isNode = js("typeof process !== 'undefined'") as Boolean
        return if (isNode) {
            js("require('os').release()") as String
        } else {
            js("navigator.appVersion || 'Unknown'") as String
        }
    }

    actual fun getUserHomeDir(): String {
        val isNode = js("typeof process !== 'undefined'") as Boolean
        return if (isNode) {
            js("require('os').homedir()") as String
        } else {
            // In browser environment, use a default path
            "~"
        }
    }

    actual fun getLogDir(): String {
        return "${getUserHomeDir()}/.autodev/logs"
    }
}
