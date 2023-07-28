package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.intentions.task.TestCodeGenRequest
import cc.unitmesh.devti.intentions.task.TestCodeGenTask
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.launch

class WriteTestIntention : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.test.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.test.family.name")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val element = getElementToAction(project, editor) ?: return
        selectElement(element, editor)

        LLMCoroutineScopeService.scope(project).launch {
            val request = TestCodeGenRequest(file, element, project, editor)
            val task = TestCodeGenTask(request)
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }
    }
}
