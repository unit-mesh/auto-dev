package cc.unitmesh.devti.language.ast

import cc.unitmesh.devti.devins.post.PostProcessorContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

data class Condition(
    val conditionKey: String,
    val valueExpression: FrontMatterType.EXPRESSION,
)

sealed class Task(open val expression: FrontMatterType.EXPRESSION?) {
    class CustomTask(override val expression: FrontMatterType.EXPRESSION?) : Task(expression)
    class Default(override val expression: FrontMatterType.EXPRESSION?) : Task(expression)
}

data class Case(
    val caseKey: String,
    val valueExpression: Task,
)

data class TaskRoutes(
    val conditions: List<Condition>,
    val cases: List<Case>,
    /**
     * A placeholder for the default task
     */
    val defaultTask: Task? = null,
) {
    fun execute(myProject: Project, context: PostProcessorContext, hobbitHole: HobbitHole): Any? {
        val conditionResult = mutableMapOf<String, Any?>()
        /// todo: get processor variable table
        val variableTable = context.compiledVariables.toMutableMap()
        variableTable["output"] = context.genText

        val processor = FunctionStatementProcessor(myProject, hobbitHole)
        conditions.forEach {
            val statement = it.valueExpression.value as Statement
            val result = processor.execute(statement, variableTable)
            conditionResult[it.conditionKey] = result
        }

        val matchedCase = cases.filter {
            val caseKey = it.caseKey

            when (val condValue = conditionResult[caseKey]) {
                is Boolean -> {
                    condValue == true || condValue == "true"
                }

                is String -> {
                    condValue.isNotEmpty()
                }

                else -> {
                    false
                }
            }
        }

        var result: Any? = null
        if (matchedCase.isEmpty()) {
            val result = ((defaultTask as? Task.Default)?.expression?.value as? Statement)?.let {
                processor.execute(it, variableTable)
            }

            logger<TaskRoutes>().info("no matched case, execute default task: $result")
            return result
        }

        matchedCase.forEach {
            val statement = (it.valueExpression as Task.CustomTask).expression?.value as Statement
            result = processor.execute(statement, variableTable)
        }

        return result
    }

    companion object {
        fun from(expression: FrontMatterType.ARRAY): TaskRoutes? {
            val arrays = expression.value as List<FrontMatterType>
            val taskRoutes = arrays.filterIsInstance<FrontMatterType.EXPRESSION>()
                .mapNotNull { caseExpr ->
                    when (val value = caseExpr.value) {
                        is ConditionCase -> {
                            transformConditionCasesToRoutes(value)
                        }
                        else -> {
                            null
                        }
                    }
                }

            return taskRoutes.firstOrNull()
        }

        /**
         * Transforms a given [ConditionCase] into a [TaskRoutes] object which contains a structured set of conditions and corresponding tasks.
         *
         * @param conditionCase The [ConditionCase] object to transform. This object contains conditions and cases that determine routing logic.
         * @return A [TaskRoutes] object that encapsulates the transformed conditions and cases, along with a default task if specified.
         */
        private fun transformConditionCasesToRoutes(conditionCase: ConditionCase): TaskRoutes {
            val conditions: List<Condition> = conditionCase.conditions.map {
                val caseKeyValue = it.value as CaseKeyValue
                Condition(caseKeyValue.key.display(), caseKeyValue.value)
            }

            var defaultTask: Task? = null

            val cases: List<Case> = conditionCase.cases.map {
                val caseKeyValue = it.value as CaseKeyValue
                val caseKey = caseKeyValue.key.display()
                if (caseKey == "default") {
                    defaultTask = Task.Default(caseKeyValue.value)
                }

                Case(caseKey, Task.CustomTask(caseKeyValue.value))
            }

            return TaskRoutes(
                conditions = conditions,
                cases = cases,
                defaultTask = defaultTask
            )
        }
    }
}
