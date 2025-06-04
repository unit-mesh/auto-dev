package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.command.dataprovider.CustomCommand
import cc.unitmesh.devti.language.ast.variable.VariableTable
import cc.unitmesh.devti.language.compiler.processor.*
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val CACHED_COMPILE_RESULT = mutableMapOf<String, DevInsCompiledResult>()
const val FLOW_FALG = "[flow]:"

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

    private val variableTable = VariableTable()

    // Initialize processors
    private val commandFactory = InsCommandFactory()
    private val variableProcessor = VariableProcessor()
    private val commandProcessor = CommandProcessor(commandFactory)

    private val processors: List<DevInElementProcessor> = listOf(
        TextSegmentProcessor(),
        CodeProcessor(),
        CommentsProcessor(),
        FrontMatterProcessor(),
        UsedProcessor(commandProcessor, variableProcessor),
        VelocityExprProcessor()
    )

    /**
     * Todo: build AST tree, then compile
     */
    suspend fun compile(): DevInsCompiledResult = withContext(Dispatchers.IO) {
        result.input = runReadAction { file.text }
        val children = runReadAction { file.children }

        val context = CompilerContext(
            project = myProject,
            file = file,
            editor = editor,
            element = element,
            output = output,
            result = result,
            variableTable = variableTable,
            logger = logger,
            skipNextCode = skipNextCode
        )

        children.forEach { psiElement ->
            if (context.isError()) return@forEach

            val processor = processors.find { it.canProcess(psiElement) }
            if (processor != null) {
                val processResult = processor.process(psiElement, context)
                if (!processResult.success) {
                    logger.warn("Failed to process element: ${processResult.errorMessage}")
                }
                if (!processResult.shouldContinue) {
                    return@forEach
                }
            } else {
                // Fallback for unknown elements
                val text = runReadAction { psiElement.text }
                context.appendOutput(text)
                logger.warn("Unknown element type: ${psiElement.elementType}")
            }

            // Update skipNextCode from context
            skipNextCode = context.skipNextCode
        }

        result.output = output.toString()
        result.variableTable = variableTable

        CACHED_COMPILE_RESULT[file.name] = result
        return@withContext result
    }

    companion object {
        suspend fun transpileCommand(file: DevInFile): List<BuiltinCommand> {
            val children = runReadAction { file.children }
            return children.mapNotNull { it ->
                when (it.elementType) {
                    DevInTypes.USED -> {
                        val used = it as DevInUsed
                        val firstChild = used.firstChild
                        val id = firstChild.nextSibling

                        return@mapNotNull when (firstChild.elementType) {
                            DevInTypes.COMMAND_START -> {
                                val originCmdName = id?.text ?: ""
                                val command = BuiltinCommand.fromString(originCmdName)
                                if (command == null) {
                                    CustomCommand.fromString(file.project, originCmdName) ?: return@mapNotNull null
                                }

                                command
                            }

                            else -> null
                        }
                    }

                    else -> null
                }
            }
        }
    }
}


