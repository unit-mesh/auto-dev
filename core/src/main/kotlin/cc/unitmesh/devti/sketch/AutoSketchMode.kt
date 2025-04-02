package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.*
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.provider.devins.LanguageProcessor
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory

@Service(Service.Level.PROJECT)
class AutoSketchMode(val project: Project) {
    var isEnable: Boolean = false

    var listener: SketchInputListener? = null

    suspend fun start(text: String, inputListener: SketchInputListener) {
        listener = inputListener
        val codeFenceList = CodeFence.parseAll(text)
        val devinCodeFence = codeFenceList.filter {
            it.language.displayName == "DevIn"
        }

        val commands: MutableList<BuiltinCommand> = mutableListOf()

        val allCode = devinCodeFence.filter {
            !it.text.contains("<DevinsError>") && (hasReadCommand(it) || hasToolchainFunctionCommand(it))
        }

        val language = CodeFence.findLanguage("DevIn")
        commands += devinCodeFence.mapNotNull {
            val psiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText(language, it.text) }
                ?: return@mapNotNull null

            LanguageProcessor.devin()?.transpileCommand(project, psiFile) ?: emptyList()
        }.flatten()

        project.getService(AgentStateService::class.java).addTools(commands)

        if (allCode.isEmpty()) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(AutoSketchModeListener.TOPIC)
                .done()
            return
        }

        val allCodeText = allCode.map { it.text }.distinct().joinToString("\n")
        if (allCodeText.trim().isEmpty()) {
            logger<SketchToolWindow>().error("No code found")
        } else {
            inputListener.manualSend(allCodeText)
        }
    }

    fun enableComposerMode() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(AutoSketchModeListener.TOPIC)
            .start()
        isEnable = true
    }

    fun send(text: String) {
        listener?.manualSend(text)
    }

    private fun hasReadCommand(fence: CodeFence): Boolean = buildAutoCommands().any { command ->
        fence.text.contains("/" + command.commandName + ":")
    }

    private fun buildAutoCommands(): Set<BuiltinCommand> {
        val of = mutableSetOf(
            DIR,
            LOCAL_SEARCH,
            FILE,
            REV,
            STRUCTURE,
            SYMBOL,
            RELATED,
            RIPGREP_SEARCH,
            RULE,
            USAGE,
            BROWSE,
        )

        of += setOf(
            PATCH, DATABASE, WRITE
        )

        return of
    }

    private suspend fun hasToolchainFunctionCommand(fence: CodeFence): Boolean {
        val toolchainCmds = ToolchainFunctionProvider.all().map { it.funcNames() }.flatten()
        return toolchainCmds.any {
            fence.text.contains("/$it:")
        }
    }

    companion object {
        fun getInstance(project: Project): AutoSketchMode {
            return project.service<AutoSketchMode>()
        }
    }
}
