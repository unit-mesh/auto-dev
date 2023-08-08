package cc.unitmesh.devti.custom

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.intentions.AbstractChatIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class CustomIntention(private val intentionConfig: CustomIntentionConfig) : AbstractChatIntention() {
    val specConfig: Map<String, String> = CustomPromptConfig.load().spec

    override fun getText(): String = intentionConfig.title

    override fun getFamilyName(): String = "Custom Intention"

    override fun getActionType(): ChatActionType = super.getActionType()

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val withRange = elementWithRange(editor, file, project) ?: return
        val selectedText = withRange.first
        val psiElement = withRange.second

        val prompt: CustomIntentionPrompt = buildCustomPrompt(psiElement!!, selectedText, intentionConfig)

    }

    private fun buildCustomPrompt(
        psiElement: PsiElement,
        selectedText: @NlsSafe String,
        config: CustomIntentionConfig,
    ): CustomIntentionPrompt {
        return CustomIntentionPrompt(selectedText, selectedText, listOf())
    }

    companion object {
        fun create(intentionConfig: CustomIntentionConfig): CustomIntention {
            return CustomIntention(intentionConfig)
        }
    }
}