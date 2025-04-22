package cc.unitmesh.devti.language.startup

import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.variable.ShireVariableTemplateCompiler
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.flow.DevInsProcessProcessor
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

@Service(Service.Level.APP)
class PasteManagerService {
    private val pasteProcessorMap = mutableMapOf<HobbitHole, DevInFile>()

    fun registerPasteProcessor(key: HobbitHole, file: DevInFile) {
        pasteProcessorMap[key] = file
    }

    fun firstProcessor(): HobbitHole? {
        return pasteProcessorMap.keys.firstOrNull()
    }

    fun executeProcessor(
        project: Project,
        hobbitHole: HobbitHole,
        text: String,
        file: PsiFile,
        editor: Editor
    ): String {
        val future = CompletableFuture<String>()
        val shireFile = pasteProcessorMap[hobbitHole] ?: return text

        val compileResult = runBlocking { DevInsProcessProcessor(project).compileResult(shireFile) }
        val variableTable = compileResult.variableTable

        val templateCompiler =
            ShireVariableTemplateCompiler(project, hobbitHole, variableTable, compileResult.output, editor)
        templateCompiler.putCustomVariable("text", text)

        val promptText = runBlocking {
            templateCompiler.compile().trim()
        }

        PostProcessorContext.getData()?.lastTaskOutput?.let {
            templateCompiler.putCustomVariable("output", it)
        }

        val flow: Flow<String>? = LlmFactory.create(project)?.stream(promptText, "", false)
        AutoDevCoroutineScope.scope(project).launch {
            val suggestion = StringBuilder()

            flow?.cancellable()?.collect { char ->
                suggestion.append(char)
            }

            val code = CodeFence.parse(suggestion.toString())
            future.complete(code.text)

            logger<ShireCopyPastePreProcessor>().info("paste code: $code")
        }

        return future.get()
    }

    companion object {
        fun getInstance(): PasteManagerService =
            ApplicationManager.getApplication().getService(PasteManagerService::class.java)
    }
}

class ShireCopyPastePreProcessor : CopyPastePreProcessor {
    override fun preprocessOnCopy(file: PsiFile, startOffsets: IntArray, endOffsets: IntArray, text: String): String? {
        return text
    }

    override fun preprocessOnPaste(
        project: Project,
        file: PsiFile,
        editor: Editor,
        text: String,
        rawText: RawText?,
    ): String {
        val instance = PasteManagerService.getInstance()
        val hobbitHole = instance.firstProcessor() ?: return text
        if (!hobbitHole.enabled) return text

        /// only for test java and kotlin
        val language = file.language.displayName.lowercase()
        if (!(language == "java" || language == "kotlin")) {
            return text
        }

        /// should be more than 7 lines
        if (text.lines().size < 5) {
            return text
        }

        return instance.executeProcessor(project, hobbitHole, text, file, editor)
    }
}
