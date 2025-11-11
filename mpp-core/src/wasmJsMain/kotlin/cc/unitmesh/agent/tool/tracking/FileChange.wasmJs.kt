package cc.unitmesh.agent.tool.tracking

import kotlinx.datetime.Clock

actual fun getCurrentTimestamp(): Long = Clock.System.now().toEpochMilliseconds()


