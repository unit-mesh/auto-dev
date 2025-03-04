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
 * The `DirInsCommand` class provides a tree-like directory listing with smart depth control and file size information.
 * It implements the `InsCommand` interface for directory exploration within a project.
 *
 * Features:
 * - Default depth limit of 2 levels for cleaner output
 * - Smart depth handling for Java-like source directories
 * - File size display for regular files
 * - Compact mode for directory display beyond depth limit
 * - Automatic exclusion of build and configuration directories
 *
 * Example output with compact mode:
 * ```
 * myDirectory/
 *   ├── README.md (2.5 KB)
 *   ├── package.json (1.2 KB)
 *   ├── config.xml (0.5 KB)
 *   └── src/{main,test,resources}/
 * ```
 *
 * Display rules:
 * - Files are always shown completely with their sizes
 * - Directories beyond depth limit are shown in compact format {dir1,dir2,dir3}
 * - For src directories:
 *   - If only one subdirectory exists, traversal continues beyond max depth
 *   - Helps handle deep Java package structures efficiently
 *
 * Excluded directories:
 * - .idea/
 * - build/
 * - target/
 * - node_modules/
 * - Any directory marked as ignored by VCS
 *
 * @param myProject The project instance in which the directory resides
 * @param dir The path of the directory to list
 */
class DirInsCommand(private val myProject: Project, private val dir: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.DIR
    private val defaultMaxDepth = 2
    private val compactMode = true

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

        // 始终完整显示文件
        files.forEachIndexed { index, file ->
            val isLast = index == files.lastIndex && subdirectories.isEmpty()
            val prefix = if (isLast) "└" else "├"
            val size = StringUtilRt.formatFileSize(file.virtualFile.length)
            output.appendLine("${" ".repeat(depth)}$prefix── ${file.name}${size?.let { " ($it)" } ?: ""}")
        }

        // 处理目录
        if (subdirectories.isNotEmpty()) {
            if (compactMode && depth >= defaultMaxDepth) {
                // 紧凑模式下只显示目录列表
                val dirNames = subdirectories
                    .filter { !isExclude(project, it) }
                    .map { it.name }
                if (dirNames.isNotEmpty()) {
                    output.appendLine("${" ".repeat(depth)}└── {${dirNames.joinToString(",")}}/")
                }
            } else {
                subdirectories.forEachIndexed { index, subdir ->
                    if (isExclude(project, subdir)) return@forEachIndexed
                    val prefix = if (index == subdirectories.lastIndex) "└" else "├"
                    output.appendLine("${" ".repeat(depth)}$prefix── ${subdir.name}/")
                    listDirectory(project, subdir, depth + 1)
                }
            }
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
