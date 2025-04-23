package cc.unitmesh.devti.language.run.runner

import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.compiler.DevInsCompiledResult
import com.intellij.openapi.editor.Editor

class ShireRunnerContext(
    val hole: HobbitHole?,
    val editor: Editor?,
    val compileResult: DevInsCompiledResult,
    val finalPrompt: String = "",
    val hasError: Boolean,
    val compiledVariables: Map<String, Any>,
)
