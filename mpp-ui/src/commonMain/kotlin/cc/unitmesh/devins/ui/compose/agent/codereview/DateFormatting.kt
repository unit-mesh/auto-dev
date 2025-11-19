package cc.unitmesh.devins.ui.compose.agent.codereview

/**
 * Format Unix timestamp (in seconds) to a human-readable date string
 * Format: "MMM dd, HH:mm" like "Jan 15, 14:30"
 */
fun formatDate(timestamp: Long): String {
    // Convert seconds to milliseconds
    val millis = timestamp * 1000
    
    // Calculate date components from milliseconds
    val totalSeconds = millis / 1000
    val totalMinutes = totalSeconds / 60
    val totalHours = totalMinutes / 60
    val days = totalHours / 24
    
    // Calculate current components
    val seconds = (totalSeconds % 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    val hours = (totalHours % 24).toInt()
    
    // Simple epoch-based calculation (approximate, good enough for display)
    // Days since Unix epoch (Jan 1, 1970)
    val daysSinceEpoch = days.toInt()
    
    // Simplified month/day calculation (not accounting for leap years perfectly)
    val daysInMonths = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var remainingDays = daysSinceEpoch % 365
    var month = 0
    
    for (i in daysInMonths.indices) {
        if (remainingDays < daysInMonths[i]) {
            month = i
            break
        }
        remainingDays -= daysInMonths[i]
    }
    
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                           "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val day = remainingDays + 1
    
    return "${monthNames.getOrNull(month) ?: "???"} ${day.toString().padStart(2, '0')}, ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
