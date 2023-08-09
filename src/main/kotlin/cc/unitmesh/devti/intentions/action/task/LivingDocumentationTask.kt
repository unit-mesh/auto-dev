package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.llms.LLMProviderFactory
import cc.unitmesh.devti.provider.LivingDocumentation
import cc.unitmesh.devti.provider.LivingDocumentationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking


class LivingDocumentationTask(
    val editor: Editor,
    val target: PsiNameIdentifierOwner,
    val type: LivingDocumentationType = LivingDocumentationType.NORMAL
): Task.Backgroundable(editor.project, AutoDevBundle.message("intentions.request.background.process.title"))  {
    override fun run(indicator: ProgressIndicator) {
        val documentation = LivingDocumentation.forLanguage(target.language) ?: return
        val stream =
            LLMProviderFactory().connector(project).stream("Generating documentation for ${target.name}...", "")

        var result = ""

        runBlocking {
            stream.collect {
                result += it
            }
        }

        documentation.updateDoc(target, "")
    }

}