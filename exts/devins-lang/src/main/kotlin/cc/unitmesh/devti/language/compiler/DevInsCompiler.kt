package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.custom.compile.VariableTemplateCompiler
import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.exec.*
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.devin.dataprovider.CustomCommand
import cc.unitmesh.devti.devin.dataprovider.ToolHubVariable
import cc.unitmesh.devti.language.parser.CodeBlockElement
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import kotlinx.coroutines.runBlocking

val CACHED_COMPILE_RESULT = mutableMapOf<String, DevInsCompiledResult>()

class DevInsCompiler(
    private val myProject: Project,
    private val file: DevInFile,
    private val editor: Editor? = null,
    private val element: PsiElement? = null
) {
    private var skipNextCode: Boolean = false
    private val logger = logger<DevInsCompiler>()
    private val result = DevInsCompiledResult()
    private val output: StringBuilder = StringBuilder()

    /**
     * Todo: build AST tree, then compile
     */
    fun compile(): DevInsCompiledResult {
        result.input = file.text
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
                DevInTypes.COMMENTS -> {
                    if (it.text.startsWith("[flow]:")) {
                        val fileName = it.text.substringAfter("[flow]:").trim()
                        val content =
                            myProject.guessProjectDir()?.findFileByRelativePath(fileName)?.let { virtualFile ->
                                virtualFile.inputStream.bufferedReader().use { reader -> reader.readText() }
                            }

                        if (content != null) {
                            val devInFile = DevInFile.fromString(myProject, content)
                            result.nextJob = devInFile
                        }
                    }
                }

                else -> {
                    output.append(it.text)
                    logger.warn("Unknown element type: ${it.elementType}")
                }
            }
        }

        result.output = output.toString()

        CACHED_COMPILE_RESULT[file.name] = result
        return result
    }

    private fun processUsed(used: DevInUsed) {
        val firstChild = used.firstChild
        val id = firstChild.nextSibling

        when (firstChild.elementType) {
            DevInTypes.COMMAND_START -> {
                val command = BuiltinCommand.fromString(id?.text ?: "")
                if (command == null) {
                    CustomCommand.fromString(myProject, id?.text ?: "")?.let { cmd ->
                        DevInFile.fromString(myProject, cmd.content).let { file ->
                            DevInsCompiler(myProject, file).compile().let {
                                output.append(it.output)
                                result.hasError = it.hasError
                            }
                        }

                        return
                    }


                    output.append(used.text)
                    logger.warn("Unknown command: ${id?.text}")
                    result.hasError = true
                    return
                }

                if (!command.requireProps) {
                    processingCommand(command, "", used, fallbackText = used.text)
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
                val agentId = id?.text
                val configs = CustomAgentConfig.loadFromProject(myProject).filter {
                    it.name == agentId
                }

                if (configs.isNotEmpty()) {
                    result.executeAgent = configs.first()
                }
            }

            DevInTypes.VARIABLE_START -> {
                val variableId = id?.text
                val variable = ToolHubVariable.lookup(myProject, variableId)
                if (variable.isNotEmpty()) {
                    output.append(variable.joinToString("\n") { it })
                    return
                }

                if (editor == null || element == null) {
                    output.append("$DEVINS_ERROR No context editor found for variable: ${used.text}")
                    result.hasError = true
                    return
                }

                val file = element.containingFile
                VariableTemplateCompiler(file.language, file, element, editor).compile(used.text).let {
                    output.append(it)
                }
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
                result.isLocalCommand = true
                SymbolInsCommand(myProject, prop)
            }

            BuiltinCommand.WRITE -> {
                result.isLocalCommand = true
                val devInCode: CodeBlockElement? = lookupNextCode(used)
                if (devInCode == null) {
                    PrintInsCommand("/" + commandNode.commandName + ":" + prop)
                } else {
                    WriteInsCommand(myProject, prop, devInCode.codeText(), used)
                }
            }

            BuiltinCommand.PATCH -> {
                result.isLocalCommand = true
                val devInCode: CodeBlockElement? = lookupNextCode(used)
                if (devInCode == null) {
                    PrintInsCommand("/" + commandNode.commandName + ":" + prop)
                } else {
                    PatchInsCommand(myProject, prop, devInCode.codeText())
                }
            }

            BuiltinCommand.COMMIT -> {
                result.isLocalCommand = true
                val devInCode: CodeBlockElement? = lookupNextCode(used)
                if (devInCode == null) {
                    PrintInsCommand("/" + commandNode.commandName + ":" + prop)
                } else {
                    CommitInsCommand(myProject, devInCode.codeText())
                }
            }

            BuiltinCommand.RUN -> {
                result.isLocalCommand = true
                RunInsCommand(myProject, prop)
            }

            BuiltinCommand.FILE_FUNC -> {
                result.isLocalCommand = true
                FileFuncInsCommand(myProject, prop)
            }

            BuiltinCommand.SHELL -> {
                result.isLocalCommand = true
                val shireCode: String? = lookupNextCode(used)?.codeText()
                ShellInsCommand(myProject, prop, shireCode)
            }

            BuiltinCommand.BROWSE -> {
                result.isLocalCommand = true
                BrowseInsCommand(myProject, prop)
            }

            BuiltinCommand.REFACTOR -> {
                result.isLocalCommand = true
                val nextTextSegment = lookupNextTextSegment(used)
                RefactorInsCommand(myProject, prop, nextTextSegment)
            }

            BuiltinCommand.DIR -> {
                result.isLocalCommand = true
                DirInsCommand(myProject, prop)
            }

            BuiltinCommand.DATABASE -> {
                result.isLocalCommand = true
                val shireCode: String? = lookupNextCode(used)?.text
                DatabaseInsCommand(myProject, prop, shireCode)
            }

            BuiltinCommand.STRUCTURE -> {
                result.isLocalCommand = true
                StructureInCommand(myProject, prop)
            }

            BuiltinCommand.LOCAL_SEARCH -> {
                result.isLocalCommand = true
                val shireCode: String? = lookupNextCode(used)?.text
                LocalSearchInsCommand(myProject, prop, shireCode)
            }

            BuiltinCommand.RIPGREP_SEARCH -> {
                result.isLocalCommand = true
                val shireCode: String? = lookupNextCode(used)?.text
                RipgrepSearchInsCommand(myProject, prop, shireCode)
            }

            BuiltinCommand.RELATED -> {
                result.isLocalCommand = true
                RelatedSymbolInsCommand(myProject, prop)
            }

            BuiltinCommand.OPEN -> {
                result.isLocalCommand = true
                OpenInsCommand(myProject, prop)
            }

            else -> {
                PrintInsCommand("/" + commandNode.commandName + ":" + prop)
            }
        }

        val execResult = runBlocking { command.execute() }

        val isSucceed = execResult?.contains("$DEVINS_ERROR") == false
        val result = if (isSucceed) {
            val hasReadCodeBlock = commandNode in listOf(
                BuiltinCommand.WRITE,
                BuiltinCommand.PATCH,
                BuiltinCommand.COMMIT,
                BuiltinCommand.DATABASE,
                BuiltinCommand.SHELL,
            )

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

    private fun lookupNextTextSegment(used: DevInUsed): String {
        val textSegment: StringBuilder = StringBuilder()
        var next: PsiElement? = used
        while (true) {
            next = next?.nextSibling
            if (next == null) {
                break
            }

            if (next.elementType == DevInTypes.TEXT_SEGMENT) {
                textSegment.append(next.text)
                break
            }
        }

        return textSegment.toString()
    }
}


