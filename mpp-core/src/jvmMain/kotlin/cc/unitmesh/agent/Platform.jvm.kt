package cc.unitmesh.agent

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

actual object Platform {
    actual val name: String = "JVM"
    actual val isJvm: Boolean = true
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = false

    actual val isIOS: Boolean = false

    actual fun getOSName(): String {
        return System.getProperty("os.name", "Unknown")
    }

    actual fun getDefaultShell(): String {
        val osName = System.getProperty("os.name", "")
        return when {
            osName.contains("Windows", ignoreCase = true) -> "cmd.exe"
            osName.contains("Mac", ignoreCase = true) -> "/bin/zsh"
            else -> "/bin/bash"
        }
    }

    actual fun getCurrentTimestamp(): String {
        val now = ZonedDateTime.now()
        return now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    actual fun getOSInfo(): String {
        val osName = System.getProperty("os.name", "Unknown")
        val osVersion = System.getProperty("os.version", "")
        val osArch = System.getProperty("os.arch", "")
        return "$osName $osVersion ($osArch)"
    }

    actual fun getOSVersion(): String {
        return System.getProperty("os.version", "Unknown")
    }

    actual fun getUserHomeDir(): String {
        return System.getProperty("user.home", "~")
    }

    actual fun getLogDir(): String {
        return "${getUserHomeDir()}/.autodev/logs"
    }
}
