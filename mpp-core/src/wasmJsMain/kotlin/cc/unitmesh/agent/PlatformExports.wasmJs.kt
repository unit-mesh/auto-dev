package cc.unitmesh.agent

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * WASM Exports for Platform API
 * Provides access to platform-specific information and utilities
 * 
 * Note: In WASM, @JsExport only works with top-level functions and classes (not objects)
 */

@JsExport
@JsName("wasmGetPlatformName")
fun wasmGetPlatformName(): String = Platform.name

@JsExport
@JsName("wasmIsJvm")
fun wasmIsJvm(): Boolean = Platform.isJvm

@JsExport
@JsName("wasmIsJs")
fun wasmIsJs(): Boolean = Platform.isJs

@JsExport
@JsName("wasmIsWasm")
fun wasmIsWasm(): Boolean = Platform.isWasm

@JsExport
@JsName("wasmIsAndroid")
fun wasmIsAndroid(): Boolean = Platform.isAndroid

@JsExport
@JsName("wasmIsIOS")
fun wasmIsIOS(): Boolean = Platform.isIOS

@JsExport
@JsName("wasmGetOSName")
fun wasmGetOSName(): String = Platform.getOSName()

@JsExport
@JsName("wasmGetDefaultShell")
fun wasmGetDefaultShell(): String = Platform.getDefaultShell()

@JsExport
@JsName("wasmGetCurrentTimestamp")
fun wasmGetCurrentTimestamp(): String = Platform.getCurrentTimestamp()

@JsExport
@JsName("wasmGetOSInfo")
fun wasmGetOSInfo(): String = Platform.getOSInfo()

@JsExport
@JsName("wasmGetOSVersion")
fun wasmGetOSVersion(): String = Platform.getOSVersion()

@JsExport
@JsName("wasmGetUserHomeDir")
fun wasmGetUserHomeDir(): String = Platform.getUserHomeDir()

@JsExport
@JsName("wasmGetLogDir")
fun wasmGetLogDir(): String = Platform.getLogDir()

/**
 * Get platform information as a simple string
 */
@JsExport
@JsName("getWasmPlatformInfo")
fun wasmPlatformInfo(): String {
    return getPlatformInfo()
}
