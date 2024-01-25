package cc.unitmesh.devti.custom

import cc.unitmesh.devti.custom.document.CustomDocumentationConfig
import cc.unitmesh.devti.custom.document.CustomLivingDocTask
import cc.unitmesh.devti.intentions.action.base.BasedDocumentationBaseIntention
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.psi.PsiElement

class CustomDocumentationBaseIntention(override val config: CustomDocumentationConfig) : BasedDocumentationBaseIntention() {

    override fun getText(): String = config.title

    override fun getFamilyName(): String = "AutoDev: Custom Documentation Intention"
    override fun priority(): Int = 99

    override fun writingDocument(editor: Editor, element: PsiElement, documentation: LivingDocumentation) {
        val task: Task.Backgroundable = CustomLivingDocTask(editor, element, config)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    companion object {
        fun create(config: CustomDocumentationConfig) = CustomDocumentationBaseIntention(config)
    }
}
