package cc.unitmesh.devti.gui.chat.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import javax.swing.Icon

data class FilePresentation(
    val name: String,
    val path: String,
    val size: Long,
    val icon: Icon,
    val presentablePath: String
) {
    companion object {
        fun from(project: Project, file: VirtualFile): FilePresentation {
            val name = file.name
            val path = file.path
            val size = file.length
            val psiFile = PsiManager.getInstance(project).findFile(file)
            val icon = psiFile?.getIcon(0) ?: AllIcons.FileTypes.Text

            return FilePresentation(name, path, size, icon, getPresentablePath(project, file))
        }

        private fun getPresentablePath(project: Project, file: VirtualFile): String =
            project.basePath?.let { basePath ->
                when (file.parent?.path) {
                    basePath -> file.name
                    else -> file.path.removePrefix(basePath)
                }
            } ?: file.path
    }
}