package cc.unitmesh.devti.prompting.code

/**
 * TODO: change to dependency tree
 */
data class TechStack(
    val coreFrameworks: MutableMap<String, Boolean> = mutableMapOf(),
    val testFrameworks: MutableMap<String, Boolean> = mutableMapOf(),
    val deps: MutableMap<String, String> = mutableMapOf(),
    val devDeps: MutableMap<String, String> = mutableMapOf()
) {
    fun coreFrameworks(): String {
        return coreFrameworks.keys.joinToString(", ")
    }

    fun testFrameworks(): String {
        return testFrameworks.keys.joinToString(", ")
    }
}