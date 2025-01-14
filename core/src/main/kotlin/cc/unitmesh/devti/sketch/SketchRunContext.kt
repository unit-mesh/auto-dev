package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import java.text.SimpleDateFormat

data class SketchRunContext(
    // Current File
    @JvmField val currentFile: VirtualFile,
    /// related files
    @JvmField val selectedFile: List<VirtualFile>,
    /// ast related files
    @JvmField val relatedFiles: List<VirtualFile>,
    // The absolute path of the USER's workspace
    @JvmField val workspace: String = workspace(),
    // The USER's OS
    @JvmField val os: String = osInfo(),
    // The current time in YYYY-MM-DD HH:MM:SS format
    @JvmField val time: String = time(),
    /// The USER's requirements
    @JvmField val input: String,
    /// toolList
    @JvmField val toolList: List<Toolchain>,
    /// shell path: The user's shell is
    @JvmField val shell: String = System.getenv("SHELL") ?: "/bin/bash",
) : TemplateContext {
    companion object {
        fun create(project: Project, editor: Editor): SketchRunContext {
            val currentFile: VirtualFile = FileDocumentManager.getInstance().getFile(editor.document)!!
            return SketchRunContext(
                currentFile = currentFile,
                selectedFile = emptyList(),
                relatedFiles = emptyList(),
                input = editor.document.text,
                workspace = workspace(project),
                toolList = SketchToolchainProvider.collect(project, editor),
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
