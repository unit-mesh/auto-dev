package cc.unitmesh.comp.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.intentions.AbstractChatIntention
import chapi.ast.javaast.JavaAnalyser
import chapi.domain.core.CodeDataStruct
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
import org.archguard.scanner.core.sourcecode.ContainerSupply
import java.awt.EventQueue.invokeLater

class TalkWithApiAction : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.companion.api.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.companion.api.family.name")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val task: Task.Backgroundable = object : Task.Backgroundable(project, "Collect context") {
            override fun run(indicator: ProgressIndicator) {
                val psiManager = PsiManager.getInstance(project)
                val psiFiles = ReadAction.compute<List<PsiFile>, Throwable> {
                    val virtualFiles =
                        FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project))
                    virtualFiles.mapNotNull {
                        psiManager.findFile(it)
                    }
                }

                val apiList: List<String> = psiFiles.asSequence()
                    .filter { it.name.endsWith("Controller.java") }.map {
                        ReadAction.compute<List<CodeDataStruct>, Throwable> {
                            JavaAnalyser().analysis(it.text, it.virtualFile.path).DataStructures
                        }
                    }.flatten().map {
                        val javaApiAnalyser = JavaApiAnalyser()
                        javaApiAnalyser.analysisByNode(it, "")
                        javaApiAnalyser.toContainerServices()
                    }.flatten()
                    .map {
                        it.resources
                    }.flatten()
                    .map {
                        val methodInfo = it.packageName + "." + it.className + "." + it.methodName
                        it.sourceHttpMethod + " " + it.sourceUrl + " (" + methodInfo + ")"
                    }
                    .toList()

                invokeLater {
                    sendToChatWindow(project, ChatActionType.CHAT) { contentPanel, _ ->
                        contentPanel.setInput(
                            """You are an experienced software developer who can help me understand this system.

Here is all this system's APIs:
${apiList.joinToString("\n")}

If you are understand, reply me with "I understand".
"""
                        )
                    }
                }
            }
        }

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }



}
