package cc.unitmesh.devti.sketch.run

object ShellSafetyCheck {
    val dangerousPatterns = mapOf(
        "\\brm\\s+(-[a-zA-Z]*f|-[a-zA-Z]*r|-[a-zA-Z]*(rf|fr))\\b.*".toRegex() to "Dangerous rm command with recursive or force flags",
        "\\brm\\s+-[a-zA-Z]*\\s+/.*".toRegex() to "Removing files from root directory",
        "\\brmdir\\s+/.*".toRegex() to "Removing directories from root",
        "\\bmkfs\\b.*".toRegex() to "Filesystem formatting command",
        "\\bdd\\b.*".toRegex() to "Low-level disk operation",
        "\\b:[(][)][{]\\s*:|:&\\s*[}];:.*".toRegex() to "Potential fork bomb",
        "\\bchmod\\s+-[a-zA-Z]*R\\b.*777\\b.*".toRegex() to "Recursive chmod with insecure permissions",
        "\\bsudo\\s+rm\\b.*".toRegex() to "Removing files with elevated privileges",
        "\\bcurl\\s+.*\\s*\\|\\s*(ba)?sh.*".toRegex() to "Downloading and executing scripts directly",
        "\\bwget\\s+.*\\s*\\|\\s*(ba)?sh.*".toRegex() to "Downloading and executing scripts directly",
        "\\bkill\\s+-9\\s+-1\\b.*".toRegex() to "Killing all user processes",
        ">\\s+/etc/.*".toRegex() to "Overwriting system configuration files",
        "\\bformat\\b.*".toRegex() to "Disk formatting command",
        "\\bfdisk\\b.*".toRegex() to "Disk partitioning tool",
        "\\bshred\\b.*".toRegex() to "Secure file deletion tool",
        "\\bfsck\\s+/dev/.*".toRegex() to "Filesystem check on device",
        "\\buserdel\\s+(root|daemon|bin|sys|sync|games|man|lp|mail|news|uucp|proxy)\\b.*".toRegex() to "Removing critical system users",
        "\\bchown\\s+-[a-zA-Z]*R\\b.*".toRegex() to "Recursive ownership change",
        "\\bmv\\s+.*\\s+/etc/.*".toRegex() to "Moving files to system configuration directory",
        "\\bchattr\\s+-[a-zA-Z]*i\\b.*".toRegex() to "Changing immutable file attributes"
    )

    fun checkDangerousCommand(command: String): Pair<Boolean, String> {
        if (command.trim().startsWith("rm ")) {
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

