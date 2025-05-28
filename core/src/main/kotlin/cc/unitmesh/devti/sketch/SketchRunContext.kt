package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.agent.tool.search.RipgrepSearcher
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ui.relativePath
import cc.unitmesh.devti.mcp.ui.McpConfigService
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import cc.unitmesh.devti.sketch.rule.ProjectRule
import cc.unitmesh.devti.sketch.run.ShellUtil
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.text.SimpleDateFormat

/**
 * provide context for [core/src/main/resources/genius/zh/code/sketch.vm]
 * make sure the context is serializable and keep same with the template
 */
data class SketchRunContext(
    val currentFile: String?,
    val currentElement: PsiElement? = null,
    val openedFiles: List<VirtualFile>,
    val relatedFiles: List<VirtualFile>,
    val workspace: String = workspace(),
    val os: String = osInfo(),
    val time: String = time(),
    val userInput: String,
    val toolList: String,
    val shell: String = System.getenv("SHELL") ?: "/bin/bash",
    val frameworkContext: String = "",
    val buildTool: String = "",
    val searchTool: String = "localSearch",
    val rule: String = "",
    val moduleInfo: String = "",
) : TemplateContext {
    companion object {
        suspend fun create(project: Project, myEditor: Editor?, input: String): SketchRunContext {
            var editor: Editor? = null
            runInEdt {
                editor = (myEditor ?: FileEditorManager.getInstance(project).selectedTextEditor)
            }
            val currentFile: VirtualFile? = if (editor != null) {
                FileDocumentManager.getInstance().getFile(editor!!.document)
            } else {
                FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            }
            val psi = currentFile?.let { runReadAction { PsiManager.getInstance(project).findFile(it) } }
            val currentElement = editor?.let { runReadAction { psi?.findElementAt(it.caretModel.offset) } }
            val creationContext =
                ChatCreationContext(ChatOrigin.Intention, ChatActionType.CHAT, psi, listOf(), element = psi)

            val buildInfo = BuildSystemProvider.guess(project).firstOrNull()
            val buildTool = if (buildInfo != null) {
                "${buildInfo.buildToolName} + ${buildInfo.languageName} + ${buildInfo.languageVersion}"
            } else {
                ""
            }

            val otherFiles = FileEditorManager.getInstance(project).openFiles.filter { it != currentFile }

            val projectRule = ProjectRule(project)
            val rules = projectRule.getAllRules().map { it.nameWithoutExtension }
            val rule = if (rules.isNotEmpty()) {
                val allRules = "- User custom coding user-rule file names: " + rules.joinToString(",") { it }
                val string = if (projectRule.hasRule("README")) {
                    projectRule.getRuleContent("README") ?: ""
                } else ""

                if (string.isNotEmpty()) {
                    allRules + "\n" + string
                } else {
                    allRules
                }
            } else {
                ""
            }

            val moduleInfo = moduleContext(project)

            return SketchRunContext(
                currentFile = currentFile?.relativePath(project),
                currentElement = currentElement,
                openedFiles = otherFiles,
                relatedFiles = emptyList(),
                userInput = input,
                workspace = workspace(project),
                toolList = getToolList(project),
                shell = ShellUtil.detectShells().firstOrNull() ?: "/bin/bash",
                frameworkContext = ChatContextProvider.collectChatContextList(project, creationContext)
                    .joinToString(",", transform = ChatContextItem::text),
                buildTool = buildTool,
                searchTool = lookupSearchTool(),
                rule = rule,
                moduleInfo = moduleInfo,
            )
        }

        private val JAVA_ONE_PROJECT_SIZE_IN_IDEA = 2


        /**
         * Get tool list from McpConfigService if available, otherwise fall back to SketchToolchainProvider
         */
        fun getToolList(project: Project): String {
            val mcpConfigService = project.getService(McpConfigService::class.java)
            val selectedTools = mcpConfigService.convertToAgentTool()
            val defaultTools = SketchToolchainProvider.collect(project)

            return if (selectedTools.isNotEmpty()) {
                (defaultTools + selectedTools).joinToString("\n") { it.toString() }
            } else {
                defaultTools.joinToString("\n")
            }
        }

        fun moduleContext(project: Project): String {
            val moduleManager = ModuleManager.getInstance(project)
            val allModules = moduleManager.modules.map { it.name }
            if (allModules.size <= JAVA_ONE_PROJECT_SIZE_IN_IDEA) {
                return ""
            }

            val rootProjectName = allModules.minByOrNull { it.length } ?: return ""

            val cleanedModules = allModules
                .filter { it != rootProjectName && !it.endsWith(".test") }
                .map { moduleName ->
                    moduleName
                        .removePrefix("$rootProjectName.")
                        .removeSuffix(".main")
                }
                .distinct() // Remove duplicates after cleaning
                .filter { it.isNotEmpty() } // Remove any empty strings that might result

            return if (cleanedModules.isNotEmpty()) {
                val modules = cleanedModules.joinToString(", ") { it }
                "- This project is a mono-repo projects, Please careful to create file in module. Here's modules: $modules"
            } else {
                ""
            }
        }
    }
}

fun lookupSearchTool(): String {
    val findRipgrepBinary = try {
        RipgrepSearcher.findRipgrepBinary()
    } catch (_: Exception) {
        null
    }

    return if (findRipgrepBinary != null) {
        "ripgrepSearch"
    } else {
        logger<SketchRunContext>().warn("Ripgrep binary not found, fallback to local search")
        "localSearch"
    }
}

private fun osInfo() =
    System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch")

private fun time() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())

private fun workspace(myProject: Project? = null): String {
    val project = myProject ?: ProjectManager.getInstance().openProjects.firstOrNull()
    return project?.guessProjectDir()?.path ?: ""
}

