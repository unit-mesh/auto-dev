package cc.unitmesh.agent.tool.shell

/**
 * Utility object for stripping ANSI escape sequences from terminal output.
 * This converts raw terminal output with color codes, cursor movements, etc.
 * into clean, readable ASCII text.
 */
object AnsiStripper {
    private const val ESC = '\u001B'
    
    /**
     * Strip all ANSI escape sequences from the given text.
     * Handles:
     * - CSI sequences (ESC[...X) - colors, cursor movement, erase
     * - OSC sequences (ESC]...BEL/ST) - window title, etc.
     * - Simple escape sequences (ESC X)
     * 
     * @param text The text containing ANSI escape sequences
     * @return Clean text with all escape sequences removed
     */
    fun strip(text: String): String {
        if (!text.contains(ESC)) {
            return text
        }
        
        val result = StringBuilder()
        var i = 0
        
        while (i < text.length) {
            val ch = text[i]
            
            when {
                ch == ESC && i + 1 < text.length -> {
                    val next = text[i + 1]
                    when (next) {
                        '[' -> {
                            // CSI sequence: ESC[...X (ends with a letter)
                            i = skipCsiSequence(text, i + 2)
                        }
                        ']' -> {
                            // OSC sequence: ESC]...BEL or ESC]...ST
                            i = skipOscSequence(text, i + 2)
                        }
                        '(' , ')' -> {
                            // Character set selection: ESC(X or ESC)X
                            i = if (i + 2 < text.length) i + 3 else text.length
                        }
                        else -> {
                            // Simple escape sequence: ESC X
                            i += 2
                        }
                    }
                }
                ch == '\r' -> {
                    // Carriage return - skip it (will be handled with newlines)
                    i++
                }
                else -> {
                    result.append(ch)
                    i++
                }
            }
        }
        
        return result.toString()
    }
    
    /**
     * Skip a CSI sequence starting at the given position.
     * CSI sequences end with a letter (0x40-0x7E).
     */
    private fun skipCsiSequence(text: String, start: Int): Int {
        var i = start
        while (i < text.length) {
            val ch = text[i]
            if (ch in '@'..'~') {
                // Found the terminating character
                return i + 1
            }
            i++
        }
        return text.length
    }
    
    /**
     * Skip an OSC sequence starting at the given position.
     * OSC sequences end with BEL (0x07) or ST (ESC\).
     */
    private fun skipOscSequence(text: String, start: Int): Int {
        var i = start
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '\u0007' -> {
                    // BEL character terminates OSC
                    return i + 1
                }
                ch == ESC && i + 1 < text.length && text[i + 1] == '\\' -> {
                    // ST (String Terminator) terminates OSC
                    return i + 2
                }
            }
            i++
        }
        return text.length
    }
    
    /**
     * Strip ANSI sequences and also normalize line endings.
     * Converts \r\n to \n and removes standalone \r.
     */
    fun stripAndNormalize(text: String): String {
        return strip(text)
            .replace("\r\n", "\n")
            .replace("\r", "")
    }
}

