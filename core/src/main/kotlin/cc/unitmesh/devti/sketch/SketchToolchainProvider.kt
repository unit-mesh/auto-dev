package cc.unitmesh.devti.sketch

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

data class Toolchain(val commandName: String, val description: String, val example: String) {
    override fun toString(): String =
        """<tool>name: ${commandName}, desc: $description, example:
<devin>
$example
</devin>
</tool>"""
}

interface SketchToolchainProvider {
    fun collect(): List<Toolchain>

    companion object {
        private val EP_NAME: ExtensionPointName<SketchToolchainProvider> =
            ExtensionPointName.create("cc.unitmesh.sketchToolchainProvider")

        fun collect(project: Project): List<Toolchain> {
            return EP_NAME.extensionList.flatMap {
                it.collect()
            }
        }
    }
}