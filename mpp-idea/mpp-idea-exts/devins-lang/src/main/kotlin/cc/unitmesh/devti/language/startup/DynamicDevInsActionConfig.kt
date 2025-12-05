package cc.unitmesh.devti.language.startup

import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.compiler.HobbitHoleParser
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.openapi.editor.Editor

data class DynamicDevInsActionConfig(
    val name: String,
    val hole: HobbitHole? = null,
    val devinFile: DevInFile,
    val editor: Editor? = null,
) {
    companion object {
        fun from(file: DevInFile): DynamicDevInsActionConfig {
            val hole = HobbitHoleParser.parse(file)
            return DynamicDevInsActionConfig(file.name, hole, file)
        }
    }
}