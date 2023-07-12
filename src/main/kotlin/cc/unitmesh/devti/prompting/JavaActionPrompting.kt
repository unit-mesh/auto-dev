package cc.unitmesh.devti.prompting

import cc.unitmesh.devti.connector.custom.PromptConfig
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.gui.chat.PromptFormatter
import cc.unitmesh.devti.settings.DevtiSettingsState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class JavaActionPrompting(
    private val action: ChatBotActionType,
    private val lang: String,
    private val selectedText: String,
    private val file: PsiFile?,
    val project: Project,
) : PromptFormatter {
    private var additionContext: String = ""
    private val devtiSettingsState = DevtiSettingsState.getInstance()
    private var promptConfig: PromptConfig? = null

    private val searchScope = GlobalSearchScope.allScope(project)
    private val javaPsiFacade = JavaPsiFacade.getInstance(project)

    private val fileName = file?.name ?: ""
    private val isController = fileName.endsWith("Controller.java")
    private val isService = fileName.endsWith("Service.java") || fileName.endsWith("ServiceImpl.java")


    init {
        val prompts = devtiSettingsState?.customEnginePrompts
        try {
            if (prompts != null) {
                promptConfig = Json.decodeFromString(prompts)
            }
        } catch (e: Exception) {
            println("Error parsing prompts: $e")
        }

        if (promptConfig == null) {
            promptConfig = PromptConfig.default()
        }
    }

    override fun getUIPrompt(): String {
        val prompt = createPrompt(selectedText)
        val finalPrompt = if (additionContext.isNotEmpty()) {
            "```java\n$additionContext\n$selectedText```"
        } else {
            selectedText
        }

        return """$prompt:
         <pre><code>$finalPrompt</pre></code>
        """.trimMargin()
    }

    override fun getRequestPrompt(): String {
        val prompt = createPrompt(selectedText)
        val finalPrompt = if (additionContext.isNotEmpty()) {
            "```java\n$additionContext\n$selectedText```"
        } else {
            selectedText
        }

        return """$prompt:
            $finalPrompt
        """.trimMargin()
    }


    private fun createPrompt(selectedText: String): String {
        var prompt = """$action this $lang code"""
        when (action) {
            ChatBotActionType.REVIEW -> {
                val codeReview = promptConfig?.codeReview
                prompt = if (codeReview?.instruction?.isNotEmpty() == true) {
                    codeReview.instruction
                } else {
                    "请检查如下的 $lang 代码"
                }
            }

            ChatBotActionType.EXPLAIN -> {
                val autoComment = promptConfig?.autoComment
                prompt = if (autoComment?.instruction?.isNotEmpty() == true) {
                    autoComment.instruction
                } else {
                    "请解释如下的 $lang 代码"
                }
            }

            ChatBotActionType.REFACTOR -> {
                val refactor = promptConfig?.refactor
                prompt = if (refactor?.instruction?.isNotEmpty() == true) {
                    refactor.instruction
                } else {
                    "请重构如下的 $lang 代码"
                }
            }

            ChatBotActionType.CODE_COMPLETE -> {
                val codeComplete = promptConfig?.autoComplete
                prompt = if (codeComplete?.instruction?.isNotEmpty() == true) {
                    codeComplete.instruction
                } else {
                    "请补全如下的 $lang 代码"
                }

                when {
                    isController -> {
                        additionContext = MvcContextFactory.createControllerPrompt(javaPsiFacade, searchScope, file)
                    }

                    isService -> {
                        additionContext = MvcContextFactory.createServicePrompt(file, javaPsiFacade, searchScope)
                    }
                }
            }

            ChatBotActionType.WRITE_TEST -> {
                val writeTest = promptConfig?.writeTest
                prompt = if (writeTest?.instruction?.isNotEmpty() == true) {
                    writeTest.instruction
                } else {
                    "请为如下的 $lang 代码编写测试"
                }

                addTestContext()
            }

            ChatBotActionType.FIX_ISSUE -> {
                prompt = "fix issue, and only submit the code changes."
                addFixIssueContext(selectedText)
            }

            ChatBotActionType.GEN_COMMIT_MESSAGE -> {
                prompt = "gen commit message"
                prepareVcsContext(selectedText)
                // todo: add context
            }

            ChatBotActionType.CREATE_DDL -> {
                prompt = "create ddl based on the follow info"
            }
        }

        return prompt
    }

    // TODO: move all manager to services?
    private val changeListManager = ChangeListManagerImpl(project)
    private fun prepareVcsContext(selectedText: String) {
        changeListManager.changeLists.forEach {
            logger.warn(it.data.toString())
            logger.warn(it.toString())
        }
    }

    private fun addTestContext() {
        val techStacks = prepareLibrary(project)
        when {
            isController -> {
                additionContext = "// tech stacks: " + techStacks.controller.keys.joinToString(", ")
            }

            isService -> {
                additionContext = "// tech stacks: " + techStacks.service.keys.joinToString(", ")
            }
        }
    }

    private fun addFixIssueContext(selectedText: String) {
        val projectPath = project.basePath ?: ""
        runReadAction {
            val lookupFile = if (selectedText.contains(projectPath)) {
                val regex = Regex("$projectPath(.*\\.java)")
                val relativePath = regex.find(selectedText)?.groupValues?.get(1) ?: ""
                val file = LocalFileSystem.getInstance().findFileByPath(projectPath + relativePath)
                file?.let {
                    val psiFile = PsiManager.getInstance(project).findFile(it) as? PsiJavaFileImpl
                    psiFile
                }
            } else {
                null
            }

            if (lookupFile != null) {
                additionContext = lookupFile.text.toString()
            }
        }
    }

    companion object {
        private val logger = Logger.getInstance(JavaActionPrompting::class.java)

    }
}
