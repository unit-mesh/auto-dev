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
 * - Automatic exclusion of build and configuration directories
 *
 * Example output:
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
        
        val files = directory.files
        val subdirectories = directory.subdirectories.filter { !isExclude(project, it) }.toList()

        // 只在深度不超过默认最大深度时显示文件
        if (depth <= defaultMaxDepth) {
            files.forEachIndexed { index, file ->
                val isLast = index == files.lastIndex && subdirectories.isEmpty()
                val prefix = if (isLast) "└" else "├"
                val size = StringUtilRt.formatFileSize(file.virtualFile.length)
                output.appendLine("${" ".repeat(depth)}$prefix── ${file.name}${size?.let { " ($it)" } ?: ""}")
            }
        }

        // 如果子目录深度超过一定值，考虑压缩显示
        if (depth > defaultMaxDepth + 1) {
            // 检查是否所有子目录都已经达到最大深度可压缩显示
            val canCompressAllSubdirs = subdirectories.all { 
                it.subdirectories.isNotEmpty() && 
                it.subdirectories.all { subdir -> 
                    !isExclude(project, subdir) && subdir.subdirectories.isEmpty() 
                }
            }
            
            if (canCompressAllSubdirs && subdirectories.isNotEmpty()) {
                // 收集所有叶节点目录名
                val compressedNames = mutableListOf<String>()
                subdirectories.forEach { subdir ->
                    val leafDirs = subdir.subdirectories.filter { !isExclude(project, it) }
                    if (leafDirs.isNotEmpty()) {
                        compressedNames.add(subdir.name)
                    }
                }
                
                if (compressedNames.isNotEmpty()) {
                    val prefix = "├"  // 这里可以根据实际情况决定是否是最后一项
                    output.appendLine("${" ".repeat(depth)}$prefix── {${compressedNames.joinToString(",")}}/")
                    return  // 不再递归显示更深层次
                }
            }
        }

        // 常规目录显示逻辑
        subdirectories.forEachIndexed { index, subdir ->
            val prefix = if (index == subdirectories.lastIndex) "└" else "├"
            output.appendLine("${" ".repeat(depth)}$prefix── ${subdir.name}/")
            
            // 判断是否需要压缩显示子目录
            if (shouldCompressChildren(project, subdir, depth + 1)) {
                compressAndDisplayChildren(project, subdir, depth + 1)
            } else {
                // 继续递归，文件显示将受深度限制
                listDirectory(project, subdir, depth + 1)
            }
        }
    }

    private fun shouldCompressChildren(project: Project, directory: PsiDirectory, depth: Int): Boolean {
        // 当深度超过阈值且子目录结构符合压缩条件时
        if (depth > defaultMaxDepth + 1) {
            val subdirs = directory.subdirectories.filter { !isExclude(project, it) }
            return subdirs.size > 1 && subdirs.all { it.subdirectories.isEmpty() }
        }
        return false
    }

    private fun compressAndDisplayChildren(project: Project, directory: PsiDirectory, depth: Int) {
        val subdirs = directory.subdirectories.filter { !isExclude(project, it) }
        if (subdirs.isEmpty()) return
        
        val subdirNames = subdirs.map { it.name }
        val prefix = "├"
        output.appendLine("${" ".repeat(depth)}$prefix── {${subdirNames.joinToString(",")}}/")
    }

    private fun isExclude(project: Project, directory: PsiDirectory): Boolean {
        if (directory.name == ".idea" ||
            directory.name == "build" ||
            directory.name == "target" ||
            directory.name == ".gradle" ||
            directory.name == "node_modules") return true

        val status = FileStatusManager.getInstance(project).getStatus(directory.virtualFile)
        return status == FileStatus.IGNORED
    }
}
