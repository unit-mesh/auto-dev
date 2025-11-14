package cc.unitmesh.agent

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * JS Exports for Platform API
 * Provides access to platform-specific information and utilities
 */

@JsExport
@JsName("JsPlatform")
object JsPlatform {
    val name: String get() = Platform.name
    val isJvm: Boolean get() = Platform.isJvm
    val isJs: Boolean get() = Platform.isJs
    val isWasm: Boolean get() = Platform.isWasm
    val isAndroid: Boolean get() = Platform.isAndroid
    val isIOS: Boolean get() = Platform.isIOS
    
    fun getOSName(): String = Platform.getOSName()
    fun getDefaultShell(): String = Platform.getDefaultShell()
    fun getCurrentTimestamp(): String = Platform.getCurrentTimestamp().toString()
    fun getOSInfo(): String = Platform.getOSInfo()
    fun getOSVersion(): String = Platform.getOSVersion()
    fun getUserHomeDir(): String = Platform.getUserHomeDir()
    fun getLogDir(): String = Platform.getLogDir()
}

/**
 * Get platform information as a simple string
 */
@JsExport
@JsName("getPlatformInfo")
fun jsPlatformInfo(): String {
    return getPlatformInfo()
}
