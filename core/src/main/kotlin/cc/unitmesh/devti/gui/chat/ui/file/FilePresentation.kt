package cc.unitmesh.devti.gui.chat.ui.file

import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
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
    var namePanel: JPanel? = null,
    var isRecentFile: Boolean = false
) {
    companion object {
        fun from(project: Project, file: VirtualFile): FilePresentation {
            val icon = VirtualFilePresentation.getIcon(file)
            
            return FilePresentation(
                virtualFile = file,
                name = file.name,
                path = file.path,
                size = file.length,
                icon = icon,
                presentablePath = getPresentablePath(project, file)
            )
        }
        
        private fun getPresentablePath(project: Project, file: VirtualFile): String {
            val path = project.basePath?.let { basePath ->
                when (file.parent?.path) {
                    basePath -> file.name
                    else -> file.path.removePrefix(basePath)
                }
            } ?: file.path

            return path.removePrefix("/")
        }
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
