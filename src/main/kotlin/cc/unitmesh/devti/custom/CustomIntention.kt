package cc.unitmesh.devti.custom

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.intentions.AbstractChatIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class CustomIntention(private val intentionConfig: CustomIntentionConfig) : AbstractChatIntention() {

    override fun getText(): String = intentionConfig.title

    override fun getFamilyName(): String = "Custom Intention"

    override fun getActionType(): ChatActionType = super.getActionType()

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {

    }

    companion object {
        fun create(intentionConfig: CustomIntentionConfig): CustomIntention {
            return CustomIntention(intentionConfig)
        }
    }
}