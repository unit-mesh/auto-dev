package cc.unitmesh.agent.linter

import cc.unitmesh.agent.linter.linters.BiomeLinter
import cc.unitmesh.agent.linter.linters.DetektLinter
import cc.unitmesh.agent.linter.linters.PMDLinter
import cc.unitmesh.agent.linter.linters.RuffLinter
import cc.unitmesh.agent.linter.linters.ShellCheckLinter
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor

/**
 * JVM platform-specific linter registration
 */
actual fun registerPlatformLinters(registry: LinterRegistry) {
    val shellExecutor = DefaultShellExecutor()
    
    // Register ShellBasedLinters for common tools
    registry.register(BiomeLinter(shellExecutor))
    registry.register(DetektLinter(shellExecutor))
    registry.register(RuffLinter(shellExecutor))
    registry.register(ShellCheckLinter(shellExecutor))
    registry.register(PMDLinter(shellExecutor))
}

