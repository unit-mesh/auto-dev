package cc.unitmesh.devti.custom.team

interface TeamContextProvider {
    /**
     * Retrieves the code of the target file associated with the given test name.
     *
     * @param methodName the name of the test
     * @return the code of the target file as a string
     */
    fun underTestFileCode(methodName: String): String

    /**
     * Retrieves the code of the target method associated with the given test name.
     *
     * @param methodName the name of the test for which to retrieve the target method code
     * @return the code of the target method as a string
     */
    fun underTestMethodCode(methodName: String): String

    /**
     * Returns a list of similar chunks.
     *
     * This method retrieves a list of similar chunks based on a certain criteria. The chunks are represented as strings.
     *
     * @return a list of similar chunks as strings
     */
    fun similarChunks(): String

    /**
     * Returns the related code for the given method.
     *
     * This method retrieves the related code that is associated with the current method. The related code
     * can be used to understand the context or dependencies of the method.
     *
     * @return The related code as a string.
     */
    fun relatedCode(): String
}