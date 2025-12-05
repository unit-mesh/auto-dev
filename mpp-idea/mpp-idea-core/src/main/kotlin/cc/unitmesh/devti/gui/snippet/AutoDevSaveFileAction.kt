package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.vfs.VirtualFileWrapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.IOException

class AutoDevSaveFileAction : AnAction(AutoDevBundle.message("autodev.save.action")) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document)
        e.presentation.isEnabled = file != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        val content = document.text
        
        val descriptor = FileSaverDescriptor(AutoDevBundle.message("autodev.save.as.file"), AutoDevBundle.message("autodev.save.as.file.description"))
        
        val dialog: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val dir = project.baseDir
        val virtualFileWrapper: VirtualFileWrapper = dialog.save(dir, virtualFile.name) ?: return

        try {
            ApplicationManager.getApplication().runWriteAction {
                val file = virtualFileWrapper.file
                file.writeText(content)

                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                AutoDevNotifications.notify(project, "File saved successfully to: ${file.absolutePath}")
            }
        } catch (ex: IOException) {
            AutoDevNotifications.error(project, "Failed to save file: ${ex.message}")
        }
    }
}
