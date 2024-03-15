package cc.unitmesh.devti.language.compiler

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager

class FileAutoCommand(private val myProject: Project, private val prop: String) : AutoCommand {
    private val logger = logger<FileAutoCommand>()
    private val output = StringBuilder()

    override fun execute(): String? {
        val range: TextRange? = if (prop.contains("#")) {
            val rangeStr = prop.substringAfter("#")
            val start = rangeStr.substringBefore("-").toInt()
            val end = rangeStr.substringAfter("-").toInt()
            TextRange(start, end)
        } else {
            null
        }

        val projectPath = myProject.guessProjectDir()?.toNioPath()
        val realpath = projectPath?.resolve(prop.trim())

        val virtualFile =
            VirtualFileManager.getInstance().findFileByUrl("file://${realpath?.toAbsolutePath()}")

        val contentsToByteArray = virtualFile?.contentsToByteArray()
        if (contentsToByteArray == null) {
            logger.warn("File not found: $realpath")
            return null
        }

        contentsToByteArray.let {
            val lang = virtualFile.let {
                PsiManager.getInstance(myProject).findFile(it)?.language?.displayName
            } ?: ""

            val content = it.toString(Charsets.UTF_8)
            val fileContent = if (range != null) {
                val subContent = try {
                    content.substring(range.startOffset, range.endOffset)
                } catch (e: StringIndexOutOfBoundsException) {
                    content
                }

                subContent
            } else {
                content
            }

            output.append("\n```$lang\n")
            output.append(fileContent)
            output.append("\n```\n")
        }

        return output.toString()
    }
}