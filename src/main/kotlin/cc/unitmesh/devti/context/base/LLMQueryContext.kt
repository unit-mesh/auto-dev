package cc.unitmesh.devti.context.base

interface LLMQueryContext {
    /**
     * convert to prompt query string
     */
    fun toQuery(): String

    /**
     * convert to json string
     */
    fun toJson(): String

    /**
     * to UML
     */
    fun toUML(): String?
}
