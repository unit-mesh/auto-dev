package cc.unitmesh.devti.vcs.gitignore

/**
 * Interface for gitignore engines that can determine if files should be ignored.
 * This interface supports both custom high-performance engines and third-party library engines.
 */
interface IgnoreEngine {
    /**
     * Determines if a file path should be ignored based on the configured rules.
     *
     * @param filePath the file path to check (relative or absolute)
     * @return true if the file should be ignored, false otherwise
     */
    fun isIgnored(filePath: String): Boolean

    /**
     * Adds a new ignore rule to the engine.
     *
     * @param pattern the gitignore pattern to add
     */
    fun addRule(pattern: String)

    /**
     * Removes an ignore rule from the engine.
     *
     * @param pattern the gitignore pattern to remove
     */
    fun removeRule(pattern: String)

    /**
     * Gets all currently configured rules.
     *
     * @return list of all rule patterns
     */
    fun getRules(): List<String>

    /**
     * Clears all rules from the engine.
     */
    fun clearRules()

    /**
     * Loads rules from gitignore content.
     *
     * @param gitIgnoreContent the content of a .gitignore file
     */
    fun loadFromContent(gitIgnoreContent: String)
}
