package cc.unitmesh.devins.ui.compose.agent.codereview

import java.text.SimpleDateFormat
import java.util.Date

actual fun formatDate(timestamp: Long): String {
    val seconds = timestamp
    val date = Date(seconds * 1000)
    val formatter = SimpleDateFormat("MMM dd, HH:mm")
    return formatter.format(date)
}
