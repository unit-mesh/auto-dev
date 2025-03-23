package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.vfs.VirtualFileWrapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.io.IOException

class AutoDevSaveFileAction : AnAction(AutoDevBundle.message("autodev.save.action")) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

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
        val content = virtualFile.contentsToByteArray().toString(Charsets.UTF_8)
        
        val descriptor = FileSaverDescriptor(
            "Save File As",
            "Choose location to save the file",
            *arrayOf()
        )
        
        val dialog: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val dir = project.baseDir
        val virtualFileWrapper: VirtualFileWrapper? = dialog.save(dir, virtualFile.name)
        
        if (virtualFileWrapper != null) {
            try {
                ApplicationManager.getApplication().runWriteAction {
                    val file = virtualFileWrapper.file
                    file.writeText(content)
                    
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    
                    Messages.showInfoMessage(
                        project,
                        "File saved successfully to: ${file.absolutePath}",
                        "File Saved"
                    )
                }
            } catch (ex: IOException) {
                Messages.showErrorDialog(
                    project,
                    "Failed to save file: ${ex.message}",
                    "Error"
                )
            }
        }
    }
}
