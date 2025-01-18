package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
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
 * @param myProject The project instance in which the directory resides.
 * @param dir The path of the directory to list.
 */
class DirInsCommand(private val myProject: Project, private val dir: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.DIR

    private val output = StringBuilder()

    override suspend fun execute(): String? {
        val virtualFile = myProject.lookupFile(dir) ?: return "File not found: $dir"
        val psiDirectory = PsiManager.getInstance(myProject).findDirectory(virtualFile) ?: return null

        output.appendLine("$dir/")
        runReadAction { listDirectory(myProject, psiDirectory, 1) }

        return output.toString()
    }

    private fun listDirectory(project: Project, directory: PsiDirectory, depth: Int) {
        val isExclude = ProjectFileIndex.getInstance(project).isUnderIgnored(directory.virtualFile)
        if (isExclude) {
            return
        }

        val files = directory.files
        val subdirectories = directory.subdirectories

        for ((index, file) in files.withIndex()) {
            if (index == files.size - 1) {
                output.appendLine("${"  ".repeat(depth)}└── ${file.name}")
            } else {
                output.appendLine("${"  ".repeat(depth)}├── ${file.name}")
            }
        }

        for ((index, subdirectory) in subdirectories.withIndex()) {
            if (index == subdirectories.size - 1) {
                output.appendLine("${"  ".repeat(depth)}└── ${subdirectory.name}/")
            } else {
                output.appendLine("${"  ".repeat(depth)}├── ${subdirectory.name}/")
            }
            listDirectory(project, subdirectory, depth + 1)
        }
    }
}

