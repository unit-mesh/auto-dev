package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.compiler.exec.*
import cc.unitmesh.devti.language.completion.BuiltinCommand
import cc.unitmesh.devti.language.parser.CodeBlockElement
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

data class CompileResult(
    var output: String = "",
    var isLocalCommand: Boolean = false,
    var hasError: Boolean = false
)

class DevInsCompiler(private val myProject: Project, val file: DevInFile, val editor: Editor? = null) {
    private var skipNextCode: Boolean = false
    private val logger = logger<DevInsCompiler>()
    private val result = CompileResult()
    private val output: StringBuilder = StringBuilder()

    /**
     * Todo: build AST tree, then compile
     */
    fun compile(): CompileResult {
        file.children.forEach {
            when (it.elementType) {
                DevInTypes.TEXT_SEGMENT -> output.append(it.text)
                DevInTypes.NEWLINE -> output.append("\n")
                DevInTypes.CODE -> {
                    if (skipNextCode) {
                        skipNextCode = false
                        return@forEach
                    }

                    output.append(it.text)
                }

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
                    result.hasError = true
                    return
                }

                val propElement = id.nextSibling?.nextSibling
                val isProp = (propElement.elementType == DevInTypes.COMMAND_PROP)
                if (!isProp) {
                    output.append(used.text)
                    logger.warn("No command prop found: ${used.text}")
                    result.hasError = true
                    return
                }

                processingCommand(command, propElement!!.text, used, fallbackText = used.text)
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

    private fun processingCommand(commandNode: BuiltinCommand, prop: String, used: DevInUsed, fallbackText: String) {
        val command: InsCommand = when (commandNode) {
            BuiltinCommand.FILE -> {
                FileInsCommand(myProject, prop)
            }

            BuiltinCommand.REV -> {
                RevInsCommand(myProject, prop)
            }

            BuiltinCommand.SYMBOL -> {
                PrintInsCommand("/" + commandNode.agentName + ":" + prop)
            }

            BuiltinCommand.WRITE -> {
                result.isLocalCommand = true
                val devInCode: CodeBlockElement? = lookupNextCode(used)
                if (devInCode == null) {
                    PrintInsCommand("/" + commandNode.agentName + ":" + prop)
                } else {
                    WriteInsCommand(myProject, prop, devInCode.text)
                }
            }

            BuiltinCommand.PATCH -> {
                result.isLocalCommand = true
                val devInCode: CodeBlockElement? = lookupNextCode(used)
                if (devInCode == null) {
                    PrintInsCommand("/" + commandNode.agentName + ":" + prop)
                } else {
                    PatchInsCommand(myProject, prop, devInCode.text)
                }
            }

            BuiltinCommand.RUN -> {
                result.isLocalCommand = true
                RunInsCommand(myProject, prop)
            }
        }

        val execResult = command.execute()

        val isSucceed = execResult?.contains("<DevliError>") == false
        val result = if (isSucceed) {
            val hasReadCodeBlock = commandNode == BuiltinCommand.WRITE || commandNode == BuiltinCommand.PATCH
            if (hasReadCodeBlock) {
                skipNextCode = true
            }

            execResult
        } else {
            execResult ?: fallbackText
        }

        output.append(result)
    }

    private fun lookupNextCode(used: DevInUsed): CodeBlockElement? {
        val devInCode: CodeBlockElement?
        var next: PsiElement? = used
        while (true) {
            next = next?.nextSibling
            if (next == null) {
                devInCode = null
                break
            }

            if (next.elementType == DevInTypes.CODE) {
                devInCode = next as CodeBlockElement
                break
            }
        }
        return devInCode
    }
}


