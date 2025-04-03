package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.Box
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.JToolBar

object InputFileToolbar {
    fun createToolbar(project: Project, model: DefaultListModel<ModelWrapper>): JToolBar {
        val toolbar = JToolBar()
        toolbar.isFloatable = false

        val reminderLabel = JBLabel(AutoDevBundle.message("chat.panel.select.files"))
        reminderLabel.border = JBUI.Borders.empty(5)
        reminderLabel.foreground = UIUtil.getContextHelpForeground()
        toolbar.add(reminderLabel)

        toolbar.add(Box.createHorizontalGlue())

        val clearAll = LinkLabel(AutoDevBundle.message("chat.panel.clear.all"), null) { _: LinkLabel<Unit>, _: Unit? ->
            model.removeAllElements()
        }

        clearAll.mediumFontFunction()
        clearAll.border = JBUI.Borders.emptyRight(20)

        toolbar.add(clearAll)

        return toolbar
    }

    private const val DEFAULT_FILE_LIMIT = 12

    fun getRecentlyOpenedFiles(
        project: Project,
        maxFiles: Int = DEFAULT_FILE_LIMIT
    ): List<FilePresentation> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles
            .take(maxFiles)
            .map { FilePresentation.from(project, it) }
            .toMutableList()
    }
}

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
