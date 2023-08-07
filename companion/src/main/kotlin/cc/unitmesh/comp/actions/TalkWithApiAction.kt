package cc.unitmesh.comp.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.intentions.AbstractChatIntention
import chapi.ast.javaast.JavaAnalyser
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ReadAction
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
import org.archguard.scanner.analyser.backend.JavaApiAnalyser
import org.archguard.scanner.core.sourcecode.ContainerService

class TalkWithApiAction : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.companion.api.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.companion.api.family.name")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val task: Task.Backgroundable = object : Task.Backgroundable(project, "Collect context") {
            override fun run(indicator: ProgressIndicator) {
                val psiManager = PsiManager.getInstance(project)
//                val virtualFiles =
//                    FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project))
//                val psiFiles = virtualFiles.mapNotNull {
//                    ReadAction.compute<PsiFile, Throwable> { psiManager.findFile(it) }
//                }

                val psiFiles = ReadAction.compute<List<PsiFile>, Throwable> {
                    val virtualFiles =
                        FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project))
                    virtualFiles.mapNotNull {
                        psiManager.findFile(it)
                    }
                }

                val javaApiAnalyser = JavaApiAnalyser()

                val controllerFiles: List<ContainerService> = psiFiles.asSequence()
                    .filter { it.name.endsWith("Controller.java") }.map {
                        JavaAnalyser().analysis(it.text, it.virtualFile.path).DataStructures
                    }.flatten().map {
                        javaApiAnalyser.analysisByNode(it, "")
                        javaApiAnalyser.toContainerServices()
                    }.flatten().toList()

                println(controllerFiles)
            }
        }

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }


}
