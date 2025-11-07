package cc.unitmesh.agent.tool.tracking

import kotlin.js.Date

actual fun getCurrentTimestamp(): Long = Date.now().toLong()

