package cc.unitmesh.agent

/**
 * Platform-specific functionality for different targets
 */
expect object Platform {
    val name: String
    val isJvm: Boolean
    val isJs: Boolean
    val isWasm: Boolean
    val isAndroid: Boolean
}

/**
 * Get current platform information
 */
fun getPlatformInfo(): String {
    return "Running on ${Platform.name}"
}
