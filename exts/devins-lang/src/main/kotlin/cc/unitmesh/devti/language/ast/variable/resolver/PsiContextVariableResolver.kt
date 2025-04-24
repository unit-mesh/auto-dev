package cc.unitmesh.devti.language.ast.variable.resolver

import cc.unitmesh.devti.language.ast.variable.DefaultPsiContextVariableProvider
import cc.unitmesh.devti.devins.variable.PsiContextVariable
import cc.unitmesh.devti.language.ast.variable.PsiContextVariableProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiManager
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolver
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolverContext
import cc.unitmesh.devti.util.virtualFile

/**
 * Include ToolchainVariableProvider and PsiContextVariableProvider
 */
class PsiContextVariableResolver(private val context: VariableResolverContext) : VariableResolver {
    private val variableProvider: PsiContextVariableProvider

    init {
        val psiFile = runReadAction {
            PsiManager.getInstance(context.myProject).findFile(virtualFile(context.editor) ?: return@runReadAction null)
        }

        variableProvider = if (psiFile?.language != null) {
            PsiContextVariableProvider.provide(psiFile.language)
        } else {
            DefaultPsiContextVariableProvider()
        }
    }

    override suspend fun resolve(initVariables: Map<String, Any>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        context.variableTable.getAllVariables().forEach {
            val psiContextVariable = PsiContextVariable.from(it.key)
            if (psiContextVariable != null) {
                result[it.key] = try {
                    runReadAction {
                        variableProvider.resolve(psiContextVariable, context.myProject, context.editor, context.element)
                    }
                } catch (e: Exception) {
                    logger<CompositeVariableResolver>().error("Failed to resolve variable: ${it.key}", e)
                    ""
                }

                return@forEach
            }
        }

        return result
    }
}