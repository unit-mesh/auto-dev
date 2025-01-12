package cc.unitmesh.devti.custom

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.action.CustomIntentionConfig
import cc.unitmesh.devti.custom.action.CustomIntentionPrompt
import cc.unitmesh.devti.custom.variable.*
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.StringWriter
import java.util.regex.Pattern

class CustomActionBaseIntention(private val intentionConfig: CustomIntentionConfig) : ChatBaseIntention() {
    private val logger = logger<CustomActionBaseIntention>()

    override fun getText(): String = intentionConfig.title
    override fun getFamilyName() = AutoDevBundle.message("autodev.custom.intentions.family")
    override fun priority() = intentionConfig.priority
    override fun getActionType(): ChatActionType = ChatActionType.CUSTOM_ACTION

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false

        val regexString = intentionConfig.matchRegex
        return try {
            val regex = Regex(regexString)
            regex.matches(file.name)
        } catch (e: Exception) {
            false
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val withRange = elementWithRange(editor, file, project) ?: return
        val selectedText = withRange.first
        val psiElement = withRange.second
        val beforeCursor = editor.document.text.substring(0, editor.caretModel.offset)
        val afterCursor = editor.document.text.substring(editor.caretModel.offset)

        val prompt: CustomIntentionPrompt = constructCustomPrompt(
            psiElement!!, findAllMatches(selectedText, intentionConfig.selectedRegex),
            beforeCursor, afterCursor
        )

        if (intentionConfig.autoInvoke) {
            sendToChatPanel(project, getActionType(), object : ContextPrompter() {
                override fun displayPrompt(): String {
                    return prompt.displayPrompt
                }

                override fun requestPrompt(): String {
                    return prompt.requestPrompt
                }
            })
        } else {
            sendToChatPanel(project) { panel, _ ->
                panel.setInput(prompt.displayPrompt)
            }
        }
    }

    private fun findAllMatches(input: String, regex: String): String {
        if (regex == "") return input
        if (input == "") return input

        val builder = StringBuilder()

        try {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(input)
            while (matcher.find()) {
                builder.append(matcher.group()).append("\n")
            }
        } catch (e: Exception) {
            logger.error("Failed to compile regex: $regex", e)
        }

        return builder.toString()
    }

    private fun constructCustomPrompt(
        psiElement: PsiElement,
        selectedText: String,
        beforeCursor: String,
        afterCursor: String
    ): CustomIntentionPrompt {
        val velocityContext = VelocityContext()

        val variableResolvers = arrayOf(
            ClassStructureVariableResolver(psiElement),
            MethodInputOutputVariableResolver(psiElement),
            SimilarChunkVariableResolver(psiElement),
            SelectionVariableResolver(psiElement.language.displayName, selectedText),
        ) + SpecResolverService.getInstance().resolvers()

        val resolverMap = LinkedHashMap<String, VariableResolver>(20)
        variableResolvers.forEach { resolver ->
            resolverMap[resolver.variableName()] = resolver
        }

        resolverMap.forEach { (variableType, resolver) ->
            val value = resolver.resolve()
            velocityContext.put(variableType, value)
        }

        velocityContext.put("beforeCursor", beforeCursor)
        velocityContext.put("afterCursor", afterCursor)
        val filePath = psiElement.containingFile.virtualFile.path
        velocityContext.put("filePath", filePath)

        val oldContextClassLoader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader(CustomActionBaseIntention::class.java.getClassLoader())

        val sw = StringWriter()
        try {
            Velocity.evaluate(velocityContext, sw, "#" + this.javaClass.name, intentionConfig.template)
        } catch (e: Exception) {
            logger.error("Failed to compile template: $intentionConfig", e)
        }

        Thread.currentThread().setContextClassLoader(oldContextClassLoader)
        val output = sw.toString()

        return CustomIntentionPrompt(output, output, listOf())
    }

    companion object {
        fun create(intentionConfig: CustomIntentionConfig): CustomActionBaseIntention {
            return CustomActionBaseIntention(intentionConfig)
        }
    }
}