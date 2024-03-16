package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.completion.BuiltinCommand
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.elementType

data class CompileResult(
    var output: String = "",
    var isLocalCommand: Boolean = false,
)

class DevInsCompiler(val myProject: Project, val file: DevInFile, val editor: Editor? = null) {
    private val logger = logger<DevInsCompiler>()
    private val result = CompileResult()
    private val output: StringBuilder = StringBuilder()

    fun compile(): CompileResult {
        file.children.forEach {
            when (it.elementType) {
                DevInTypes.TEXT_SEGMENT -> output.append(it.text)
                DevInTypes.NEWLINE -> output.append("\n")
                // todo: add lazy to process code
                DevInTypes.CODE -> output.append(it.text)
                DevInTypes.USED -> processUsed(it as DevInUsed)
                else -> {
                    output.append(it.text)
                    logger.warn("Unknown element type: ${it.elementType}")
                }
            }
        }

        result.output = output.toString()
        return result
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
                logger.warn("Unknown [cc.unitmesh.devti.language.psi.DevInUsed] type: ${firstChild.elementType}")
                output.append(used.text)
            }
        }
    }

    private fun processingCommand(commandNode: BuiltinCommand, prop: String, fallbackText: String) {
        val command: AutoCommand = when (commandNode) {
            BuiltinCommand.FILE -> {
                FileAutoCommand(myProject, prop)
            }

            BuiltinCommand.REV -> {
                RevAutoCommand(myProject, prop)
            }

            BuiltinCommand.SYMBOL -> {
                PrintAutoCommand("/" + commandNode.agentName + ":" + prop)
            }

            BuiltinCommand.WRITE -> {
                result.isLocalCommand = true
                PrintAutoCommand("/" + commandNode.agentName + ":" + prop)
            }

            BuiltinCommand.PATCH -> {
                result.isLocalCommand = true
                PrintAutoCommand("/" + commandNode.agentName + ":" + prop)
            }

            BuiltinCommand.RUN -> {
                result.isLocalCommand = true
                RunAutoCommand(myProject, prop)
            }
        }

        val result = command.execute() ?: fallbackText
        output.append(result)
    }

}