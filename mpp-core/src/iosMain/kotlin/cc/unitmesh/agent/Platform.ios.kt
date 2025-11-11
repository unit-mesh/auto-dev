package cc.unitmesh.agent

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Clock
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSProcessInfo

@OptIn(ExperimentalForeignApi::class)
actual object Platform {
    actual val name: String = "iOS"
    actual val isJvm: Boolean = false
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = false

    actual val isIOS: Boolean = true

    actual fun getOSName(): String {
        return NSProcessInfo.processInfo.operatingSystemVersionString
    }

    actual fun getDefaultShell(): String {
        return "/bin/sh"
    }

    actual fun getCurrentTimestamp(): String {
        return Clock.System.now().toString()
    }

    actual fun getOSInfo(): String {
        val processInfo = NSProcessInfo.processInfo
        return "iOS ${processInfo.operatingSystemVersionString}"
    }

    actual fun getOSVersion(): String {
        return NSProcessInfo.processInfo.operatingSystemVersionString
    }

    actual fun getUserHomeDir(): String {
        return NSHomeDirectory()
    }

    actual fun getLogDir(): String {
        return "${getUserHomeDir()}/.autodev/logs"
    }
}

