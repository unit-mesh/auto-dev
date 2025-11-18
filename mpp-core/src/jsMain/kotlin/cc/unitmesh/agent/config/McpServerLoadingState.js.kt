package cc.unitmesh.agent.config

import kotlin.js.Date

actual fun getCurrentTimeMillis(): Long = Date.now().toLong()
