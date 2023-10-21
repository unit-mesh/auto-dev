package cc.unitmesh.devti.custom.team

interface TeamContextProvider {
    /**
     * Retrieves the code of the target file associated with the given test name.
     *
     * @param testName the name of the test
     * @return the code of the target file as a string
     */
    fun getTargetFileCode(testName: String): String

    /**
     * Retrieves the code of the target method associated with the given test name.
     *
     * @param testName the name of the test for which to retrieve the target method code
     * @return the code of the target method as a string
     */
    fun getTargetMethodCode(testName: String): String
}