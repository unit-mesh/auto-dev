package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager


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
    private val defaultMaxDepth = 2

    private val output = StringBuilder()

    override suspend fun execute(): String? {
        val virtualFile = myProject.lookupFile(dir) ?: return "File not found: $dir"
        val psiDirectory = PsiManager.getInstance(myProject).findDirectory(virtualFile) ?: return null

        output.appendLine("$dir/")
        runReadAction { listDirectory(myProject, psiDirectory, 1) }

        return output.toString()
    }

    private fun listDirectory(project: Project, directory: PsiDirectory, depth: Int) {
        if(isExclude(project, directory)) return
        if (depth > defaultMaxDepth && !shouldContinueForPath(directory)) return

        val files = directory.files
        val subdirectories = directory.subdirectories

        val items = files.map { it.name to StringUtilRt.formatFileSize(it.virtualFile.length) } +
                subdirectories.map { it.name + "/" to null }

        items.forEachIndexed { index, (name, size) ->
            val prefix = if (index == items.lastIndex) "└" else "├"
            output.appendLine("${" ".repeat(depth)}$prefix $name${size?.let { " ($it)" } ?: ""}")
            if (size == null) listDirectory(project, subdirectories[index - files.size], depth + 1)
        }
    }

    /// todo: replace to intellij source content dir
    private fun shouldContinueForPath(directory: PsiDirectory): Boolean {
        // 如果是 src 目录，检查是否是单路径结构
        if (directory.name == "src" || directory.parent?.name == "src") {
            val subdirs = directory.subdirectories
            // 对于只有一个子目录的情况，继续遍历
            return subdirs.size == 1
        }
        return false
    }

    private fun isExclude(project: Project, directory: PsiDirectory): Boolean {
        if (directory.name == ".idea" ||
            directory.name == "build" ||
            directory.name == "target" ||
            directory.name == "node_modules") return true

        val status = FileStatusManager.getInstance(project).getStatus(directory.virtualFile)
        return status == FileStatus.IGNORED
    }
}
