package cc.unitmesh.devti.language.ast.variable.resolver

import cc.unitmesh.devti.language.ast.action.PatternActionProcessor
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolver
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolverContext
import cc.unitmesh.devti.language.ast.snapshot.VariableSnapshotRecorder

class UserCustomVariableResolver(
    private val context: VariableResolverContext,
) : VariableResolver {
    private val record = VariableSnapshotRecorder.getInstance(context.myProject)
    override suspend fun resolve(initVariables: Map<String, Any>): Map<String, String> {
        record.clear()

        val vars: MutableMap<String, Any?> = initVariables.toMutableMap()
        return context.hole?.variables?.mapValues {
            PatternActionProcessor(context.myProject, context.hole, vars).execute(it.value)
        } ?: emptyMap()
    }
}