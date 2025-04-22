package cc.unitmesh.devti.startup

import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.compiler.HobbitHoleParser
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.openapi.editor.Editor

data class DynamicDevInActionConfig(
    val name: String,
    val hole: HobbitHole? = null,
    val devinFile: DevInFile,
    val editor: Editor? = null,
) {
    companion object {
        fun from(file: DevInFile): DynamicDevInActionConfig {
            val hole = HobbitHoleParser.parse(file)
            return DynamicDevInActionConfig(file.name, hole, file)
        }
    }
}