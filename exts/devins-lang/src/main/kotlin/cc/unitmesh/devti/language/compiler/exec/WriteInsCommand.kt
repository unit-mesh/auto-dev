package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.model.LineInfo
import cc.unitmesh.devti.language.psi.DevInUsed
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class WriteInsCommand(val myProject: Project, val argument: String, val content: String, val used: DevInUsed) :
    InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.WRITE

    private val pathSeparator = "/"

    override suspend fun execute(): String? {
        val range: LineInfo? = LineInfo.fromString(runReadAction { used.text })
        val filepath = argument.split("#")[0]
        val projectDir = myProject.guessProjectDir() ?: return "$DEVINS_ERROR: Project directory not found"

        val virtualFile = runReadAction { myProject.lookupFile(filepath) }
        if (virtualFile == null) {
            return writeToFile(filepath, projectDir)
        }

        val psiFile = runReadAction { PsiManager.getInstance(myProject).findFile(virtualFile) }
            ?: return "$DEVINS_ERROR: File not found: $argument"

        var output: String? = null
        runInEdt {
            FileEditorManager.getInstance(myProject).openFile(virtualFile, true)
            output = executeInsert(psiFile, range, content)
        }
        return output
    }

    private fun writeToFile(filepath: String, projectDir: VirtualFile): String {
        val hasChildPath = filepath.contains(pathSeparator)
        if (!hasChildPath) {
            return createNewContent(projectDir, filepath, content) ?: "$DEVINS_ERROR: Create File failed: $argument"
        }

        val parentPath = filepath.substringBeforeLast(pathSeparator)
        var parentDir = projectDir.findChild(parentPath)
        if (parentDir == null) {
            val parentDirs = parentPath.split(pathSeparator)
            parentDir = projectDir
            for (dir in parentDirs) {
                if (dir.isEmpty()) continue

                if (parentDir?.findChild(dir) == null) {
                    var parentDir: VirtualFile? = null
                    runInEdt {
                        runWriteAction {
                            parentDir = parentDir?.createChildDirectory(this, dir)
                        }
                    }
                } else {
                    parentDir = parentDir.findChild(dir)
                }
            }

            if (parentDir == null) {
                return "$DEVINS_ERROR: Create Directory failed: $parentPath"
            }
        }

        return createNewContent(parentDir, filepath, content) ?: "$DEVINS_ERROR: Create File failed: $argument"
    }

    private fun createNewContent(parentDir: VirtualFile, filepath: String, content: String): String? {
        var newFile: VirtualFile? = null
        var result: String? = null
        runInEdt {
            return@runInEdt WriteCommandAction.runWriteCommandAction(myProject) {
                val name = filepath.substringAfterLast(pathSeparator)
                if (name.isEmpty()) {
                    return@runWriteCommandAction
                }

                newFile = parentDir.createChildData(this, name)

                val document = runReadAction { FileDocumentManager.getInstance().getDocument(newFile) }
                if (document == null) {
                    result = "$DEVINS_ERROR: Document not found: $argument"
                    return@runWriteCommandAction
                }

                FileEditorManager.getInstance(myProject).openFile(newFile, true)
                document.setText(content)

                result = "Writing to file: $argument"
            }
        }

        return result
    }

    private fun executeInsert(
        psiFile: PsiFile,
        range: LineInfo?,
        content: String
    ): String {
        val document = runReadAction {
            PsiDocumentManager.getInstance(myProject).getDocument(psiFile)
        } ?: return "$DEVINS_ERROR: File not found: $argument"

        val startLine = range?.startLine ?: 0
        val endLine = if (document.lineCount == 0) 1 else range?.endLine ?: document.lineCount

        try {
            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine - 1)
            WriteCommandAction.runWriteCommandAction(myProject) {
                document.replaceString(startOffset, endOffset, content)
            }

            return "Writing to file: $argument"
        } catch (e: Exception) {
            return "$DEVINS_ERROR: ${e.message}"
        }
    }
}
