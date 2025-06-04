package cc.unitmesh.devti.gui.chat.ui.file

import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
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

    /**
     * Constructs a display name for the given file presentation based on the associated virtual file.
     *
     * For file-system routing frameworks where files have conventional names but directories carry semantic meaning:
     * - Next.js: app/dashboard/page.tsx -> dashboard/page.tsx
     * - Django: myapp/views.py -> myapp/views.py
     * - Nuxt: pages/about/index.vue -> about/index.vue
     *
     * Shows directory context for conventional filenames that appear frequently across projects.
     */
    fun displayName(): @NlsSafe String {
        val filename = this.virtualFile.name
        val filenameWithoutExtension = filename.substringBeforeLast('.')

        // File-system routing and framework convention patterns
        val routingConventionFiles = setOf(
            // Next.js App Router
            "page", "layout", "loading", "error", "not-found", "route", "template", "default",
            // Traditional index files
            "index",
            // Django patterns
            "views", "models", "urls", "forms", "admin", "serializers", "tests",
            // Flask/FastAPI patterns
            "app", "main", "routes", "models", "schemas",
            // Vue/Nuxt patterns
            "middleware", "plugins", "store",
            // React/Vue component patterns
            "component", "components", "hook", "hooks", "context", "provider",
            // General patterns
            "config", "settings", "constants", "types", "utils", "helpers"
        )

        if (filenameWithoutExtension in routingConventionFiles) {
            val parent = this.virtualFile.parent?.name
            if (parent != null) {
                // For index files, show more context as they're especially common
                if (filenameWithoutExtension == "index") {
                    val grandParent = this.virtualFile.parent?.parent?.name
                    return if (grandParent != null) {
                        "$grandParent/$parent/$filename"
                    } else {
                        "$parent/$filename"
                    }
                } else {
                    // For other conventional files, show parent directory context
                    return "$parent/$filename"
                }
            }
        }

        return filename
    }
}
