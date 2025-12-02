package cc.unitmesh.devins.idea.renderer.sketch.actions

import cc.unitmesh.devti.AutoDevNotifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException

/**
 * Business logic actions for Code operations in mpp-idea.
 * Reuses core module's AutoDevCopyToClipboardAction, AutoDevInsertCodeAction, AutoDevSaveFileAction logic.
 * 
 * Related GitHub Issue: https://github.com/phodal/auto-dev/issues/25
 */
object IdeaCodeActions {
    
    /**
     * Copy code to clipboard
     */
    fun copyToClipboard(code: String): Boolean {
        return try {
            val selection = StringSelection(code)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, null)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Insert code at cursor position in the currently selected editor
     * @return true if insertion was successful
     */
    fun insertAtCursor(project: Project, code: String): Boolean {
        val textEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return false
        val document = textEditor.document
        
        if (!document.isWritable) return false
        
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        val currentSelection = textEditor.selectionModel
        
        return try {
            WriteCommandAction.writeCommandAction(project).compute<Boolean, RuntimeException> {
                val offset: Int
                
                if (currentSelection.hasSelection()) {
                    offset = currentSelection.selectionStart
                    document.replaceString(currentSelection.selectionStart, currentSelection.selectionEnd, code)
                } else {
                    offset = textEditor.caretModel.offset
                    document.insertString(offset, code)
                }
                
                PsiDocumentManager.getInstance(project).commitDocument(document)
                if (psiFile != null) {
                    CodeStyleManager.getInstance(project).reformatText(psiFile, offset, offset + code.length)
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if there's a writable editor available for insertion
     */
    fun canInsertAtCursor(project: Project): Boolean {
        val textEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return false
        return textEditor.document.isWritable
    }
    
    /**
     * Save code to a new file using file chooser dialog
     */
    fun saveToFile(project: Project, code: String, suggestedFileName: String = "code.txt") {
        val descriptor = FileSaverDescriptor("Save Code", "Save code to a file")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val dir = project.baseDir
        val virtualFileWrapper = dialog.save(dir, suggestedFileName) ?: return
        
        try {
            ApplicationManager.getApplication().runWriteAction {
                val file = virtualFileWrapper.file
                file.writeText(code)
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                AutoDevNotifications.notify(project, "File saved successfully to: ${file.absolutePath}")
            }
        } catch (ex: IOException) {
            AutoDevNotifications.error(project, "Failed to save file: ${ex.message}")
        }
    }
    
    /**
     * Get suggested file name based on language
     */
    fun getSuggestedFileName(language: String): String {
        val extension = when (language.lowercase()) {
            "kotlin" -> "kt"
            "java" -> "java"
            "python" -> "py"
            "javascript", "js" -> "js"
            "typescript", "ts" -> "ts"
            "rust" -> "rs"
            "go" -> "go"
            "c" -> "c"
            "cpp", "c++" -> "cpp"
            "csharp", "c#" -> "cs"
            "ruby" -> "rb"
            "php" -> "php"
            "swift" -> "swift"
            "scala" -> "scala"
            "html" -> "html"
            "css" -> "css"
            "json" -> "json"
            "yaml", "yml" -> "yaml"
            "xml" -> "xml"
            "sql" -> "sql"
            "shell", "bash", "sh" -> "sh"
            "markdown", "md" -> "md"
            else -> "txt"
        }
        return "code.$extension"
    }
}

