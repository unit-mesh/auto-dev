package cc.unitmesh.devti.flow

/**
 * AutoDev TaskFlow is the built-in Workflow for AutoDev, see in [#81](https://github.com/unit-mesh/auto-dev/issues/81)
 */
interface TaskFlow {
    /**
     * This method is used to clarify the purpose of user's requirement.
     * It returns a string will specify format, will parse in design()
     * For example, tableNames in SQL:
     * ```markdown
     * [item1, item2, item3]
     * ```
     * @return A string representing the documentation for the class.
     */
    fun clarify(): String

    /**
     * This method is used to ask LLM to design the tasking flow based on the given context.
     * It returns a string will specify format, will parse in execute()
     *
     * @param context The context for designing the task flow.
     */
    fun design(context: Any): String {
        return ""
    }

    /**
     * Executes the last step of the task flow.
     *
     * Like:
     * - Execute SQL in [GenSqlFlow]
     * - Execute HTTP request in [GenHttpFlow]
     * - Execute shell command in [GenShellFlow]
     *
     * @return A string representing the result of the execution.
     */
    fun execute(): String {
        return ""
    }
}