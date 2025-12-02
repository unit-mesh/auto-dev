package cc.unitmesh.devti.language.compiler.exec.file

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.InsCommandListener
import cc.unitmesh.devti.command.InsCommandStatus
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.model.LineInfo
import cc.unitmesh.devti.language.utils.findFile
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.util.relativePath
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

/**
 * FileInsCommand is responsible for reading a file and returning its contents.
 *
 * @param myProject the Project in which the file operations are performed
 * @param prop the property string containing the file name and optional line range
 *
 * In JetBrins Junie early version, only return 300 lines of file content with a comments for others.
 * In Cursor and Windsurf, will fetch 30 lines and structure the content with a code block.
 */
class FileInsCommand(private val myProject: Project, private val prop: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.FILE
    private val MAX_LINES = 300

    override suspend fun execute(): String? {
        val range: LineInfo? = LineInfo.fromString(prop)

        // prop name can be src/file.name#L1-L2
        val filepath = prop.split("#")[0]
        var virtualFile: VirtualFile? = myProject.lookupFile(filepath)

        if (virtualFile == null) {
            val filename = filepath.split("/").last()
            try {
                virtualFile = runReadAction { myProject.findFile(filename, false) }
            } catch (e: Exception) {
                // Ignore and continue to search
            }
        }

        // If still not found, try to search in dependencies (JAR files) or use Search Everywhere API
        if (virtualFile == null) {
            virtualFile = searchInDependencies(filepath)
        }

        if (virtualFile == null) {
            AutoDevNotifications.warn(myProject, "File not found: $prop")
            return "File not found: $prop"
        }

        InsCommandListener.notify(this, InsCommandStatus.SUCCESS, virtualFile)

        val (content, lang) = runReadAction {
            val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile)
            val language = psiFile?.language?.displayName ?: ""

            val fileContent = when (psiFile) {
                is PsiCompiledFile -> {
                    // For compiled files (like .class files), get the decompiled text
                    psiFile.decompiledPsiFile?.text ?: virtualFile.readText()
                }

                else -> {
                    virtualFile.readText()
                }
            }

            Pair(fileContent, language)
        }

        val fileContent = splitLines(range, content)

        val realPath = virtualFile.relativePath(myProject)

        val output = StringBuilder()
        output.append("## file: $realPath")
        output.append("\n```$lang\n")
        output.append(fileContent)
        output.append("\n```\n")
        return output.toString()
    }

    /**
     * Search for file in dependencies (JAR files) using various strategies:
     * 1. Search by class name using PsiShortNamesCache
     * 2. Search by filename using FilenameIndex with allScope
     * 3. Try to interpret as a fully qualified class name
     */
    private fun searchInDependencies(filepath: String): VirtualFile? {
        return runReadAction {
            val filename = filepath.split("/").last()

            // Strategy 1: If it looks like a class name (ends with .java, .kt, etc.), search in all scope
            if (filename.contains(".")) {
                val files = FilenameIndex.getVirtualFilesByName(
                    filename,
                    GlobalSearchScope.allScope(myProject)
                )
                val matchedFile = files.firstOrNull { virtualFile ->
                    virtualFile.path.endsWith(filepath) || virtualFile.path.contains(filepath)
                }
                if (matchedFile != null) {
                    return@runReadAction matchedFile
                }
                if (files.isNotEmpty()) {
                    return@runReadAction files.first()
                }
            }
            // Strategy 2: Try to search as a class name (e.g., "String" or "java.lang.String")
            val className = if (filename.contains(".")) {
                filename.substringBeforeLast(".")
            } else {
                filename
            }

            // Search by short class name
            val psiClasses = PsiShortNamesCache.getInstance(myProject)
                .getClassesByName(className, GlobalSearchScope.allScope(myProject))

            if (psiClasses.isNotEmpty()) {
                // Try to match by full path if possible
                val matchedClass = psiClasses.firstOrNull { psiClass ->
                    val qualifiedName = psiClass.qualifiedName ?: ""
                    qualifiedName.replace(".", "/").contains(filepath.replace(".", "/"))
                }

                return@runReadAction (matchedClass ?: psiClasses.first()).containingFile?.virtualFile
            }

            // Strategy 3: Try to interpret as fully qualified class name (e.g., "java.lang.String")
            if (filepath.contains(".") && !filename.contains("/")) {
                val scope = GlobalSearchScope.allScope(myProject)
                val fullyQualifiedName = filepath.substringBeforeLast(".java")
                    .substringBeforeLast(".kt")
                    .replace("/", ".")

                val classes = PsiShortNamesCache.getInstance(myProject)
                    .getClassesByName(fullyQualifiedName.substringAfterLast("."), scope)

                val matchedClass = classes.firstOrNull { it.qualifiedName == fullyQualifiedName }
                if (matchedClass != null) {
                    return@runReadAction matchedClass.containingFile?.virtualFile
                }
            }

            null
        }
    }

    private fun splitLines(range: LineInfo?, content: String): String {
        val lines = content.lines()
        val currentSize = lines.size
        return if (range == null) {
            limitMaxSize(currentSize, content)
        } else {
            val endLine = minOf(range.endLine, currentSize)
            lines.slice(range.startLine - 1 until endLine)
                .joinToString("\n")
        }
    }

    private fun limitMaxSize(size: Int, content: String): String {
        return if (size > MAX_LINES) {
            val code = content.split("\n")
                .slice(0 until MAX_LINES)
                .joinToString("\n")

            // 计算合理的下一块行范围建议
            val nextChunkStart = MAX_LINES + 1
            val nextChunkEnd = minOf(size, MAX_LINES * 2)

            val suggestion = if (nextChunkEnd > nextChunkStart) {
                "\nUse `filename#L${nextChunkStart}-L${nextChunkEnd}` to get next chunk of lines."
            } else ""

            "File too long, only showing first $MAX_LINES lines of $size total lines.\n$code$suggestion"
        } else {
            content
        }
    }
}

