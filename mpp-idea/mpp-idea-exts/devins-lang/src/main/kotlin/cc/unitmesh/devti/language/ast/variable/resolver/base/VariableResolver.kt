package cc.unitmesh.devti.language.ast.variable.resolver.base

/**
 * The `VariableResolver` interface is designed to provide a mechanism for resolving variables.
 */
interface VariableResolver {
    suspend fun resolve(initVariables: Map<String, Any>): Map<String, Any>
}
