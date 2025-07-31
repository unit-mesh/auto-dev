package cc.unitmesh.devti.vcs.gitignore

import nl.basjes.gitignore.GitIgnore

/**
 * Wrapper around the nl.basjes.gitignore library to implement the IgnoreEngine interface.
 * This serves as the fallback engine for the dual-engine architecture.
 */
class BasjesIgnoreEngine : IgnoreEngine {
    private var gitIgnore: GitIgnore = GitIgnore("")
    private val rules = mutableListOf<String>()

    override fun isIgnored(filePath: String): Boolean {
        return try {
            gitIgnore.isIgnoredFile(filePath)
        } catch (e: Exception) {
            // If the library fails, default to not ignored
            false
        }
    }

    override fun addRule(pattern: String) {
        rules.add(pattern)
        rebuildGitIgnore()
    }

    override fun removeRule(pattern: String) {
        rules.remove(pattern)
        rebuildGitIgnore()
    }

    override fun getRules(): List<String> {
        return rules.toList()
    }

    override fun clearRules() {
        rules.clear()
        gitIgnore = GitIgnore("")
    }

    override fun loadFromContent(gitIgnoreContent: String) {
        clearRules()

        val lines = gitIgnoreContent.lines()
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty()) {
                rules.add(trimmedLine)
            }
        }

        rebuildGitIgnore()
    }

    /**
     * Rebuilds the internal GitIgnore instance with current rules.
     * This is necessary because the nl.basjes.gitignore library doesn't support
     * dynamic rule addition/removal.
     */
    private fun rebuildGitIgnore() {
        val content = rules.joinToString("\n")
        gitIgnore = GitIgnore(content)
    }
    
    /**
     * Gets the number of active rules.
     *
     * @return the number of rules
     */
    fun getRuleCount(): Int = rules.size
    
    /**
     * Gets statistics about the engine for debugging/monitoring.
     *
     * @return a map of statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "ruleCount" to getRuleCount(),
            "engineType" to "Basjes"
        )
    }
}
