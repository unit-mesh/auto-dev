package cc.unitmesh.agent.tool.tracking

import kotlinx.datetime.Clock

actual fun getCurrentTimestamp(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

