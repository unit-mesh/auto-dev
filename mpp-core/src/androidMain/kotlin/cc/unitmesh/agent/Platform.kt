package cc.unitmesh.agent

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

actual object Platform {
    actual val name: String = "Android ${android.os.Build.VERSION.RELEASE}"
    actual val isJvm: Boolean = true  // Android uses JVM
    actual val isJs: Boolean = false
    actual val isWasm: Boolean = false
    actual val isAndroid: Boolean = true

    actual fun getOSName(): String {
        return "Android"
    }

    actual fun getDefaultShell(): String {
        return "/system/bin/sh"  // Android uses sh shell
    }

    actual fun getCurrentTimestamp(): String {
        val now = ZonedDateTime.now()
        return now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
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
}

