package cc.unitmesh.devti.bridge.knowledge

import cc.unitmesh.devti.bridge.KnowledgeTransfer
import cc.unitmesh.devti.bridge.provider.KnowledgeWebApiProvider
import cc.unitmesh.devti.bridge.utils.StructureCommandUtil
import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

val API_METHODS: List<String> = listOf("GET", "POST", "PUT", "DELETE", "PATCH")

/**
 * ```devin
 * 从 API 调用链来进行分析
 * /knowledge:GET#/api/blog
 * 从 Controller 到 Repository 的调用链
 * /knowledge:BlogController#getBlogBySlug
 * ```
 */
class KnowledgeFunctionProvider : ToolchainFunctionProvider {
    override fun funcNames(): List<String> = listOf(KnowledgeTransfer.Knowledge.name)

    override fun isApplicable(project: Project, funcName: String): Boolean =
        funcName == KnowledgeTransfer.Knowledge.name


    /**
     * 1. try use KnowledgeWebApiProvider
     *
     * 2. try use RipGrep Search by APIs
     *
     */
    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        // split prop to method and path
        val split = prop.split("#")
        if (split.size != 2) {
            val lookupFile = project.lookupFile(prop) ?: return "Invalid API format or File not found"
            if (lookupFile.isValid) {
                /// since VueFile can only find by File Usage
                val psiFile = runReadAction { PsiManager.getInstance(project).findFile(lookupFile) }
                    ?: return "Invalid API format or File not found or PsiFile not found"
                return RelatedClassesProvider.provide(psiFile.language)?.lookupIO(psiFile)?.joinToString("\n") {
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

        val elementText = KnowledgeWebApiProvider.available(project).map {
            it.lookupApiCallTree(project, method, path)
        }.flatten().joinToString("\n") {
            it.text
        }

        return elementText
    }
}