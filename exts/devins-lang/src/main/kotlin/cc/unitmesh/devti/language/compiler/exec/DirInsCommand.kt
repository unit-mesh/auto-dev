package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.utils.lookupFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager

/**
 *  Dir List files and directories in a tree-like structure
 */
class DirInsCommand(private val myProject: Project, private val dir: String) : InsCommand {
    private val output = StringBuilder()

    override suspend fun execute(): String? {
        val virtualFile = myProject.lookupFile(dir) ?: return "File not found: $dir"
        val psiDirectory = PsiManager.getInstance(myProject).findDirectory(virtualFile) ?: return null

        output.appendLine("$dir/")
        listDirectory(psiDirectory, 1)

        return output.toString()
    }

    private fun listDirectory(directory: PsiDirectory, depth: Int) {
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
            listDirectory(subdirectory, depth + 1)
        }
    }
}

