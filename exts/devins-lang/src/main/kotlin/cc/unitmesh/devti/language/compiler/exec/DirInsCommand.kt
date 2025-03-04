package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern


/**
 * The `DirInsCommand` class is responsible for listing files and directories in a tree-like structure for a given directory path within a project.
 * It implements the `InsCommand` interface and provides an `execute` method to perform the directory listing operation asynchronously.
 *
 * The tree structure is visually represented using indentation and symbols (`├──`, `└──`) to denote files and subdirectories. Files are listed
 * first, followed by subdirectories, which are recursively processed to display their contents.
 *
 * Example output:
 * ```
 * myDirectory/
 *   ├── file1.txt
 *   ├── file2.txt
 *   └── subDirectory/
 *       ├── file3.txt
 *       └── subSubDirectory/
 *           └── file4.txt
 * ```
 *
 * About depth design:
 * In Java Langauge, the depth of dir are very long
 * In JavaScript Langauge, the dirs files are too many
 *
 * @param myProject The project instance in which the directory resides.
 * @param dir The path of the directory to list.
 */
class DirInsCommand(private val myProject: Project, private val dir: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.DIR
    private val HASH_FILE_PATTERN: Pattern = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(?:\\.json|@[0-9a-f]+\\.json)$",
        Pattern.CASE_INSENSITIVE
    )

    fun isHashJson(file: VirtualFile?): Boolean {
        return file != null && HASH_FILE_PATTERN.matcher(file.name).matches()
    }

    private val output = StringBuilder()
    private val maxLength = 2

    override suspend fun execute(): String? {
        val virtualFile = myProject.lookupFile(dir) ?: return "File not found: $dir"
        val future = CompletableFuture<String>()
        val task = object : Task.Backgroundable(myProject, "Processing context", false) {
            override fun run(indicator: ProgressIndicator) {
                val psiDirectory = runReadAction {
                    PsiManager.getInstance(myProject!!).findDirectory(virtualFile)
                }

                if (psiDirectory == null) {
                    future.complete("Directory not found: $dir")
                    return
                }

                output.appendLine("$dir/")
                runReadAction { listDirectory(myProject!!, psiDirectory, 1) }
                future.complete(
                    "Here is the directory tree (depth = 2) for $dir:\n$output"
                )
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        return future.get()
    }

    private fun listDirectory(project: Project, directory: PsiDirectory, depth: Int) {
        if (depth > maxLength || isExclude(project, directory)) return

        val files = directory.files.filter { !it.fileType.isBinary && !isHashJson(it.virtualFile) }
        val subdirectories = directory.subdirectories

        val items = files.map { it.name to StringUtilRt.formatFileSize(it.virtualFile.length) } +
                subdirectories.map { it.name + "/" to null }

        items.forEachIndexed { index, (name, size) ->
            val prefix = if (index == items.lastIndex) "└" else "├"
            output.appendLine("${" ".repeat(depth)}$prefix $name${size?.let { " ($it)" } ?: ""}")
            if (size == null) listDirectory(project, subdirectories[index - files.size], depth + 1)
        }
    }

    private fun isExclude(project: Project, directory: PsiDirectory): Boolean {
        if (directory.name == ".idea") return true

        val status = FileStatusManager.getInstance(project).getStatus(directory.virtualFile)
        return status == FileStatus.IGNORED
    }
}

