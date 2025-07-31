package cc.unitmesh.devti.vcs.gitignore

/**
 * Represents a single gitignore rule that can match file paths.
 */
interface IgnoreRule {
    /**
     * Checks if this rule matches the given file path.
     *
     * @param filePath the file path to check
     * @return true if the rule matches the file path, false otherwise
     */
    fun matches(filePath: String): Boolean

    /**
     * Gets the original pattern string for this rule.
     *
     * @return the original gitignore pattern
     */
    fun getPattern(): String

    /**
     * Indicates whether this is a negation rule (starts with !).
     *
     * @return true if this is a negation rule, false otherwise
     */
    fun isNegated(): Boolean
}
