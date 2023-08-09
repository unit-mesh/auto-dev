package cc.unitmesh.devti.custom

import cc.unitmesh.devti.custom.task.CustomLivingDocTask
import cc.unitmesh.devti.intentions.action.base.BasedDocumentationIntention
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.psi.PsiNameIdentifierOwner

class CustomDocumentationIntention(override val config: CustomDocumentationConfig) : BasedDocumentationIntention() {

    override fun getText(): String = config.title

    override fun getFamilyName(): String = "AutoDev: Custom Documentation Intention"
    override fun priority(): Int = 99

    override fun writingDocument(editor: Editor, element: PsiNameIdentifierOwner) {
        val task: Task.Backgroundable = CustomLivingDocTask(editor, element, config)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    companion object {
        fun create(it: CustomDocumentationConfig): CustomDocumentationIntention {
            return CustomDocumentationIntention(it)
        }
    }
}
