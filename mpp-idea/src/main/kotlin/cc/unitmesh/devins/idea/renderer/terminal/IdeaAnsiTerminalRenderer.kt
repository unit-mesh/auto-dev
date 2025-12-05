package cc.unitmesh.devins.idea.renderer.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.compose.IdeaLaunchedEffect
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devins.ui.compose.terminal.AnsiParser
import cc.unitmesh.devins.ui.compose.terminal.TerminalCell
import cc.unitmesh.devins.ui.compose.terminal.TerminalLine
import cc.unitmesh.devins.ui.compose.terminal.TerminalState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Jewel-themed ANSI terminal renderer for IntelliJ IDEA.
 * 
 * Renders terminal output with ANSI escape sequence support including:
 * - Colors (16 standard colors + 256 color palette)
 * - Text styles (bold, italic, underline, dim)
 * - Cursor movements and screen manipulation
 * 
 * Uses Jewel theme colors for better integration with IntelliJ IDEA UI.
 * 
 * @param ansiText Text containing ANSI escape sequences
 * @param modifier Modifier for the terminal container
 * @param maxHeight Maximum height of the terminal display in dp
 * @param backgroundColor Background color (defaults to Jewel panel background)
 */
@Composable
fun IdeaAnsiTerminalRenderer(
    ansiText: String,
    modifier: Modifier = Modifier,
    maxHeight: Int = 600,
    backgroundColor: Color = AutoDevColors.Neutral.c900
) {
    val terminalState = remember { TerminalState() }
    val parser = remember { AnsiParser() }

    // Parse the ANSI text when it changes
    IdeaLaunchedEffect(ansiText) {
        terminalState.clearScreen()
        parser.parse(ansiText, terminalState)
    }

    IdeaTerminalRenderer(
        terminalState = terminalState,
        modifier = modifier,
        showCursor = false,
        maxHeight = maxHeight,
        backgroundColor = backgroundColor
    )
}

/**
 * Core terminal renderer component using Jewel theming.
 */
@Composable
private fun IdeaTerminalRenderer(
    terminalState: TerminalState,
    modifier: Modifier = Modifier,
    showCursor: Boolean = false,
    maxHeight: Int = 600,
    backgroundColor: Color = AutoDevColors.Neutral.c900
) {
    val defaultForeground = AutoDevColors.Neutral.c300
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

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
                .padding(12.dp)
        ) {
            val visibleLines = terminalState.getVisibleLines()

            visibleLines.forEachIndexed { lineIndex, line ->
                IdeaTerminalLineRenderer(
                    line = line,
                    defaultForeground = defaultForeground,
                    showCursor = showCursor && lineIndex == terminalState.cursorY,
                    cursorX = if (lineIndex == terminalState.cursorY) terminalState.cursorX else -1
                )
            }

            // Show cursor on empty line if at the end
            if (showCursor && terminalState.cursorY >= visibleLines.size) {
                IdeaCursorIndicator()
            }
        }
    }
}

/**
 * Renders a single terminal line with styled text using Jewel components.
 */
@Composable
private fun IdeaTerminalLineRenderer(
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
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            ),
            modifier = Modifier.padding(vertical = 1.dp)
        )

        if (showCursor && cursorX >= 0 && cursorX >= line.cells.size) {
            IdeaCursorIndicator()
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
 * Cursor indicator for live terminals.
 */
@Composable
private fun IdeaCursorIndicator() {
    Box(
        modifier = Modifier
            .size(width = 8.dp, height = 16.dp)
            .background(AutoDevColors.Cyan.c400.copy(alpha = 0.7f))
    )
}

