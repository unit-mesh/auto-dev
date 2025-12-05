package cc.unitmesh.devti.language.run.flow

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.compiler.DevInsCompiledResult
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.compiler.FLOW_FALG
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.psi.DevInVisitor
import cc.unitmesh.devti.language.run.runner.ShireConsoleView
import cc.unitmesh.devti.language.run.runner.cancelWithConsole
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiUtilBase


@Service(Service.Level.PROJECT)
class DevInsProcessProcessor(val project: Project) {
    private val conversationService = project.service<DevInsConversationService>()

    /**
     * This function takes a DevInFile as input and returns a list of PsiElements that are comments.
     * It iterates through the DevInFile and adds any comments it finds to the list.
     *
     * @param devInFile the DevInFile to search for comments
     * @return a list of PsiElements that are comments
     */
    private fun collectComments(devInFile: DevInFile): List<PsiComment> {
        val comments = mutableListOf<PsiComment>()
        devInFile.accept(object : DevInVisitor() {
            override fun visitComment(comment: PsiComment) {
                comments.add(comment)
            }
        })

        return comments
    }

    /**
     * Process the output of a script based on the exit code and flag comment.
     * If LLM returns a DevIn code, execute it.
     * If the exit code is not 0, attempts to fix the script with LLM.
     * If the exit code is 0 and there is a flag comment, process it.
     *
     * Flag comment format:
     * - [flow]:flowable.devin, means next step is flowable.devin
     *
     * @param output The output of the script
     * @param event The process event containing the exit code
     * @param scriptPath The path of the script file
     */
    suspend fun process(output: String, event: ProcessEvent, scriptPath: String, consoleView: ShireConsoleView?) {
        conversationService.refreshIdeOutput(scriptPath, output)

        val code = CodeFence.parse(conversationService.getLlmResponse(scriptPath))
        if (code.language == DevInLanguage.INSTANCE) {
            executeTask(DevInFile.fromString(project, code.text), consoleView)
        }

        when {
            event.exitCode == 0 -> {
                val shireFile: DevInFile = runReadAction { DevInFile.lookup(project, scriptPath) } ?: return
                val firstComment = collectComments(shireFile).firstOrNull() ?: return
                if (firstComment.textRange.startOffset == 0) {
                    val text = firstComment.text
                    if (text.startsWith(FLOW_FALG)) {
                        val nextScript = text.substring(FLOW_FALG.length)
                        val newScript = DevInFile.lookup(project, nextScript) ?: return
                        this.executeTask(newScript, consoleView)
                    }
                }
            }

            event.exitCode != 0 -> {
                conversationService.retryScriptExecution(scriptPath, consoleView)
            }
        }
    }

    /**
     * This function is responsible for running a task with a new script.
     * @param newScript The new script to be run.
     */
    suspend fun executeTask(newScript: DevInFile, consoleView: ShireConsoleView?) {
        val shireCompiler = createCompiler(project, newScript)
        val result = shireCompiler.compile()
        if (result.output != "") {
            AutoDevNotifications.warn(project, result.output)
        }

        if (result.hasError) {
            if (consoleView == null) return

            try {
                LlmFactory.create(project)
                    .stream(result.output, "", true)
                    .cancelWithConsole(consoleView)
                    .collect {
                        consoleView.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                    }
            } catch (e: Exception) {
                consoleView.print(e.message ?: "Error", ConsoleViewContentType.ERROR_OUTPUT)
            }
        } else {
            if (result.nextJob == null) return

            val nextJob = result.nextJob!!
            val nextResult = createCompiler(project, nextJob).compile()
            if (nextResult.output != "") {
                AutoDevNotifications.warn(project, nextResult.output)
            }
        }
    }

    suspend fun compileResult(newScript: DevInFile): DevInsCompiledResult {
        val devInsCompiler = createCompiler(project, newScript)
        val result = devInsCompiler.compile()
        return result
    }

    private fun createCompiler(
        project: Project,
        devInFile: DevInFile
    ): DevInsCompiler {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val element: PsiElement? = editor?.caretModel?.currentCaret?.offset?.let {
            val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return@let null
            getElementAtOffset(psiFile, it)
        }

        return DevInsCompiler(project, devInFile, editor, element)
    }

    private fun getElementAtOffset(psiFile: PsiElement, offset: Int): PsiElement? {
        var element = psiFile.findElementAt(offset) ?: return null

        if (element is PsiWhiteSpace) {
            element = element.parent
        }

        return element
    }
}