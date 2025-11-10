package cc.unitmesh.agent.config

import kotlinx.datetime.Clock

actual fun getCurrentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

