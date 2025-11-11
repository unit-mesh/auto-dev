package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.ui.graphics.Color

/**
 * ANSI escape sequence parser for terminal emulation.
 * Handles colors, cursor movement, text formatting, and screen manipulation.
 */
class AnsiParser {
    companion object {
        // ANSI color codes (standard 16 colors)
        val ANSI_COLORS = mapOf(
            // Normal colors
            30 to Color(0xFF000000), // Black
            31 to Color(0xFFCD3131), // Red
            32 to Color(0xFF0DBC79), // Green
            33 to Color(0xFFE5E510), // Yellow
            34 to Color(0xFF2472C8), // Blue
            35 to Color(0xFFBC3FBC), // Magenta
            36 to Color(0xFF11A8CD), // Cyan
            37 to Color(0xFFE5E5E5), // White
            
            // Bright colors
            90 to Color(0xFF666666), // Bright Black (Gray)
            91 to Color(0xFFF14C4C), // Bright Red
            92 to Color(0xFF23D18B), // Bright Green
            93 to Color(0xFFF5F543), // Bright Yellow
            94 to Color(0xFF3B8EEA), // Bright Blue
            95 to Color(0xFFD670D6), // Bright Magenta
            96 to Color(0xFF29B8DB), // Bright Cyan
            97 to Color(0xFFFFFFFF), // Bright White
            
            // Background colors (40-47, 100-107)
            40 to Color(0xFF000000),
            41 to Color(0xFFCD3131),
            42 to Color(0xFF0DBC79),
            43 to Color(0xFFE5E510),
            44 to Color(0xFF2472C8),
            45 to Color(0xFFBC3FBC),
            46 to Color(0xFF11A8CD),
            47 to Color(0xFFE5E5E5),
            
            100 to Color(0xFF666666),
            101 to Color(0xFFF14C4C),
            102 to Color(0xFF23D18B),
            103 to Color(0xFFF5F543),
            104 to Color(0xFF3B8EEA),
            105 to Color(0xFFD670D6),
            106 to Color(0xFF29B8DB),
            107 to Color(0xFFFFFFFF),
        )
        
        const val ESC = '\u001B'
        const val CSI = '['
    }
    
    /**
     * Parse ANSI escape sequences and apply them to terminal state.
     */
    fun parse(text: String, state: TerminalState) {
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            
            when {
                ch == ESC && i + 1 < text.length && text[i + 1] == CSI -> {
                    // Found CSI sequence: ESC[
                    val seqStart = i + 2
                    var seqEnd = seqStart
                    
                    // Find the end of the sequence (a letter)
                    while (seqEnd < text.length && !text[seqEnd].isLetter()) {
                        seqEnd++
                    }
                    
                    if (seqEnd < text.length) {
                        val params = text.substring(seqStart, seqEnd)
                        val command = text[seqEnd]
                        handleCsiSequence(params, command, state)
                        i = seqEnd + 1
                    } else {
                        i++
                    }
                }
                ch == '\r' -> {
                    // Carriage return - move cursor to start of line
                    state.cursorX = 0
                    i++
                }
                ch == '\n' -> {
                    // Line feed - move to next line
                    state.newLine()
                    i++
                }
                ch == '\b' -> {
                    // Backspace
                    if (state.cursorX > 0) {
                        state.cursorX--
                    }
                    i++
                }
                else -> {
                    // Regular character
                    state.writeChar(ch)
                    i++
                }
            }
        }
    }
    
    private fun handleCsiSequence(params: String, command: Char, state: TerminalState) {
        when (command) {
            'm' -> handleSgr(params, state) // Select Graphic Rendition
            'A' -> handleCursorUp(params, state)
            'B' -> handleCursorDown(params, state)
            'C' -> handleCursorForward(params, state)
            'D' -> handleCursorBack(params, state)
            'H', 'f' -> handleCursorPosition(params, state)
            'J' -> handleEraseDisplay(params, state)
            'K' -> handleEraseLine(params, state)
            's' -> state.saveCursor()
            'u' -> state.restoreCursor()
        }
    }
    
    private fun handleSgr(params: String, state: TerminalState) {
        if (params.isEmpty()) {
            state.resetStyle()
            return
        }
        
        val codes = params.split(';').mapNotNull { it.toIntOrNull() }
        var i = 0
        
        while (i < codes.size) {
            val code = codes[i]
            when (code) {
                0 -> state.resetStyle()
                1 -> state.bold = true
                2 -> state.dim = true
                3 -> state.italic = true
                4 -> state.underline = true
                7 -> state.inverse = true
                22 -> { state.bold = false; state.dim = false }
                23 -> state.italic = false
                24 -> state.underline = false
                27 -> state.inverse = false
                
                // Foreground colors
                in 30..37, in 90..97 -> {
                    state.foregroundColor = ANSI_COLORS[code]
                }
                39 -> state.foregroundColor = null // Default foreground
                
                // Background colors
                in 40..47, in 100..107 -> {
                    state.backgroundColor = ANSI_COLORS[code]
                }
                49 -> state.backgroundColor = null // Default background
                
                // 256 color mode: 38;5;n or 48;5;n
                38 -> {
                    if (i + 2 < codes.size && codes[i + 1] == 5) {
                        state.foregroundColor = get256Color(codes[i + 2])
                        i += 2
                    }
                }
                48 -> {
                    if (i + 2 < codes.size && codes[i + 1] == 5) {
                        state.backgroundColor = get256Color(codes[i + 2])
                        i += 2
                    }
                }
            }
            i++
        }
    }
    
    private fun handleCursorUp(params: String, state: TerminalState) {
        val n = params.toIntOrNull() ?: 1
        state.cursorY = maxOf(0, state.cursorY - n)
    }
    
    private fun handleCursorDown(params: String, state: TerminalState) {
        val n = params.toIntOrNull() ?: 1
        state.cursorY = minOf(state.lines.size - 1, state.cursorY + n)
    }
    
    private fun handleCursorForward(params: String, state: TerminalState) {
        val n = params.toIntOrNull() ?: 1
        state.cursorX += n
    }
    
    private fun handleCursorBack(params: String, state: TerminalState) {
        val n = params.toIntOrNull() ?: 1
        state.cursorX = maxOf(0, state.cursorX - n)
    }
    
    private fun handleCursorPosition(params: String, state: TerminalState) {
        val parts = params.split(';')
        val row = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val col = parts.getOrNull(1)?.toIntOrNull() ?: 1
        state.cursorY = maxOf(0, row - 1)
        state.cursorX = maxOf(0, col - 1)
    }
    
    private fun handleEraseDisplay(params: String, state: TerminalState) {
        val mode = params.toIntOrNull() ?: 0
        when (mode) {
            0 -> state.clearFromCursorToEnd()
            1 -> state.clearFromStartToCursor()
            2, 3 -> state.clearScreen()
        }
    }
    
    private fun handleEraseLine(params: String, state: TerminalState) {
        val mode = params.toIntOrNull() ?: 0
        when (mode) {
            0 -> state.clearLineFromCursor()
            1 -> state.clearLineBeforeCursor()
            2 -> state.clearLine()
        }
    }
    
    private fun get256Color(code: Int): Color {
        // Simplified 256 color palette
        return when (code) {
            in 0..15 -> ANSI_COLORS[30 + (code % 8)] ?: Color.White
            in 16..231 -> {
                // 216 color cube: 16 + 36*r + 6*g + b
                val idx = code - 16
                val r = (idx / 36) * 51
                val g = ((idx % 36) / 6) * 51
                val b = (idx % 6) * 51
                Color(r, g, b)
            }
            in 232..255 -> {
                // Grayscale
                val gray = 8 + (code - 232) * 10
                Color(gray, gray, gray)
            }
            else -> Color.White
        }
    }
}

