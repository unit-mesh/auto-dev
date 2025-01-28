package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.InsCommandListener
import cc.unitmesh.devti.devin.InsCommandStatus
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.model.LineInfo
import cc.unitmesh.devti.language.utils.findFile
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.sketch.ui.patch.readText
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

/**
 * FileInsCommand is responsible for reading a file and returning its contents.
 *
 * @param myProject the Project in which the file operations are performed
 * @param prop the property string containing the file name and optional line range
 *
 */
class FileInsCommand(private val myProject: Project, private val prop: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.FILE
    private val output = StringBuilder()

    override suspend fun execute(): String? {
        val range: LineInfo? = LineInfo.fromString(prop)

        // prop name can be src/file.name#L1-L2
        val filepath = prop.split("#")[0]
        var virtualFile: VirtualFile? = myProject.lookupFile(filepath)

        if (virtualFile == null) {
            val filename = filepath.split("/").last()
            virtualFile = myProject.findFile(filename, false)
        }

        val content = virtualFile?.readText()
        if (content == null) {
            AutoDevNotifications.warn(myProject, "File not found: $prop")
            /// not show error message to just notify
            return "File not found: $prop"
        }

        InsCommandListener.notify(this, InsCommandStatus.SUCCESS, virtualFile)

        val lang = PsiManager.getInstance(myProject).findFile(virtualFile)?.language?.displayName ?: ""

        val fileContent = if (range != null) {
            val subContent = try {
                content.split("\n").slice(range.startLine - 1 until range.endLine)
                    .joinToString("\n")
            } catch (e: StringIndexOutOfBoundsException) {
                content
            }

            subContent
        } else {
            content
        }

        // add file path
        output.append("// File: $prop\n")
        output.append("\n```$lang\n")
        output.append(fileContent)
        output.append("\n```\n")

        return output.toString()
    }
}

