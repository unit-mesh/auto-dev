package cc.unitmesh.devti.language.compiler.exec

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class OpenInsCommand(val myProject: Project, private val filename: String) : InsCommand {
    override suspend fun execute(): String? {
        val editor = FileEditorManager.getInstance(myProject).selectedTextEditor
        val currentFile = if (editor != null) {
            FileDocumentManager.getInstance().getFile(editor.document)!!
        } else {
            FileEditorManager.getInstance(myProject).selectedFiles.first()
        }

        return currentFile.name
    }
}
