package cc.unitmesh.comp.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.AbstractChatIntention
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

class TalkWithApiAction : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.companion.api.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.companion.api.family.name")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val task: Task.Backgroundable = object : Task.Backgroundable(project, "Collect context") {
            override fun run(indicator: ProgressIndicator) {
                // search all Controller file from project stub
                 val psiManager = PsiManager.getInstance(project)
                 val virtualFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project))
                 val psiFiles = virtualFiles.mapNotNull { psiManager.findFile(it) }
                 val controllerFiles = psiFiles.filter { it.name.endsWith("Controller.java") }

                // todo: convert by chapi
            }
        }

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}