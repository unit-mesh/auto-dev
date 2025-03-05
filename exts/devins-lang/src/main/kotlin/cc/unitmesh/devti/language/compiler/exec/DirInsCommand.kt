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
import com.intellij.psi.PsiFile
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

    // 定义表示目录树节点的数据模型
    private sealed class TreeNode {
        abstract val name: String

        // 文件节点，包含文件大小信息
        data class FileNode(override val name: String, val size: String?) : TreeNode()

        // 目录节点，包含子节点列表
        data class DirectoryNode(
            override val name: String, val children: MutableList<TreeNode> = mutableListOf()
        ) : TreeNode() {
            // 添加子节点的便捷方法
            fun addChild(child: TreeNode) {
                children.add(child)
            }
        }

        // 压缩目录节点，用于显示多个同层次目录
        data class CompressedNode(override val name: String, val subdirNames: List<String>) : TreeNode()
    }

    override suspend fun execute(): String? {
        val virtualFile = myProject.lookupFile(dir) ?: return "File not found: $dir"
        val psiDirectory = PsiManager.getInstance(myProject).findDirectory(virtualFile) ?: return null

        // 第一步：构建目录树模型
        val rootNode = runReadAction {
            buildDirectoryTree(myProject, psiDirectory, 1)
        } ?: return null

        // 第二步：将树模型转换为文本表示
        val output = StringBuilder().apply {
            appendLine("$dir/")
            renderTree(rootNode, 1, this)
        }

        return output.toString()
    }

    /**
     * 构建目录树的数据模型
     */
    private fun buildDirectoryTree(project: Project, directory: PsiDirectory, depth: Int): TreeNode.DirectoryNode? {
        if (isExcluded(project, directory)) return null

        val dirNode = TreeNode.DirectoryNode(directory.name)

        // 添加文件节点（受深度限制）
        if (depth <= defaultMaxDepth) {
            directory.files.forEach { file ->
                val fileSize = StringUtilRt.formatFileSize(file.virtualFile.length)
                dirNode.addChild(TreeNode.FileNode(file.name, fileSize))
            }
        }

        // 添加目录节点
        val subdirectories = directory.subdirectories.filter { !isExcluded(project, it) }

        // 检查是否应该压缩显示子目录
        if (shouldCompressSubdirectories(project, directory, subdirectories, depth)) {
            // 获取可以压缩的子目录列表
            val compressableSubdirs = getCompressableSubdirectories(subdirectories)
            if (compressableSubdirs.isNotEmpty()) {
                dirNode.addChild(TreeNode.CompressedNode("compressed", compressableSubdirs.map { it.name }))
            }
        } else {
            // 常规递归处理子目录
            subdirectories.forEach { subdir ->
                buildDirectoryTree(project, subdir, depth + 1)?.let { subdirNode ->
                    dirNode.addChild(subdirNode)
                }
            }
        }

        return dirNode
    }

    /**
     * 判断是否应该压缩显示子目录
     */
    private fun shouldCompressSubdirectories(
        project: Project, directory: PsiDirectory, subdirectories: List<PsiDirectory>, depth: Int): Boolean {
        // 深度超过阈值且有多个子目录时考虑压缩
        return depth > defaultMaxDepth + 1 && subdirectories.size > 1 &&
                // 确保这些子目录大多是叶子节点或近似叶子节点
                subdirectories.all { subdir ->
                    val childDirs = subdir.subdirectories.filter { !isExcluded(project, it) }
                    childDirs.isEmpty() || childDirs.all { it.subdirectories.isEmpty() }
                }
    }

    /**
     * 获取可以压缩显示的子目录
     */
    private fun getCompressableSubdirectories(subdirectories: List<PsiDirectory>): List<PsiDirectory> {
        // 这里可以添加更复杂的逻辑来决定哪些目录可以压缩
        return subdirectories
    }

    /**
     * 将目录树渲染为文本输出
     */
    private fun renderTree(node: TreeNode, depth: Int, output: StringBuilder) {
        val indent = " ".repeat(depth)

        when (node) {
            is TreeNode.DirectoryNode -> {
                // 目录节点的子节点渲染
                node.children.forEachIndexed { index, child ->
                    val isLast = index == node.children.lastIndex
                    val prefix = if (isLast) "└" else "├"

                    when (child) {
                        is TreeNode.FileNode -> {
                            val sizeInfo = child.size?.let { " ($it)" } ?: ""
                            output.appendLine("$indent$prefix── ${child.name}$sizeInfo")
                        }

                        is TreeNode.DirectoryNode -> {
                            output.appendLine("$indent$prefix── ${child.name}/")
                            renderTree(child, depth + 1, output)
                        }

                        is TreeNode.CompressedNode -> {
                            output.appendLine("$indent$prefix── {${child.subdirNames.joinToString(",")}}/")
                        }
                    }
                }
            }

            else -> {} // 其他类型节点在这里不需要单独处理
        }
    }

    /**
     * 判断目录是否应被排除
     */
    private fun isExcluded(project: Project, directory: PsiDirectory): Boolean {
        val excludedDirs = setOf(".idea", "build", "target", ".gradle", "node_modules")
        if (directory.name in excludedDirs) return true

        val status = FileStatusManager.getInstance(project).getStatus(directory.virtualFile)
        return status == FileStatus.IGNORED
    }
}
