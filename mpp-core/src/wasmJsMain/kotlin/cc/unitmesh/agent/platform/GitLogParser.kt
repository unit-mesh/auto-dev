package cc.unitmesh.agent.platform

import cc.unitmesh.devins.workspace.GitCommitInfo

/**
 * Parser for classic git log output format
 * 
 * Handles parsing of git log output in the standard format:
 * ```
 * commit <hash> (<refs>)
 * Author: <name> <email>
 * Date:   <date>
 * 
 *     <commit message>
 * ```
 */
object GitLogParser {
    private val COMMIT_PATTERN = Regex("""^commit\s+([a-f0-9]+)(?:\s+\((.+?)\))?$""")
    private val AUTHOR_PATTERN = Regex("""^Author:\s+(.+?)\s+<(.+?)>$""")
    private val DATE_PATTERN = Regex("""^Date:\s+(.+)$""")
    
    /**
     * Parse git log output into a list of GitCommitInfo
     * 
     * @param logOutput The raw output from `git log` command
     * @return List of parsed commits
     */
    fun parse(logOutput: String): List<GitCommitInfo> {
        if (logOutput.isBlank()) return emptyList()
        
        val commits = mutableListOf<GitCommitInfo>()
        val lines = logOutput.lines()
        
        var currentHash: String? = null
        var currentAuthor: String? = null
        var currentEmail: String? = null
        var currentDate: Long? = null
        val currentMessage = StringBuilder()
        var inMessage = false
        
        fun commitCurrent() {
            if (currentHash != null && currentAuthor != null) {
                commits.add(
                    GitCommitInfo(
                        hash = currentHash!!,
                        author = currentAuthor!!,
                        email = currentEmail ?: "",
                        date = currentDate ?: 0L,
                        message = currentMessage.toString().trim()
                    )
                )
            }
            // Reset state
            currentHash = null
            currentAuthor = null
            currentEmail = null
            currentDate = null
            currentMessage.clear()
            inMessage = false
        }
        
        for (line in lines) {
            when {
                // New commit starts
                line.startsWith("commit ") -> {
                    // Save previous commit if exists
                    commitCurrent()
                    
                    // Parse commit hash and refs
                    val match = COMMIT_PATTERN.matchEntire(line)
                    if (match != null) {
                        currentHash = match.groupValues[1]
                    }
                }
                
                // Author line
                line.startsWith("Author: ") -> {
                    val match = AUTHOR_PATTERN.matchEntire(line)
                    if (match != null) {
                        currentAuthor = match.groupValues[1]
                        currentEmail = match.groupValues[2]
                    }
                }
                
                // Date line
                line.startsWith("Date: ") -> {
                    val match = DATE_PATTERN.matchEntire(line)
                    if (match != null) {
                        currentDate = parseGitDate(match.groupValues[1])
                    }
                    inMessage = true
                }
                
                // Empty line (separates header from message or between commits)
                line.isBlank() -> {
                    // Continue, could be separator before message or between commits
                }
                
                // Commit message (indented with spaces)
                inMessage && line.isNotEmpty() -> {
                    // Message lines are typically indented with 4 spaces
                    val messageLine = if (line.startsWith("    ")) {
                        line.substring(4)
                    } else {
                        line.trimStart()
                    }
                    
                    if (currentMessage.isNotEmpty()) {
                        currentMessage.append("\n")
                    }
                    currentMessage.append(messageLine)
                }
            }
        }
        
        // Don't forget the last commit
        commitCurrent()
        
        return commits
    }
    
    /**
     * Parse git date string to Unix timestamp (seconds since epoch)
     * 
     * Git date format examples:
     * - "Tue Nov 18 15:33:49 2025 +0800"
     * - "Tue Nov 18 15:33:49 2025 -0500"
     */
    private fun parseGitDate(dateStr: String): Long {
        return try {
            // For WASM, we'll use a simple approximation
            // Git date format: "Tue Nov 18 15:33:49 2025 +0800"
            // We need to extract the components and convert to Unix timestamp
            
            val parts = dateStr.trim().split(Regex("\\s+"))
            if (parts.size < 5) return 0L
            
            // Parts: [weekday, month, day, time, year, timezone?]
            val monthStr = parts[1]
            val day = parts[2].toIntOrNull() ?: return 0L
            val timeParts = parts[3].split(":")
            val year = parts[4].toIntOrNull() ?: return 0L
            
            if (timeParts.size != 3) return 0L
            
            val hour = timeParts[0].toIntOrNull() ?: return 0L
            val minute = timeParts[1].toIntOrNull() ?: return 0L
            val second = timeParts[2].toIntOrNull() ?: return 0L
            
            val month = parseMonth(monthStr)
            
            // Calculate Unix timestamp
            val timestamp = calculateTimestamp(year, month, day, hour, minute, second)
            
            // Adjust for timezone if present
            if (parts.size > 5) {
                val timezone = parts[5]
                val tzOffset = parseTimezoneOffset(timezone)
                return timestamp - tzOffset
            }
            
            timestamp
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun parseMonth(monthStr: String): Int {
        return when (monthStr) {
            "Jan" -> 1
            "Feb" -> 2
            "Mar" -> 3
            "Apr" -> 4
            "May" -> 5
            "Jun" -> 6
            "Jul" -> 7
            "Aug" -> 8
            "Sep" -> 9
            "Oct" -> 10
            "Nov" -> 11
            "Dec" -> 12
            else -> 1
        }
    }
    
    private fun parseTimezoneOffset(timezone: String): Int {
        // Format: +0800 or -0500
        if (timezone.length != 5) return 0
        
        val sign = if (timezone[0] == '+') 1 else -1
        val hours = timezone.substring(1, 3).toIntOrNull() ?: return 0
        val minutes = timezone.substring(3, 5).toIntOrNull() ?: return 0
        
        return sign * (hours * 3600 + minutes * 60)
    }
    
    private fun calculateTimestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        // Days since epoch (1970-01-01)
        var days = 0L
        
        // Add days for complete years
        for (y in 1970 until year) {
            days += if (isLeapYear(y)) 366 else 365
        }
        
        // Add days for complete months in current year
        for (m in 1 until month) {
            days += daysInMonth(m, year)
        }
        
        // Add remaining days
        days += day - 1
        
        // Convert to seconds
        return days * 86400L + hour * 3600L + minute * 60L + second
    }
    
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
    
    private fun daysInMonth(month: Int, year: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 0
        }
    }
}

