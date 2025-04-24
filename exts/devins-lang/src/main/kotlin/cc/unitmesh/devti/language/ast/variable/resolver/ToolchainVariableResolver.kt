package cc.unitmesh.devti.language.ast.variable.resolver

import cc.unitmesh.devti.devins.variable.ToolchainVariable
import com.intellij.openapi.diagnostic.logger
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolver
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolverContext
import cc.unitmesh.devti.language.provider.ToolchainVariableProvider

/**
 * Include ToolchainVariableProvider and PsiContextVariableProvider
 */
class ToolchainVariableResolver(
    private val context: VariableResolverContext,
) : VariableResolver {
    override suspend fun resolve(initVariables: Map<String, Any>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        context.variableTable.getAllVariables().forEach {
            val variable = ToolchainVariable.from(it.key) ?: return@forEach
            val provider = ToolchainVariableProvider
                .provide(variable, context.element, context.myProject) ?: return@forEach

            result[it.key] = try {
                val resolvedValue = provider.resolve(variable, context.myProject, context.editor, context.element)
                val value = (resolvedValue as? ToolchainVariable)?.value ?: resolvedValue
                logger<ToolchainVariableResolver>().info("start to resolve variable: $value")
                value
            } catch (e: Exception) {
                logger<ToolchainVariableResolver>().error("Failed to resolve variable: ${it.key}", e)
                ""
            }

        }

        return result
    }
}