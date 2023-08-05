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
     * for align correct [DtModel]
     */
    @Deprecated("not used", ReplaceWith("toQuery()"))
    fun toUML(): String = toQuery()
}
