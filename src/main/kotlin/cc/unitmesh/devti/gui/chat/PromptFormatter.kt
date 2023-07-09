package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.analysis.DtClass.Companion.fromPsiClass
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope


interface PromptFormatter {
    fun getUIPrompt(): String

    fun getRequestPrompt(): String
}

data class ControllerContext(
    val services: List<PsiClass>,
    val entities: List<PsiClass>,
)

class BotActionPrompting(
    private val action: ChatBotActionType,
    private val lang: String,
    private val selectedText: String,
    private val file: PsiFile?,
    project: Project,
) : PromptFormatter {
    private val searchScope = GlobalSearchScope.allScope(project)
    private val javaPsiFacade = JavaPsiFacade.getInstance(project)

    private val fileName = file?.name ?: ""

    override fun getUIPrompt(): String {
        val prompt = createPrompt()

        return """$prompt:
         <pre><code>$selectedText</pre></code>
        """.trimMargin()
    }

    override fun getRequestPrompt(): String {
        val prompt = createPrompt()

        return """$prompt:
            $selectedText
        """.trimMargin()
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

            return@runReadAction ControllerContext(
                services = services,
                entities = emptyList()
            )
        }
    }

    private fun createPrompt(): String {
        var prompt = """$action this $lang code"""

        when (action) {
            ChatBotActionType.REVIEW -> {
                prompt = "检查如下的 $lang 代码"
            }

            ChatBotActionType.EXPLAIN -> {
                prompt = "解释如下的 $lang 代码"
            }

            ChatBotActionType.CODE_COMPLETE -> {
                prompt = "补全如下的 $lang 代码"

                val isController = fileName.endsWith("Controller.java")
                val isService = fileName.endsWith("Service.java") || fileName.endsWith("ServiceImpl.java")

                when {
                    isController -> {
                        prompt = createControllerPrompt()
                    }

                    isService -> {
                        prompt = """代码补其 $lang 要求：
                                            |1. 不允许同时使用 BeanUtils 和 Map 转换 DTO
                                            |2. 直接调用 repository 的方法时，使用 get, find, count, delete, save, update 这类方法
                                            |3. Service 层应该捕获并处理可能出现的异常。通常情况下，应该将异常转换为应用程序自定义异常并抛出。
                                            """.trimMargin()
                    }
                }

            }

            else -> {}
        }

        return prompt
    }

    private fun createControllerPrompt(): String {
        val file = file as? PsiJavaFileImpl
        val services = prepareControllerContext(file)
        val servicesList = services?.services?.map {
            DtClass.fromPsiClass(it).format()
        }

        val servicePrompt = if (servicesList.isNullOrEmpty()) {
            """|相关 Service 的信息如下： ```\n$servicesList```""".trimMargin()
        } else {
            ""
        }

        val clazz = DtClass.fromJavaFile(file)
        return """代码补全 $lang 要求：
                |- 在 Controller 中使用 BeanUtils 完成 DTO 的转换
                |- 不允许把 json，map 这类对象传到 service 中 
                |$servicePrompt
                |- // current package: ${clazz.packageName}
                |- // current class: ${clazz.name}
                |- 需要补全的代码如下：
            """.trimMargin()
    }
}