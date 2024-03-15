package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.completion.BuiltinCommand
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.elementType
import kotlin.io.path.readText

class DevInCompiler(val myProject: Project, val file: DevInFile, val editor: Editor? = null) {
    private val logger = logger<DevInCompiler>()
    private val output: StringBuilder = StringBuilder()

    fun compile(): String {
        file.children.forEach {
            when (it.elementType) {
                DevInTypes.TEXT_SEGMENT -> output.append(it.text)
                DevInTypes.NEWLINE -> output.append("\n")
                DevInTypes.CODE -> {
                    output.append(it.text)
                }

                DevInTypes.USED -> {
                    processUsed(it as DevInUsed)
                }

                else -> {
                    output.append(it.text)
                    logger.warn("Unknown element type: ${it.elementType}")
                }
            }
        }

        return output.toString()
    }

    private fun processUsed(used: DevInUsed) {
        val firstChild = used.firstChild
        val id = firstChild.nextSibling

        when (firstChild.elementType) {
            DevInTypes.COMMAND_START -> {
                val command = BuiltinCommand.fromString(id?.text ?: "")
                if (command == null) {
                    output.append(used.text)
                    logger.warn("Unknown command: ${id?.text}")
                    return
                }


                val propElement = id.nextSibling?.nextSibling
                val isProp = (propElement.elementType == DevInTypes.COMMAND_PROP)
                if (!isProp) {
                    output.append(used.text)
                    logger.warn("No command prop found: ${used.text}")
                    return
                }

                processingCommand(command, propElement!!.text)
            }

            DevInTypes.AGENT_START -> {
                /**
                 * add for post action
                 */
            }

            DevInTypes.VARIABLE_START -> {
                /**
                 * Todo, call [cc.unitmesh.devti.custom.compile.VariableTemplateCompiler]
                 */
            }

            else -> {
                output.append(used.text)
                logger.warn("Unknown used type: ${firstChild.elementType}")
            }
        }
    }

    private fun processingCommand(command: BuiltinCommand, prop: @NlsSafe String) {
        when (command) {
            BuiltinCommand.FILE -> {
                val range: TextRange? = if (prop.contains("#")) {
                    val rangeStr = prop.substringAfter("#")
                    val start = rangeStr.substringBefore("-").toInt()
                    val end = rangeStr.substringAfter("-").toInt()
                    TextRange(start, end)
                } else {
                    null
                }

                val filepath = prop.trim()
                val projectPath = myProject.guessProjectDir()?.toNioPath()
                val realpath = projectPath?.resolve(filepath)
                val content = realpath?.readText()

                content?.let {
                    val virtualFile: VirtualFile? = VirtualFileManager.getInstance().findFileByNioPath(realpath)
                    val lang = virtualFile?.let {
                        PsiManager.getInstance(myProject).findFile(it)?.language?.displayName
                    } ?: ""

                    output.append("\n```$lang\n")
                    output.append(content)
                    output.append("\n```\n")
                }

            }

            BuiltinCommand.REV -> {
                logger.info("handling rev")
                output.append(command.agentName)
            }

            BuiltinCommand.SYMBOL -> {
                logger.info("handling symbol")
                output.append(command.agentName)
            }

            BuiltinCommand.WRITE -> {
                logger.info("handling write")
                output.append(command.agentName)
            }
        }
    }
}