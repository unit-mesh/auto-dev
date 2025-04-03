package cc.unitmesh.devti.gui.chat.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import javax.swing.Icon
import javax.swing.JPanel

data class FilePresentation(
    val virtualFile: VirtualFile,
    val name: String = virtualFile.name,
    val path: String = virtualFile.path,
    val size: Long = virtualFile.length,
    val icon: Icon? = null,
    val presentablePath: String = "",
    var panel: JPanel? = null,
    var namePanel: JPanel? = null
) {
    companion object {
        fun from(project: Project, file: VirtualFile): FilePresentation {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            val icon = psiFile?.getIcon(0) ?: AllIcons.FileTypes.Text
            
            return FilePresentation(
                virtualFile = file,
                name = file.name,
                path = file.path,
                size = file.length,
                icon = icon,
                presentablePath = getPresentablePath(project, file)
            )
        }
        
        private fun getPresentablePath(project: Project, file: VirtualFile): String =
            project.basePath?.let { basePath ->
                when (file.parent?.path) {
                    basePath -> file.name
                    else -> file.path.removePrefix(basePath)
                }
            } ?: file.path
    }
    
    fun relativePath(project: Project): String {
        return project.basePath?.let { basePath ->
            if (path.startsWith(basePath)) {
                path.substring(basePath.length).removePrefix("/")
            } else {
                path
            }
        } ?: path
    }
}
