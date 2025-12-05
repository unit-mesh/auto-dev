package cc.unitmesh.devti.language.compiler.exec.file

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.psi.DevInUsed
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.ui.patch.writeText
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import kotlin.text.isNotEmpty

class WriteInsCommand(val myProject: Project, val argument: String, val content: String, val used: DevInUsed) :
    InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.WRITE

    override suspend fun execute(): String? {
        val filepath = argument.split("#")[0]
        val projectDir = myProject.guessProjectDir() ?: return "$DEVINS_ERROR: Project directory not found"

        val fileExists = runReadAction {
            myProject.lookupFile(filepath) != null
        }

        if (!fileExists) {
            return writeToFile(filepath, projectDir)
        }

        val virtualFile = runReadAction { myProject.lookupFile(filepath) }!!
        runInEdt {
            virtualFile.writeText(content)
            if (!AutoSketchMode.getInstance(myProject).isEnable) {
                FileEditorManager.getInstance(myProject).openFile(virtualFile, true)
            }
        }

        return "Writing to file: $argument"
    }

    private fun writeToFile(filepath: String, projectDir: VirtualFile): String {
        val hasChildPath = filepath.contains(pathSeparator)
        if (!hasChildPath) {
            return runWriteAction {
                createFileWithContent(projectDir, filepath)
            } ?: "$DEVINS_ERROR: Create File failed: $argument"
        }

        val filename = filepath.substringAfterLast(pathSeparator)
        val dirPath = filepath.substringBeforeLast(pathSeparator)

        return runWriteAction {
            val currentDir = getOrCreateDirectory(projectDir, dirPath)
            createFileWithContent(currentDir, filename)
        } ?: ""
    }

    private fun runWriteAction(action: () -> String): String? {
        var result: String? = null
        val disposable = Disposer.newCheckedDisposable()
        runInEdtAsync(disposable) {
            result = WriteCommandAction.runWriteCommandAction<String>(myProject, action)
        }

        return result
    }

    private fun createFileWithContent(parentDir: VirtualFile, fileName: String): String {
        if (fileName.isEmpty()) return "$DEVINS_ERROR: File name is empty: $argument"

        try {
            val newFile = parentDir.createChildData(this, fileName)
            newFile.writeText(content)
            return "Writing to file: $argument"
        } catch (e: Exception) {
            return "$DEVINS_ERROR: ${e.message}"
        }
    }

    companion object {
        private val pathSeparator = "/"
        fun getOrCreateDirectory(baseDir: VirtualFile, path: String): VirtualFile {
            var currentDir = baseDir
            val pathSegments = path.split(pathSeparator).filter { it.isNotEmpty() }

            for (segment in pathSegments) {
                val childDir = currentDir.findChild(segment)
                currentDir = childDir ?: currentDir.createChildDirectory(this, segment)
            }

            return currentDir
        }
    }
}

fun runInEdtAsync(disposable: CheckedDisposable, action: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(action) { disposable.isDisposed }
}

