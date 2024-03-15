package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.completion.BuiltinCommand
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.util.elementType

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

                processingCommand(command, propElement!!.text, fallbackText = used.text)
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

    private fun processingCommand(command: BuiltinCommand, prop: String, fallbackText: String) {
        when (command) {
            BuiltinCommand.FILE -> {
                val result = FileAutoCommand(myProject, prop).execute() ?: fallbackText
                output.append(result)
            }

            BuiltinCommand.REV -> {
                val result = RevAutoCommand(myProject, prop).execute() ?: fallbackText
                output.append(result)
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