package cc.unitmesh.devti.language.ast.variable.resolver

import cc.unitmesh.devti.devins.variable.SystemInfoVariable
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolver
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolverContext

/**
 * SystemInfoVariableResolver is a class that provides a way to resolve system information variables.
 */
class SystemInfoVariableResolver(
    private val context: VariableResolverContext,
) : VariableResolver {
    override suspend fun resolve(initVariables: Map<String, Any>): Map<String, Any> {
        return SystemInfoVariable.all().associate {
            it.variableName to it.value!!
        }
    }
}