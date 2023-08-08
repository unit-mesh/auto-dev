package cc.unitmesh.devti.custom

import cc.unitmesh.devti.custom.variable.*
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.AbstractChatIntention
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.builtin.DefaultContextPrompter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter

class CustomIntention(private val intentionConfig: CustomIntentionConfig) : AbstractChatIntention() {
    val specConfig: Map<String, String> = CustomPromptConfig.load().spec

    override fun getText(): String = intentionConfig.title

    override fun getFamilyName(): String = "Custom Intention"

    override fun getActionType(): ChatActionType = ChatActionType.CUSTOM_ACTION

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val withRange = elementWithRange(editor, file, project) ?: return
        val selectedText = withRange.first
        val psiElement = withRange.second
        val prompt: CustomIntentionPrompt = buildCustomPrompt(psiElement!!, selectedText, intentionConfig)

        sendToChatPanel(project, getActionType(), object : ContextPrompter() {
            override fun displayPrompt(): String {
                return prompt.displayPrompt
            }

            override fun requestPrompt(): String {
                return prompt.requestPrompt
            }
        })
    }

    private fun buildCustomPrompt(
        psiElement: PsiElement,
        selectedText: @NlsSafe String,
        config: CustomIntentionConfig,
    ): CustomIntentionPrompt {
        val stringBuilderWriter = StringWriter()
        val velocityContext = VelocityContext()

        val variableResolvers = arrayOf(
            MethodInputOutputVariableResolver(psiElement),
            SelectionVariableResolver(psiElement.language.displayName ?: "", selectedText),
        ) + SpecResolverService.getInstance().createResolvers()

        val resolverMap = LinkedHashMap<String, VariableResolver>(10)
        for (resolver in variableResolvers) {
            resolverMap[resolver.variableName()] = resolver
        }

        resolverMap.forEach { (variableType, resolver) ->
            val value = resolver.resolve()
            velocityContext.put(variableType, value)
        }

        Velocity.evaluate(velocityContext, stringBuilderWriter, "", intentionConfig.template)
        val output = stringBuilderWriter.toString()

        return CustomIntentionPrompt(output, output, listOf())
    }

    companion object {
        fun create(intentionConfig: CustomIntentionConfig): CustomIntention {
            return CustomIntention(intentionConfig)
        }
    }
}