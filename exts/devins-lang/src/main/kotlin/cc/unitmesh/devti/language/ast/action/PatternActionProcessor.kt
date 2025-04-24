package cc.unitmesh.devti.language.ast.action

import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.VariableTransform
import cc.unitmesh.devti.language.ast.shireql.ShireQLProcessor
import cc.unitmesh.devti.language.compiler.searcher.PatternSearcher
import cc.unitmesh.devti.language.debugger.snapshot.VariableSnapshotRecorder
import cc.unitmesh.devti.devins.post.PostProcessorContext
import com.intellij.openapi.project.Project

public class PatternActionProcessor(
    override val myProject: Project,
    override val hole: HobbitHole,
    private val variableMap: MutableMap<String, Any?>
) :
    PatternFuncProcessor(myProject, hole) {

    private val record: VariableSnapshotRecorder = VariableSnapshotRecorder.getInstance(myProject)


    /**
     * We should execute the variable function with the given key and pipeline functions.
     *
     * Each function output will be the input of the next function.
     */
    suspend fun execute(actionTransform: VariableTransform): String {
        if (actionTransform.patternActionFuncs.isEmpty()) {
            return ""
        }

        if (actionTransform.isQueryStatement) {
            return ShireQLProcessor(myProject, hole).execute(actionTransform)
        }

        var input: Any = ""
        if (actionTransform.pattern.isNotBlank() && actionTransform.pattern != "any" && actionTransform.pattern != "null") {
            input = PatternSearcher.findFilesByRegex(myProject, actionTransform.pattern)
                .map { it.path }
                .toTypedArray()
        }

        return execute(actionTransform, input)
    }

    /**
     * This method is used to execute a series of transformations on the input based on the provided PatternActionTransform.
     * The transformations are applied in the order they are defined in the PatternActionTransform.
     * The input can be of any type, but the transformations are applied as if the input is a String.
     * If the input is not a String, it will be converted to a String before applying the transformations.
     * The result of each transformation is used as the input for the next transformation.
     * If the transformation is a Cat, the executeCatFunc method is called with the action and the original input.
     * The result of the last transformation is returned as a String.
     *
     * @param transform The PatternActionTransform that defines the transformations to be applied.
     * @param input The input on which the transformations are to be applied.
     * @return The result of applying the transformations to the input as a String.
     */
    suspend fun execute(transform: VariableTransform, input: Any): String {
        record.addSnapshot(transform.variable, input)

        var result = input
        val data = PostProcessorContext.getData()
        if (data?.lastTaskOutput != null && data.lastTaskOutput != "null") {
            if (variableMap["output"] == null) {
                variableMap["output"] = data.lastTaskOutput
            }
        }

        transform.patternActionFuncs.forEach { action ->
            result = patternFunctionExecute(action, result, input, variableMap)
            record.addSnapshot(transform.variable, result, action.funcName, result)
        }

        variableMap[transform.variable] = result
        return result.toString()
    }
}
