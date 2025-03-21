package cc.unitmesh.terminal.sketch

object ShellSyntaxSafetyCheck {

    /**
     * Check if shell command contains dangerous operations
     * @return Pair<Boolean, String> - first: is dangerous, second: reason message
     */
    fun checkDangerousCommand(command: String): Pair<Boolean, String> {
        val dangerousPatterns = mapOf(
            "\\brm\\s+(-[a-zA-Z]*f|-[a-zA-Z]*r|-[a-zA-Z]*(rf|fr))\\b.*".toRegex() to "Dangerous rm command with recursive or force flags",
            "\\brm\\s+-[a-zA-Z]*\\s+/\\b.*".toRegex() to "Removing files from root directory",
            "\\brmdir\\s+/\\b.*".toRegex() to "Removing directories from root",
            "\\bmkfs\\b.*".toRegex() to "Filesystem formatting command",
            "\\bdd\\b.*".toRegex() to "Low-level disk operation",
            "\\b:[(][)][{]\\s*:|:&\\s*[}];:.*".toRegex() to "Potential fork bomb",
            "\\bchmod\\s+-[a-zA-Z]*R\\b.*777\\b.*".toRegex() to "Recursive chmod with insecure permissions",
            "\\bsudo\\s+rm\\b.*".toRegex() to "Removing files with elevated privileges",
        )

        // Also catch simpler rm commands (without flags but still potentially dangerous)
        if (command.trim().startsWith("rm ") && !command.contains("-i") && !command.contains("--interactive")) {
            return Pair(true, "Remove command detected, use with caution")
        }

        for ((pattern, message) in dangerousPatterns) {
            if (pattern.containsMatchIn(command)) {
                return Pair(true, message)
            }
        }

        return Pair(false, "")
    }
}