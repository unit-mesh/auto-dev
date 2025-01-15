package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import cc.unitmesh.devti.sketch.run.ShellUtil
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat

data class SketchRunContext(
    val currentFile: VirtualFile?,
    val currentElement: PsiElement? = null,
    val selectedFile: List<VirtualFile>,
    val relatedFiles: List<VirtualFile>,
    val workspace: String = workspace(),
    val os: String = osInfo(),
    val time: String = time(),
    val userInput: String,
    val toolList: String,
    val shell: String = System.getenv("SHELL") ?: "/bin/bash",
    val frameworkContext: String = ""
) : TemplateContext {
    companion object {
        fun create(project: Project, myEditor: Editor?, input: String): SketchRunContext {
            val editor = myEditor ?: FileEditorManager.getInstance(project).selectedTextEditor
            val currentFile: VirtualFile? = if (editor != null) {
                FileDocumentManager.getInstance().getFile(editor.document)!!
            } else {
                FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            }

            val psi = if (currentFile != null) {
                PsiManager.getInstance(project).findFile(currentFile)
            } else {
                null
            }

            val currentElement = if (editor != null) {
                psi?.findElementAt(editor.caretModel.offset)
            } else {
                null
            }

            val creationContext =
                ChatCreationContext(ChatOrigin.Intention, ChatActionType.CHAT, psi, listOf(), element = psi)

            return SketchRunContext(
                currentFile = currentFile,
                currentElement = currentElement,
                selectedFile = emptyList(),
                relatedFiles = emptyList(),
                userInput = input,
                workspace = workspace(project),
                toolList = SketchToolchainProvider.collect(project).joinToString("\n"),
                shell = ShellUtil.listShell()?.firstOrNull() ?: "/bin/bash",
                frameworkContext = runBlocking {
                    return@runBlocking ChatContextProvider.collectChatContextList(project, creationContext)
                }.joinToString(",", transform = ChatContextItem::text)
            )
        }
    }
}

private fun osInfo() =
    System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch")

private fun time() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())

private fun workspace(myProject: Project? = null): String {
    val project = myProject ?: ProjectManager.getInstance().openProjects.firstOrNull()
    return project?.guessProjectDir()?.path ?: ""
}
