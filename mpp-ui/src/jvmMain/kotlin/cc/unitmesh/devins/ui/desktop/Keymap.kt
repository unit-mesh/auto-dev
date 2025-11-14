package cc.unitmesh.devins.ui.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import cc.unitmesh.agent.Platform

/**
 * Cross-platform keyboard shortcut definitions
 * 
 * On macOS, uses Command (Meta) key for standard shortcuts
 * On Windows/Linux, uses Ctrl key
 * 
 * Example:
 * ```kotlin
 * Item(
 *     "Open Project...",
 *     onClick = onOpenFile,
 *     shortcut = Keymap.openProject,
 *     mnemonic = 'O'
 * )
 * ```
 */
@OptIn(ExperimentalComposeUiApi::class)
object Keymap {
    private val isMac: Boolean = Platform.getOSName().contains("mac", ignoreCase = true)
    
    /**
     * Open Project: Command+O (Mac) / Ctrl+O (Windows/Linux)
     */
    val openProject: KeyShortcut = KeyShortcut(
        Key.O,
        meta = isMac,
        ctrl = !isMac
    )
    
    /**
     * Exit Application: Command+Q (Mac) / Ctrl+Q (Windows/Linux)
     */
    val exitApp: KeyShortcut = KeyShortcut(
        Key.Q,
        meta = isMac,
        ctrl = !isMac
    )
    
    /**
     * Copy: Command+C (Mac) / Ctrl+C (Windows/Linux)
     */
    val copy: KeyShortcut = KeyShortcut(
        Key.C,
        meta = isMac,
        ctrl = !isMac
    )
    
    /**
     * Paste: Command+V (Mac) / Ctrl+V (Windows/Linux)
     */
    val paste: KeyShortcut = KeyShortcut(
        Key.V,
        meta = isMac,
        ctrl = !isMac
    )
    
    /**
     * Save: Command+S (Mac) / Ctrl+S (Windows/Linux)
     */
    val save: KeyShortcut = KeyShortcut(
        Key.S,
        meta = isMac,
        ctrl = !isMac
    )
    
    /**
     * Undo: Command+Z (Mac) / Ctrl+Z (Windows/Linux)
     */
    val undo: KeyShortcut = KeyShortcut(
        Key.Z,
        meta = isMac,
        ctrl = !isMac
    )
    
    /**
     * Redo: Command+Shift+Z (Mac) / Ctrl+Shift+Z (Windows/Linux)
     */
    val redo: KeyShortcut = KeyShortcut(
        Key.Z,
        meta = isMac,
        ctrl = !isMac,
        shift = true
    )
    
    /**
     * New: Command+N (Mac) / Ctrl+N (Windows/Linux)
     */
    val new: KeyShortcut = KeyShortcut(
        Key.N,
        meta = isMac,
        ctrl = !isMac
    )
    
    /**
     * Find: Command+F (Mac) / Ctrl+F (Windows/Linux)
     */
    val find: KeyShortcut = KeyShortcut(
        Key.F,
        meta = isMac,
        ctrl = !isMac
    )
    
    /**
     * Select All: Command+A (Mac) / Ctrl+A (Windows/Linux)
     */
    val selectAll: KeyShortcut = KeyShortcut(
        Key.A,
        meta = isMac,
        ctrl = !isMac
    )
}
