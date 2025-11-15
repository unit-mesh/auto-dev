package cc.unitmesh.agent.linter

import cc.unitmesh.agent.linter.linters.ActionlintLinter
import cc.unitmesh.agent.linter.linters.BiomeLinter
import cc.unitmesh.agent.linter.linters.CheckovLinter
import cc.unitmesh.agent.linter.linters.DetektLinter
import cc.unitmesh.agent.linter.linters.DotenvLinter
import cc.unitmesh.agent.linter.linters.ESLintLinter
import cc.unitmesh.agent.linter.linters.GitleaksLinter
import cc.unitmesh.agent.linter.linters.GolangciLintLinter
import cc.unitmesh.agent.linter.linters.HTMLHintLinter
import cc.unitmesh.agent.linter.linters.HadolintLinter
import cc.unitmesh.agent.linter.linters.MarkdownlintLinter
import cc.unitmesh.agent.linter.linters.PMDLinter
import cc.unitmesh.agent.linter.linters.PylintLinter
import cc.unitmesh.agent.linter.linters.RuffLinter
import cc.unitmesh.agent.linter.linters.SQLFluffLinter
import cc.unitmesh.agent.linter.linters.SemgrepLinter
import cc.unitmesh.agent.linter.linters.ShellCheckLinter
import cc.unitmesh.agent.linter.linters.SwiftLintLinter
import cc.unitmesh.agent.linter.linters.YamllintLinter
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor

/**
 * JavaScript platform-specific linter registration
 */
actual fun registerPlatformLinters(registry: LinterRegistry) {
    val shellExecutor = DefaultShellExecutor()

    /// ActionlintLinter
    registry.register(ActionlintLinter(shellExecutor))
    //BiomeLinter
    registry.register(BiomeLinter(shellExecutor))
    //CheckovLinter
    registry.register(CheckovLinter(shellExecutor))
    //DetektLinter
    registry.register(DetektLinter(shellExecutor))
    //DotenvLinter
    registry.register(DotenvLinter(shellExecutor))
    //ESLintLinter
    registry.register(ESLintLinter(shellExecutor))
    //GitleaksLinter
    registry.register(GitleaksLinter(shellExecutor))
    //GolangciLintLinter
    registry.register(GolangciLintLinter(shellExecutor))
    //HadolintLinter
    registry.register(HadolintLinter(shellExecutor))
    //HTMLHintLinter
    registry.register(HTMLHintLinter(shellExecutor))
    //MarkdownlintLinter
    registry.register(MarkdownlintLinter(shellExecutor))
    //PMDLinter
    registry.register(PMDLinter(shellExecutor))
    //PylintLinter
    registry.register(PylintLinter(shellExecutor))
    //RuffLinter
    registry.register(RuffLinter(shellExecutor))
    //SemgrepLinter
    registry.register(SemgrepLinter(shellExecutor))
    //ShellCheckLinter
    registry.register(ShellCheckLinter(shellExecutor))
    //SQLFluffLinter
    registry.register(SQLFluffLinter(shellExecutor))
    //SwiftLintLinter
    registry.register(SwiftLintLinter(shellExecutor))
    //YamllintLinter
    registry.register(YamllintLinter(shellExecutor))
}

