package cc.unitmesh.idea.provider

import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.prompting.CommitPrompting
import cc.unitmesh.devti.prompting.model.PromptConfig
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.TechStackProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.idea.flow.MvcContextService
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class JvmIdeaContextPrompter : ContextPrompter() {
    private var additionContext: String = ""
    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private var promptConfig: PromptConfig? = null
    private lateinit var mvcContextService: MvcContextService
    private var fileName = ""
    private lateinit var changeListManager: ChangeListManager

    private fun langSuffix(): String = when (lang) {
        "Java" -> "java"
        "Kotlin" -> "kt"
        else -> "java"
    }

    private fun isController() = fileName.endsWith("Controller." + langSuffix())
    private fun isService() =
        fileName.endsWith("Service." + langSuffix()) || fileName.endsWith("ServiceImpl." + langSuffix())


    override fun initContext(
        actionType: ChatBotActionType,
        selectedText: String,
        file: PsiFile?,
        project: Project,
        offset: Int
    ) {
        super.initContext(actionType, selectedText, file, project, offset)
        changeListManager = ChangeListManagerImpl.getInstance(project)
        mvcContextService = MvcContextService(project)

        lang = file?.language?.displayName ?: ""
        fileName = file?.name ?: ""
    }

    init {
        val prompts = autoDevSettingsState.customEnginePrompts
        promptConfig = PromptConfig.tryParse(prompts)
    }

    override fun displayPrompt(): String {
        val prompt = createPrompt(selectedText)
        val finalPrompt = if (additionContext.isNotEmpty()) {
            """$additionContext
                |$selectedText""".trimMargin()
        } else {
            selectedText
        }

        return """$prompt:
         <pre><code>$finalPrompt</pre></code>
        """.trimMargin()
    }

    override fun requestPrompt(): String {
        val prompt = createPrompt(selectedText)
        val finalPrompt = if (additionContext.isNotEmpty()) {
            """$additionContext
                |$selectedText""".trimMargin()
        } else {
            selectedText
        }

        return """$prompt:
            $finalPrompt
        """.trimMargin()
    }


    private fun createPrompt(selectedText: String): String {
        var prompt = """$action this $lang code"""
        when (action!!) {
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
                    "Complete $lang code, return rest code, no explaining"
                }

                when {
                    isController() -> {
                        val spec = PromptConfig.load().spec["controller"]
                        if (!spec.isNullOrEmpty()) {
                            additionContext = "requirements: \n$spec"
                        }
                        additionContext += mvcContextService.controllerPrompt(file)
                    }

                    isService() -> {
                        val spec = PromptConfig.load().spec["service"]
                        if (!spec.isNullOrEmpty()) {
                            additionContext = "requirements: \n$spec"
                        }
                        additionContext += mvcContextService.servicePrompt(file)
                    }

                    else -> {
                        additionContext = SimilarChunksWithPaths.createQuery(file!!) ?: ""
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
                prompt = """suggest 10 commit messages based on the following diff:
commit messages should:
 - follow conventional commits
 - message format should be: <type>[scope]: <description>

examples:
 - fix(authentication): add password regex pattern
 - feat(storage): add new test cases
 
 {{diff}}
 """
                prepareVcsContext()
            }

            ChatBotActionType.CREATE_DDL -> {
                val spec = PromptConfig.load().spec["ddl"]
                if (!spec.isNullOrEmpty()) {
                    additionContext = "requirements: \n$spec"
                }
                prompt = "create ddl based on the follow info"
            }

            ChatBotActionType.CREATE_CHANGELOG -> {
                prompt = "generate release note base on the follow commit"
            }
        }

        return prompt
    }

    private fun prepareVcsContext() {
        val changes = changeListManager.changeLists.flatMap {
            it.changes
        }

//        EditorHistoryManager, after 2023.2, can use the following code
//        val commitWorkflowUi: CommitWorkflowUi = project.service()
//        val changes = commitWorkflowUi.getIncludedChanges()

        val prompting = project!!.service<CommitPrompting>()
        additionContext += prompting.computeDiff(changes)
    }

    private fun addTestContext() {
        val techStackProvider = TechStackProvider.stack(file?.language?.displayName ?: "")
        val techStacks = techStackProvider!!.prepareLibrary()
        when {
            isController() -> {
                additionContext = "// tech stacks: " + techStacks.controllerString()
            }

            isService() -> {
                additionContext = "// tech stacks: " + techStacks.serviceString()
            }
        }
    }

    private fun addFixIssueContext(selectedText: String) {
        val projectPath = project!!.basePath ?: ""
        runReadAction {
            val lookupFile = if (selectedText.contains(projectPath)) {
                val regex = Regex("$projectPath(.*\\.)${langSuffix()}")
                val relativePath = regex.find(selectedText)?.groupValues?.get(1) ?: ""
                val file = LocalFileSystem.getInstance().findFileByPath(projectPath + relativePath)
                file?.let {
                    val psiFile = PsiManager.getInstance(project!!).findFile(it)
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
}
