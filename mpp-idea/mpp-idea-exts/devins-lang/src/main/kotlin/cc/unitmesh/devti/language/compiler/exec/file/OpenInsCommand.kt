package cc.unitmesh.devti.language.compiler.exec.file

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class OpenInsCommand(val myProject: Project, private val filename: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.OPEN

    override suspend fun execute(): String? {
        val file = myProject.lookupFile(filename)
        if (file != null) {
            FileEditorManager.getInstance(myProject).openFile(file, true)
        }

        return "Opening $filename..."
    }
}
