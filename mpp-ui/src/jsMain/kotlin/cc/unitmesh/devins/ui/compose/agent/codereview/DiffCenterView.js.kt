package cc.unitmesh.devins.ui.compose.agent.codereview

import kotlin.js.Date

actual fun formatDate(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    return date.toLocaleString()
}

