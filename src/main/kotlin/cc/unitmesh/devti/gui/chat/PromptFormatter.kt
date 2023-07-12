package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.analysis.DtClass.Companion.fromPsiClass
import cc.unitmesh.devti.connector.custom.PromptConfig
import cc.unitmesh.devti.connector.custom.PromptItem
import cc.unitmesh.devti.settings.DevtiSettingsState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.plugins.gradle.GradleManager
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver
import org.jetbrains.plugins.gradle.service.project.JavaGradleProjectResolver
import org.jetbrains.plugins.gradle.util.GradleConstants


interface PromptFormatter {
    fun getUIPrompt(): String

    fun getRequestPrompt(): String
}

data class ControllerContext(
    val services: List<PsiClass>,
    val models: List<PsiClass>,
)

class BotActionPrompting(
    private val action: ChatBotActionType,
    private val lang: String,
    private val selectedText: String,
    private val file: PsiFile?,
    val project: Project,
) : PromptFormatter {
    var additionContext: String = ""

    companion object {
        private val logger = Logger.getInstance(BotActionPrompting::class.java)
    }

    private val devtiSettingsState = DevtiSettingsState.getInstance()
    private var promptConfig: PromptConfig? = null

    private val searchScope = GlobalSearchScope.allScope(project)
    private val javaPsiFacade = JavaPsiFacade.getInstance(project)

    private val fileName = file?.name ?: ""

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
            promptConfig = PromptConfig(
                PromptItem("Auto complete", "{code}"),
                PromptItem("Auto comment", "{code}"),
                PromptItem("Code review", "{code}"),
                PromptItem("Find bug", "{code}"),
                PromptItem("Write test", "{code}")
            )
        }
    }

    override fun getUIPrompt(): String {
        val prompt = createPrompt(selectedText)
        val finalPrompt = if (additionContext.isNotEmpty()) {
            "$selectedText\n\naddition info: ###$additionContext###"
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
            "$selectedText\n\naddition info: ###$additionContext###"
        } else {
            selectedText
        }

        return """$prompt:
            $finalPrompt
        """.trimMargin()
    }


    private fun prepareServiceContext(serviceFile: PsiJavaFileImpl?): List<PsiClass>? {
        return runReadAction {
            if (serviceFile == null) return@runReadAction null

            val allImportStatements = serviceFile.importList?.allImportStatements

            val entities = allImportStatements?.filter {
                it.importReference?.text?.matches(Regex(".*\\.(model|entity|domain)\\..*")) ?: false
            }?.mapNotNull {
                val importText = it.importReference?.text ?: return@mapNotNull null
                javaPsiFacade.findClass(importText, searchScope)
            } ?: emptyList()

            return@runReadAction entities
        }
    }

    private fun prepareControllerContext(controllerFile: PsiJavaFileImpl?): ControllerContext? {
        return runReadAction {
            if (controllerFile == null) return@runReadAction null

            val allImportStatements = controllerFile.importList?.allImportStatements

            val services = allImportStatements?.filter {
                it.importReference?.text?.endsWith("Service", true) ?: false
            }?.mapNotNull {
                val importText = it.importReference?.text ?: return@mapNotNull null
                javaPsiFacade.findClass(importText, searchScope)
            } ?: emptyList()

            // filter out model, entity, dto, from import statements
            val entities = allImportStatements?.filter {
                it.importReference?.text?.matches(Regex(".*\\.(model|entity|domain|dto)\\..*")) ?: false
            }?.mapNotNull {
                val importText = it.importReference?.text ?: return@mapNotNull null
                javaPsiFacade.findClass(importText, searchScope)
            } ?: emptyList()

            return@runReadAction ControllerContext(
                services = services,
                models = entities
            )
        }
    }

    private val isController = fileName.endsWith("Controller.java")
    private val isService = fileName.endsWith("Service.java") || fileName.endsWith("ServiceImpl.java")
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
                        additionContext = createControllerPrompt()
                    }

                    isService -> {
                        additionContext = createServicePrompt()
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

            ChatBotActionType.FixIssue -> {
                prompt = "fix issue, and only submit the code changes."
                addFixIssueContext(selectedText)
            }
        }

        return prompt
    }

    private fun addTestContext() {
        val projectDataManager = ProjectDataManager.getInstance()
        val projectData = projectDataManager.getExternalProjectData(
            project, GradleConstants.SYSTEM_ID, project.basePath!!
        )
        val libraries = projectData?.externalProjectStructure?.children?.filter {
            it.data is LibraryData
        }
        logger.warn("libraries: $libraries")

        when {
            isController -> {
                additionContext = "要求：1. 技术栈：MockMvc + Spring Boot Test + Mockito + AssertJ + JsonPath"
            }

            isService -> {
                additionContext = "要求：1. 技术栈：Mockito + AssertJ"
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

    private fun createServicePrompt(): String {
        val file = file as? PsiJavaFileImpl
        val relevantModel = prepareServiceContext(file)

        return """Complete java code, return rest code, no explaining.
${relevantModel?.joinToString("\n")}
"""
    }

    private fun createControllerPrompt(): String {
        val file = file as? PsiJavaFileImpl
        val context = prepareControllerContext(file)
        val services = context?.services?.map {
            DtClass.fromPsiClass(it).format()
        }
        val models = context?.models?.map {
            DtClass.fromPsiClass(it).format()
        }

        val relevantModel = (services ?: emptyList()) + (models ?: emptyList())

        val clazz = DtClass.fromJavaFile(file)
        return """Complete java code, return rest code, no explaining. 
```java
${relevantModel.joinToString("\n")}\n
// current path: ${clazz.path}
"""
    }
}