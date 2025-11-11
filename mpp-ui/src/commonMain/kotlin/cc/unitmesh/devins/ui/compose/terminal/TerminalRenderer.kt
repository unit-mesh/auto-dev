package cc.unitmesh.devins.ui.compose.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.sketch.getFiraCodeFontFamily

/**
 * Terminal renderer that displays terminal state with ANSI color support.
 *
 * This component renders a terminal buffer with proper ANSI escape sequence handling,
 * including colors, text styles, and cursor movements.
 *
 * @param terminalState The terminal state to render
 * @param modifier Modifier for the terminal container
 * @param showCursor Whether to show the cursor (for live terminals)
 * @param maxHeight Maximum height of the terminal display
 */
@Composable
fun TerminalRenderer(
    terminalState: TerminalState,
    modifier: Modifier = Modifier,
    showCursor: Boolean = false,
    maxHeight: Int = 600
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val defaultForeground = MaterialTheme.colorScheme.onSurface

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Auto-scroll to bottom when new content is added
    LaunchedEffect(terminalState.lines.size) {
        verticalScrollState.animateScrollTo(verticalScrollState.maxValue)
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .heightIn(max = maxHeight.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
                .padding(8.dp)
        ) {
            val visibleLines = terminalState.getVisibleLines()

            visibleLines.forEachIndexed { lineIndex, line ->
                TerminalLineRenderer(
                    line = line,
                    defaultForeground = defaultForeground,
                    showCursor = showCursor && lineIndex == terminalState.cursorY,
                    cursorX = if (lineIndex == terminalState.cursorY) terminalState.cursorX else -1
                )
            }

            // Show cursor on empty line if at the end
            if (showCursor && terminalState.cursorY >= visibleLines.size) {
                CursorIndicator()
            }
        }
    }
}

/**
 * Renders a single terminal line with styled text.
 */
@Composable
private fun TerminalLineRenderer(
    line: TerminalLine,
    defaultForeground: Color,
    showCursor: Boolean,
    cursorX: Int
) {
    val annotatedString = buildAnnotatedString {
        line.cells.forEachIndexed { index, cell ->
            val fgColor = getCellColor(cell, defaultForeground, isBackground = false)
            val bgColor = getCellBackgroundColor(cell)
            val alpha = if (cell.dim) 0.6f else 1.0f

            pushStyle(
                SpanStyle(
                    color = fgColor.copy(alpha = alpha),
                    background = bgColor,
                    fontWeight = if (cell.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (cell.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (cell.underline) TextDecoration.Underline else null
                )
            )

            append(cell.char)
            pop()
        }

        // Add cursor if needed
        if (showCursor && cursorX >= 0) {
            // Ensure we have enough space for cursor
            while (length <= cursorX) {
                append(' ')
            }
        }
    }

    Row {
        Text(
            text = annotatedString,
            fontFamily = getFiraCodeFontFamily(),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(vertical = 1.dp)
        )

        if (showCursor && cursorX >= 0 && cursorX >= line.cells.size) {
            CursorIndicator()
        }
    }
}

/**
 * Get the foreground color for a cell, handling inverse video.
 */
private fun getCellColor(cell: TerminalCell, defaultColor: Color, isBackground: Boolean): Color {
    val fg = cell.foregroundColor ?: defaultColor
    val bg = cell.backgroundColor ?: Color.Transparent

    return if (cell.inverse) {
        if (isBackground) fg else bg.takeIf { it != Color.Transparent } ?: defaultColor
    } else {
        if (isBackground) bg else fg
    }
}

/**
 * Get the background color for a cell.
 */
private fun getCellBackgroundColor(cell: TerminalCell): Color {
    val bg = cell.backgroundColor ?: Color.Transparent
    val fg = cell.foregroundColor ?: Color.Transparent

    return if (cell.inverse) {
        fg.takeIf { it != Color.Transparent } ?: Color.Transparent
    } else {
        bg
    }
}

/**
 * Simple cursor indicator.
 */
@Composable
private fun CursorIndicator() {
    Box(
        modifier = Modifier
            .size(8.dp, 16.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    )
}

/**
 * Parse ANSI text and render it in a terminal.
 * This is a convenience composable that handles both parsing and rendering.
 *
 * @param ansiText Text containing ANSI escape sequences
 * @param modifier Modifier for the terminal container
 * @param maxHeight Maximum height of the terminal display
 */
@Composable
fun AnsiTerminalRenderer(
    ansiText: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 600
) {
    val terminalState = remember { TerminalState() }
    val parser = remember { AnsiParser() }

    // Parse the ANSI text when it changes
    LaunchedEffect(ansiText) {
        terminalState.clearScreen()
        parser.parse(ansiText, terminalState)
    }

    TerminalRenderer(
        terminalState = terminalState,
        modifier = modifier,
        showCursor = false,
        maxHeight = maxHeight
    )
}

/**
 * Live terminal renderer that can be updated incrementally.
 * Useful for streaming terminal output.
 *
 * @param modifier Modifier for the terminal container
 * @param maxHeight Maximum height of the terminal display
 * @param showCursor Whether to show a blinking cursor
 * @param onTerminalReady Callback when terminal is ready, provides functions to append text
 */
@Composable
fun LiveTerminalRenderer(
    modifier: Modifier = Modifier,
    maxHeight: Int = 600,
    showCursor: Boolean = true,
    onTerminalReady: (appendText: (String) -> Unit, clear: () -> Unit) -> Unit = { _, _ -> }
) {
    val terminalState = remember { TerminalState() }
    val parser = remember { AnsiParser() }

    // Provide callbacks to parent
    LaunchedEffect(Unit) {
        onTerminalReady(
            { text -> parser.parse(text, terminalState) },
            { terminalState.clearScreen() }
        )
    }

    TerminalRenderer(
        terminalState = terminalState,
        modifier = modifier,
        showCursor = showCursor,
        maxHeight = maxHeight
    )
}

