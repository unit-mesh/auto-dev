package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.compiler.data.LineInfo
import cc.unitmesh.devti.language.compiler.utils.lookupFile
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

class FileAutoCommand(private val myProject: Project, private val prop: String) : AutoCommand {
    private val logger = logger<FileAutoCommand>()
    private val output = StringBuilder()

    override fun execute(): String? {
        val range: LineInfo? = LineInfo.fromString(prop)

        val virtualFile = myProject.lookupFile(prop.trim())

        val contentsToByteArray = virtualFile?.contentsToByteArray()
        if (contentsToByteArray == null) {
            logger.warn("File not found: $virtualFile")
            return null
        }

        contentsToByteArray.let {
            val lang = virtualFile.let {
                PsiManager.getInstance(myProject).findFile(it)?.language?.displayName
            } ?: ""

            val content = it.toString(Charsets.UTF_8)
            val fileContent = if (range != null) {
                val subContent = try {
                    content.substring(range.startLine, range.endLine)
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

