package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color

/**
 * Represents a single character cell in the terminal with its styling.
 */
data class TerminalCell(
    val char: Char = ' ',
    val foregroundColor: Color? = null,
    val backgroundColor: Color? = null,
    val bold: Boolean = false,
    val dim: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val inverse: Boolean = false
) {
    fun isEmpty(): Boolean = char == ' ' && foregroundColor == null && backgroundColor == null
}

/**
 * Represents a line in the terminal.
 */
data class TerminalLine(
    val cells: MutableList<TerminalCell> = mutableListOf()
) {
    fun getCell(x: Int): TerminalCell {
        // Expand line if needed
        while (cells.size <= x) {
            cells.add(TerminalCell())
        }
        return cells[x]
    }
    
    fun setCell(x: Int, cell: TerminalCell) {
        while (cells.size <= x) {
            cells.add(TerminalCell())
        }
        cells[x] = cell
    }
    
    fun clear() {
        cells.clear()
    }
    
    fun clearFrom(x: Int) {
        if (x < cells.size) {
            cells.subList(x, cells.size).clear()
        }
    }
    
    fun clearBefore(x: Int) {
        for (i in 0 until minOf(x, cells.size)) {
            cells[i] = TerminalCell()
        }
    }
    
    fun toText(): String {
        return cells.joinToString("") { it.char.toString() }.trimEnd()
    }
    
    fun isEmpty(): Boolean = cells.all { it.isEmpty() }
}

/**
 * Terminal state that maintains the screen buffer, cursor position, and current styling.
 * This is the core state that gets updated by the ANSI parser.
 */
class TerminalState(
    val maxLines: Int = 1000 // Maximum scrollback buffer
) {
    // Screen buffer
    val lines: SnapshotStateList<TerminalLine> = mutableStateListOf(TerminalLine())
    
    // Cursor position
    var cursorX: Int = 0
    var cursorY: Int = 0
    
    // Saved cursor position (for save/restore)
    private var savedCursorX: Int = 0
    private var savedCursorY: Int = 0
    
    // Current text style
    var foregroundColor: Color? = null
    var backgroundColor: Color? = null
    var bold: Boolean = false
    var dim: Boolean = false
    var italic: Boolean = false
    var underline: Boolean = false
    var inverse: Boolean = false
    
    /**
     * Write a character at the current cursor position with current styling.
     */
    fun writeChar(ch: Char) {
        ensureLineExists(cursorY)
        
        val currentLine = lines[cursorY]
        val cell = TerminalCell(
            char = ch,
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor,
            bold = bold,
            dim = dim,
            italic = italic,
            underline = underline,
            inverse = inverse
        )
        
        currentLine.setCell(cursorX, cell)
        cursorX++
    }
    
    /**
     * Move to a new line (like pressing Enter).
     */
    fun newLine() {
        cursorX = 0
        cursorY++
        ensureLineExists(cursorY)
        
        // Trim old lines if we exceed max buffer
        while (lines.size > maxLines) {
            lines.removeAt(0)
            cursorY--
        }
    }
    
    /**
     * Reset all text styling to defaults.
     */
    fun resetStyle() {
        foregroundColor = null
        backgroundColor = null
        bold = false
        dim = false
        italic = false
        underline = false
        inverse = false
    }
    
    /**
     * Clear the entire screen.
     */
    fun clearScreen() {
        lines.clear()
        lines.add(TerminalLine())
        cursorX = 0
        cursorY = 0
    }
    
    /**
     * Clear from cursor to end of screen.
     */
    fun clearFromCursorToEnd() {
        ensureLineExists(cursorY)
        
        // Clear from cursor to end of current line
        lines[cursorY].clearFrom(cursorX)
        
        // Clear all lines after current
        if (cursorY + 1 < lines.size) {
            lines.subList(cursorY + 1, lines.size).clear()
        }
    }
    
    /**
     * Clear from start of screen to cursor.
     */
    fun clearFromStartToCursor() {
        ensureLineExists(cursorY)
        
        // Clear all lines before current
        for (i in 0 until cursorY) {
            if (i < lines.size) {
                lines[i].clear()
            }
        }
        
        // Clear from start of current line to cursor
        lines[cursorY].clearBefore(cursorX + 1)
    }
    
    /**
     * Clear the current line.
     */
    fun clearLine() {
        ensureLineExists(cursorY)
        lines[cursorY].clear()
    }
    
    /**
     * Clear from cursor to end of line.
     */
    fun clearLineFromCursor() {
        ensureLineExists(cursorY)
        lines[cursorY].clearFrom(cursorX)
    }
    
    /**
     * Clear from start of line to cursor.
     */
    fun clearLineBeforeCursor() {
        ensureLineExists(cursorY)
        lines[cursorY].clearBefore(cursorX + 1)
    }
    
    /**
     * Save current cursor position.
     */
    fun saveCursor() {
        savedCursorX = cursorX
        savedCursorY = cursorY
    }
    
    /**
     * Restore saved cursor position.
     */
    fun restoreCursor() {
        cursorX = savedCursorX
        cursorY = savedCursorY
        ensureLineExists(cursorY)
    }
    
    /**
     * Ensure a line exists at the given index.
     */
    private fun ensureLineExists(y: Int) {
        while (lines.size <= y) {
            lines.add(TerminalLine())
        }
    }
    
    /**
     * Get the terminal content as plain text.
     */
    fun toText(): String {
        return lines.joinToString("\n") { it.toText() }
    }
    
    /**
     * Get visible lines (non-empty lines from the end).
     */
    fun getVisibleLines(): List<TerminalLine> {
        // Find the last non-empty line
        var lastNonEmpty = lines.size - 1
        while (lastNonEmpty >= 0 && lines[lastNonEmpty].isEmpty()) {
            lastNonEmpty--
        }
        
        return if (lastNonEmpty >= 0) {
            lines.subList(0, lastNonEmpty + 1)
        } else {
            emptyList()
        }
    }
}

