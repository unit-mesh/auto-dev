package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.completion.BuiltinCommand
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.elementType

class DevInCompiler(val project: Project, val file: DevInFile, val editor: Editor) {
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
        val id = used.children.getOrNull(1)

        when (firstChild.elementType) {
            DevInTypes.COMMAND_START -> {
                /**
                 *
                 */

                val command = BuiltinCommand.fromString(id?.text ?: "")
                if (command == null) {
                    output.append(used.text)
                    logger.warn("Unknown command: ${id?.text}")
                    return
                }

                when (command) {
                    BuiltinCommand.FILE -> TODO()
                    BuiltinCommand.REV -> TODO()
                    BuiltinCommand.SYMBOL -> TODO()
                    BuiltinCommand.WRITE -> TODO()
                }
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
}