package cc.unitmesh.devti.language.run.runner

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.config.DevInActionLocation
import cc.unitmesh.devti.language.ast.variable.ShireVariableTemplateCompiler
import cc.unitmesh.devti.language.compiler.DevInsCompiledResult
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.compiler.streaming.OnStreamingService
import cc.unitmesh.devti.language.console.addCancelCallback
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.provider.ActionLocationEditor
import cc.unitmesh.devti.language.provider.TerminalLocationExecutor
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.DevInsConfiguration
import cc.unitmesh.devti.language.run.ShireProcessHandler
import cc.unitmesh.devti.language.run.flow.DevInsConversationService
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.llms.cancelHandler
import cc.unitmesh.devti.util.workerThread
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.CompletableFuture

class ShireRunner(
    private val project: Project,
    private val console: ShireConsoleView?,
    private val configuration: DevInsConfiguration,
    private val variableMap: Map<String, String>,
    private val processHandler: ShireProcessHandler,
) {
    private var compiledVariables: Map<String, Any> = mapOf()

    private var isCanceled: Boolean = false

    private val cancelListeners = mutableSetOf<(String) -> Unit>()

    suspend fun execute(parsedResult: DevInsCompiledResult): String? {
        prepareExecute(parsedResult, compiledVariables, project, console)

        val runResult = CompletableFuture<String>()

        val varsMap = variableFromPostProcessorContext(variableMap)

        val runnerContext = processTemplateCompile(parsedResult, varsMap, project, configuration, console)
        if (runnerContext.hasError) {
            processHandler.exitWithError()
            return null
        }

        val service = project.getService(OnStreamingService::class.java)
        service?.onStart(project, runnerContext.finalPrompt)

        this.compiledVariables = runnerContext.compiledVariables

        project.getService(DevInsConversationService::class.java)
            .createConversation(configuration.getScriptPath(), runnerContext.compileResult)

        if (runnerContext.hole?.actionLocation == DevInActionLocation.TERMINAL_MENU) {
            executeTerminalUiTask(runnerContext) { response, textRange ->
                runResult.complete(response)
                executePostFunction(runnerContext, runnerContext.hole, response, textRange)
            }
        } else {
            executeNormalUiTask(runnerContext) { response, textRange ->
                runResult.complete(response)
                executePostFunction(runnerContext, runnerContext.hole, response, textRange)
            }
        }

        return withContext(Dispatchers.IO) {
            runResult.get()
        }
    }

    private fun executeTerminalUiTask(context: ShireRunnerContext, postFunction: PostFunction) {
        CoroutineScope(workerThread).launch {
            val handler = TerminalLocationExecutor.provide(project)?.bundler(project, variableMap["input"] ?: "")
            if (handler == null) {
                console?.print("Terminal not found", ConsoleViewContentType.ERROR_OUTPUT)
                processHandler.exitWithError()
                return@launch
            }

            val llmResult = StringBuilder()
            runBlocking {
                try {
                    LlmFactory.create(project)?.stream(context.finalPrompt, "", false)
                        ?.cancelWithConsole(console)?.collect {
                            llmResult.append(it)
                            handler.onChunk.invoke(it)
                        } ?: console?.print(
                        "ShireRunner:" + AutoDevBundle.message("devins.llm.notfound"),
                        ConsoleViewContentType.ERROR_OUTPUT
                    )
                } catch (e: Exception) {
                    console?.print(e.message ?: "Error", ConsoleViewContentType.ERROR_OUTPUT)
                    handler.onFinish?.invoke(null)
                    processHandler.exitWithError()
                }
            }

            val response = llmResult.toString()
            handler.onFinish?.invoke(response)

            postFunction(response, null)
            processHandler.detachProcess()
        }
    }

    private fun executeNormalUiTask(runData: ShireRunnerContext, postFunction: PostFunction) {
        val agent = runData.compileResult.executeAgent
        val hobbitHole = runData.hole

        val shireLlmExecutorContext = ShireLlmExecutorContext(
            configuration = configuration,
            processHandler = processHandler,
            console = console,
            myProject = project,
            hole = hobbitHole,
            prompt = runData.finalPrompt,
            editor = runData.editor,
        )
        val shireLlmExecutor: ShireLlmExecutor = when {
            agent != null -> {
                CustomRemoteAgentLlmExecutor(shireLlmExecutorContext, agent)
            }

            else -> {
                val isLocalMode = runData.compileResult.isLocalCommand
                ShireDefaultLlmExecutor(shireLlmExecutorContext, isLocalMode)
            }
        }

        shireLlmExecutor.execute(postFunction)
    }

    fun executePostFunction(
        runnerContext: ShireRunnerContext,
        hobbitHole: HobbitHole?,
        response: String?,
        textRange: TextRange?,
    ) {
        if (console?.isCanceled() == true) return
        val currentFile = runnerContext.editor?.virtualFile?.let {
            runReadAction { PsiManager.getInstance(project).findFile(it) }
        }
        val context = PostProcessorContext(
            currentFile = currentFile,
            currentLanguage = currentFile?.language,
            genText = response,
            modifiedTextRange = textRange,
            editor = runnerContext.editor,
            lastTaskOutput = response,
            compiledVariables = compiledVariables,
            llmModelName = hobbitHole?.model,
        )

        PostProcessorContext.updateContextAndVariables(context)

        val endStreamProcessor = hobbitHole?.executeStreamingEndProcessor(project, console, context, compiledVariables)
        PostProcessorContext.updateOutput(endStreamProcessor)

        val afterStreamHandler = hobbitHole?.executeAfterStreamingProcessor(project, console, context)
        PostProcessorContext.updateOutput(afterStreamHandler)

        try {
            processHandler.detachProcess()
        } catch (e: Exception) {
//            console?.print(e.message ?: "Error", ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    @Synchronized
    fun addCancelListener(listener: (String) -> Unit) {
        if (isCanceled) cancel(listener)
        else cancelListeners.add(listener)
    }

    @Synchronized
    fun cancel() {
        if (!isCanceled) {
            isCanceled = true
            cancelListeners.forEach { cancel(it) }
        }
    }

    fun isCanceled() = isCanceled

    private fun cancel(cancel: (String) -> Unit) {
        cancel("This job is canceled")
    }

    companion object {
        /**
         * Thi api design for compile only
         */
        suspend fun compileOnly(
            project: Project,
            shireFile: DevInFile,
            initVariables: Map<String, String>,
            sampleEditor: Editor? = null
        ): ShireRunnerContext {
            val parsedResult = preAnalysisAndLocalExecute(shireFile, project, sampleEditor)
            prepareExecute(parsedResult, initVariables, project, null, userEditor = sampleEditor)

            val variables = variableFromPostProcessorContext(initVariables)
            val runnerContext = processTemplateCompile(parsedResult, variables, project, null, null,
                userEditor = sampleEditor
            )

            val service = project.getService(OnStreamingService::class.java)
            service?.onStart(project, runnerContext.finalPrompt)
            return runnerContext
        }

        suspend fun preAnalysisAndLocalExecute(
            shireFile: DevInFile,
            project: Project,
            editor: Editor? = null,
        ): DevInsCompiledResult {
            val baseEditor = editor ?: ActionLocationEditor.defaultEditor(project)
            val syntaxAnalyzer = DevInsCompiler(project, shireFile, baseEditor)
            return syntaxAnalyzer.compile()
        }

        fun prepareExecute(
            parsedResult: DevInsCompiledResult,
            variables: Map<String, Any>,
            project: Project,
            consoleView: ShireConsoleView?,
            userEditor: Editor? = null,
        ): PostProcessorContext {
            val hobbitHole = parsedResult.config
            val editor = userEditor ?: FileEditorManager.getInstance(project).selectedTextEditor
            hobbitHole?.pickupElement(project, editor)

            val file = runReadAction {
                editor?.let { PsiManager.getInstance(project).findFile(it.virtualFile) }
            }

            val context = PostProcessorContext.getData() ?: PostProcessorContext(
                currentFile = file,
                currentLanguage = file?.language,
                editor = editor,
                compiledVariables = variables,
                llmModelName = hobbitHole?.model
            )

            PostProcessorContext.updateContextAndVariables(context)

            val vars: MutableMap<String, Any?> = variables.toMutableMap()
            hobbitHole?.executeBeforeStreamingProcessor(project, context, consoleView, vars)

            val streamingService = project.getService(OnStreamingService::class.java)
            streamingService.clearStreamingService()
            hobbitHole?.onStreaming?.forEach {
                streamingService.registerStreamingService(it, consoleView)
            }

            hobbitHole?.setupStreamingEndProcessor(project, context)

            return context
        }

        private suspend fun processTemplateCompile(
            compileResult: DevInsCompiledResult,
            variableMap: Map<String, String>,
            project: Project,
            shireConfiguration: DevInsConfiguration?,
            shireConsoleView: ShireConsoleView?,
            userEditor: Editor? = null,
        ): ShireRunnerContext {
            val hobbitHole = compileResult.config
            val editor = userEditor ?: ActionLocationEditor.provide(project, hobbitHole?.actionLocation)

            val templateCompiler =
                ShireVariableTemplateCompiler(
                    project,
                    hobbitHole,
                    compileResult.variableTable,
                    compileResult.output,
                    editor
                )

            variableMap.forEach { (key, value) ->
                templateCompiler.putCustomVariable(key, value)
            }

            val promptTextTrim = templateCompiler.compile().trim()
            val compiledVariables = templateCompiler.compiledVariables

            PostProcessorContext.getData()?.lastTaskOutput?.let {
                templateCompiler.putCustomVariable("output", it)
            }

            if (shireConsoleView != null && shireConfiguration != null) {
                printCompiledOutput(shireConsoleView, promptTextTrim, shireConfiguration)
            }

            var hasError = false

            if (promptTextTrim.isEmpty()) {
                shireConsoleView?.print("No content to run", ConsoleViewContentType.ERROR_OUTPUT)
                hasError = true
            }

            if (promptTextTrim.contains(DEVINS_ERROR)) {
                hasError = true
            }

            return ShireRunnerContext(
                hobbitHole,
                editor = editor,
                compileResult,
                promptTextTrim,
                hasError,
                compiledVariables
            )
        }

        private fun printCompiledOutput(
            console: ConsoleViewWrapperBase,
            promptText: String,
            shireConfiguration: DevInsConfiguration,
        ) {
            console.print("Shire Script: ${shireConfiguration.getScriptPath()}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
            console.print("Shire Script Compile output:\n", ConsoleViewContentType.SYSTEM_OUTPUT)
            PostProcessorContext.getData()?.llmModelName?.let {
                console.print("Used model: $it\n", ConsoleViewContentType.SYSTEM_OUTPUT)
            }

            promptText.split("\n").forEach {
                when {
                    it.contains(DEVINS_ERROR) -> {
                        console.print(it, ConsoleViewContentType.LOG_ERROR_OUTPUT)
                    }

                    else -> {
                        console.print(it, ConsoleViewContentType.USER_INPUT)
                    }
                }
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
            }

            console.print("\n--------------------\n", ConsoleViewContentType.NORMAL_OUTPUT)
        }

        private fun variableFromPostProcessorContext(initValue: Map<String, String>): MutableMap<String, String> {
            val varsMap = initValue.toMutableMap()
            val data = PostProcessorContext.getData()
            val variables = data?.compiledVariables
            if (variables?.get("output") != null && initValue["output"] == null) {
                varsMap["output"] = variables["output"].toString()
            }

            return varsMap
        }
    }
}

fun <T> Flow<T>.cancelWithConsole(consoleView: ConsoleView?): Flow<T> =
    cancelHandler { consoleView?.addCancelCallback(it) }
