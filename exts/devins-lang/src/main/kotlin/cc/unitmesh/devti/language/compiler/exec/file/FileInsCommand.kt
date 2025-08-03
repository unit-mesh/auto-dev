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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.runReadAction

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
                return "File not found: $prop"
            }
        }

        val content = try {
            virtualFile?.readText()
        } catch (e: Exception) {
            null
        }

        if (virtualFile == null || content == null) {
            AutoDevNotifications.warn(myProject, "File not found: $prop")
            return "File not found: $prop"
        }

        InsCommandListener.notify(this, InsCommandStatus.SUCCESS, virtualFile)

        val lang = runReadAction {
            PsiManager.getInstance(myProject).findFile(virtualFile)?.language?.displayName ?: ""
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

