package cc.unitmesh.agent

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

actual object Platform {
    actual val name: String = "Android ${android.os.Build.VERSION.RELEASE}"
    actual val isJvm: Boolean = true  // Android uses JVM
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = true

    actual val isIOS: Boolean = false

    actual fun getOSName(): String {
        return "Android"
    }

    actual fun getDefaultShell(): String {
        return "/system/bin/sh"
    }

    actual fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    actual fun getOSInfo(): String {
        val osVersion = android.os.Build.VERSION.RELEASE
        val sdkVersion = android.os.Build.VERSION.SDK_INT
        val device = android.os.Build.DEVICE
        val model = android.os.Build.MODEL
        val manufacturer = android.os.Build.MANUFACTURER
        return "Android $osVersion (API $sdkVersion) - $manufacturer $model ($device)"
    }

    actual fun getOSVersion(): String {
        return "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
    }

    actual fun getUserHomeDir(): String {
        // Android doesn't have a traditional home directory, use app data directory
        return "/data/data"
    }

    actual fun getLogDir(): String {
        return "${getUserHomeDir()}/.autodev/logs"
    }
}

