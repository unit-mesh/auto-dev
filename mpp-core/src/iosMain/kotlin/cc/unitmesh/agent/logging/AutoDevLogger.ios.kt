package cc.unitmesh.agent.logging

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSHomeDirectory

@OptIn(ExperimentalForeignApi::class)
actual fun getPlatformLogDirectory(): String {
    return "${NSHomeDirectory()}/.autodev/logs"
}

