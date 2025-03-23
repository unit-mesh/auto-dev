package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.BROWSE
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.DATABASE
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.DIR
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.FILE
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.LOCAL_SEARCH
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.PATCH
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.RELATED
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.REV
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.RIPGREP_SEARCH
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.STRUCTURE
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.SYMBOL
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand.WRITE
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.provider.devins.LanguageProcessor
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory

@Service(Service.Level.PROJECT)
class AutoSketchMode(val project: Project) {
    var isEnable: Boolean = false

    var listener: SketchInputListener? = null

    fun start(text: String, inputListener: SketchInputListener) {
        listener = inputListener
        val codeFenceList = CodeFence.parseAll(text)
        val devinCodeFence = codeFenceList.filter {
            it.language.displayName == "DevIn"
        }

        val commands: MutableList<BuiltinCommand> = mutableListOf()

        val allCode = devinCodeFence.filter {
            !it.text.contains("<DevinsError>") && (hasReadCommand(it) || hasToolchainFunctionCommand(it))
        }

        invokeLater {
            val language = CodeFence.findLanguage("DevIn") ?: return@invokeLater
            commands += devinCodeFence.mapNotNull {
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(language, it.text)
                    ?: return@mapNotNull null

                LanguageProcessor.devin()?.transpileCommand(project, psiFile) ?: emptyList()
            }.flatten()

            project.getService(AgentStateService::class.java).addTools(commands)
        }

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

    fun send(text: String) {
        listener?.manualSend(text)
    }

    private fun hasReadCommand(fence: CodeFence): Boolean = AUTOABLE_COMMANDS.any { command ->
        fence.text.contains("/" + command.commandName + ":")
    }

    val AUTOABLE_COMMANDS =
        setOf(
            DIR,
            LOCAL_SEARCH,
            FILE,
            REV,
            STRUCTURE,
            SYMBOL,
            DATABASE, // should be handle in run sql
            RELATED,
            RIPGREP_SEARCH,
            BROWSE,
            PATCH,
            WRITE
        )

    private fun hasToolchainFunctionCommand(fence: CodeFence): Boolean {
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
