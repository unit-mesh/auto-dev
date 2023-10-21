package cc.unitmesh.devti.custom.team

class SimpleTeamContextProvider : TeamContextProvider {
    /**
     * Retrieves the code of the target file associated with the given test name.
     */
    override fun getTargetFileCode(fileName: String): String {
        return "TODO"
    }

    override fun getTargetMethodCode(testName: String): String {
        return "TODO"
    }
}