package cc.unitmesh.terminal

import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.NewUI
import org.jetbrains.plugins.terminal.LocalBlockTerminalRunner.Companion.BLOCK_TERMINAL_REGISTRY

class NewTerminalUiUtil {
    fun isNewTerminal(): Boolean {
        return NewUI.isEnabled() && !Registry.`is`(BLOCK_TERMINAL_REGISTRY, false)
    }
}
