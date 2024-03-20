package cc.unitmesh.devti.language.documentation

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.custom.compile.CustomVariable
import cc.unitmesh.devti.language.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.psi.DevInTypes
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType

class DevInsDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val project = element?.project ?: return null
        return when (element.elementType) {
            DevInTypes.AGENT_ID -> {
                val agentConfigs = CustomAgentConfig.loadFromProject(project).filter {
                    it.name == element.text
                }

                if (agentConfigs.isEmpty()) return null
                agentConfigs.joinToString("\n") { it.description }
            }

            DevInTypes.COMMAND_ID -> {
                BuiltinCommand.all().find { it.commandName == element.text }?.description
            }
            DevInTypes.VARIABLE_ID -> {
                CustomVariable.all().find { it.variable == element.text }?.description
            }

            else -> {
                element.text
            }
        }
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? = contextElement ?: file.findElementAt(targetOffset)
}
