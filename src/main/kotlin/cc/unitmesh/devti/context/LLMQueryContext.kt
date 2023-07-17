package cc.unitmesh.devti.context

interface LLMQueryContext {
    /**
     * convert to prompt query string
     */
    fun toQuery(): String

    /**
     * convert to json string
     */
    fun toJson(): String
}
