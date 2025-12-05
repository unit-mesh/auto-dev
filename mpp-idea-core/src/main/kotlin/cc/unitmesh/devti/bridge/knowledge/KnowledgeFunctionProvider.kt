package cc.unitmesh.devti.bridge.knowledge

import cc.unitmesh.devti.bridge.KnowledgeTransfer
import cc.unitmesh.devti.bridge.provider.KnowledgeWebApiProvider
import cc.unitmesh.devti.bridge.utils.StructureCommandUtil
import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import cc.unitmesh.devti.util.relativePath
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

val API_METHODS: List<String> = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE")

/**
 * The `KnowledgeFunctionProvider` class is a specialized toolchain function provider designed to analyze API call chains and code structure relationships within a project.
 * It supports two primary modes of analysis:
 *
 * 1. **API Endpoint Analysis**: This mode tracks the call chain by HTTP method and path, allowing for the identification of how API endpoints are processed through the system.
 * 2. **Code Structure Analysis**: This mode tracks relationships between classes and methods, enabling the exploration of how different parts of the codebase interact.
 *
 * The class is particularly useful for understanding the flow of data and control within a project, especially in scenarios where API calls traverse multiple layers of the application, such as from a Controller to a Repository.
 *
 * ### Usage Examples:
 * - **API Endpoint Analysis**: `/knowledge:GET#/api/blog`
 *   This example traces the call chain for a GET request to the `/api/blog` endpoint.
 * - **Code Structure Analysis**: `/knowledge:BlogController#getBlogBySlug`
 *   This example traces the call chain from the `getBlogBySlug` method in the `BlogController` class.
 *
 * ### Example Output:
 * The output of the `execute` method is a string that includes the related code snippets, their paths, and the language of the code. For example:
 * <devin>
 * Here is /knowledge:GET#/api/blog related code:
 * ```Kotlin
 * // src/main/kotlin/com/example/BlogController.kt
 * @GetMapping("/api/blog")
 * fun getBlogBySlug(@PathVariable slug: String): Blog {
 *     return blogService.getBlogBySlug(slug)
 * }
 * ```
 * </devin>
 *
 */
class KnowledgeFunctionProvider : ToolchainFunctionProvider {
    override suspend fun funcNames(): List<String> = listOf(KnowledgeTransfer.Knowledge.name)

    override suspend fun isApplicable(project: Project, funcName: String): Boolean =
        funcName == KnowledgeTransfer.Knowledge.name

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String
    ): Any {
        val split = prop.split("#")
        if (split.size != 2) {
            val lookupFile = project.lookupFile(prop) ?: return "Invalid API format or File not found"
            if (lookupFile.isValid) {
                /// since VueFile can only find by File Usage
                val psiFile = runReadAction { PsiManager.getInstance(project).findFile(lookupFile) }
                    ?: return "Invalid API format or File not found or PsiFile not found"

                val elements = RelatedClassesProvider.provide(psiFile.language)?.lookupIO(psiFile)
                return elements?.joinToString("\n") {
                    StructureCommandUtil.getFileStructure(project, lookupFile, psiFile)
                } ?: "No related classes found"
            }

            return "Invalid API format"
        }

        val method = split[0]
        if (!API_METHODS.contains(method)) {
            return "Invalid API method"
        }

        val path = split[1]

        val psiElements = KnowledgeWebApiProvider.available(project).map {
            it.lookupApiCallTree(project, method, path)
        }.flatten()

        val elementText = psiElements.joinToString("\n") {
            runReadAction {
                val path = it.containingFile.virtualFile?.relativePath(project)
                if (path != null) {
                    "${commentSymbol(it)} " + path + "\n" + it.text
                } else {
                    it.text
                }
            }
        }

        val lang = psiElements.firstOrNull()?.language?.displayName ?: ""
        return "Here is $prop related code:\n```$lang\n$elementText\n```"
    }

    fun commentSymbol(element: PsiElement): String {
        return LanguageCommenters.INSTANCE.forLanguage(element.language)?.lineCommentPrefix ?: "//"
    }
}