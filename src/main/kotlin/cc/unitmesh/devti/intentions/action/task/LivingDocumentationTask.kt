package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.provider.LivingDocumentation
import cc.unitmesh.devti.provider.LivingDocumentationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner

class LivingDocumentationTask(
    val editor: Editor,
    val project: Project,
    val target: PsiNameIdentifierOwner,
    val type: LivingDocumentationType = LivingDocumentationType.NORMAL
): Task.Backgroundable(project, AutoDevBundle.message("intentions.request.background.process.title"))  {
    override fun run(indicator: ProgressIndicator) {
        val documentation = LivingDocumentation.forLanguage(target.language) ?: return
        // todo: send prompt to gpt
        documentation.updateDoc(target, "")
    }

}