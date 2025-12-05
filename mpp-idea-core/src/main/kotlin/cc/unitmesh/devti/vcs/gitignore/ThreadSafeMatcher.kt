package cc.unitmesh.devti.vcs.gitignore

import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.concurrent.withLock

/**
 * A thread-safe wrapper around regex Pattern that ensures safe concurrent access to Matcher instances.
 * Since Matcher objects are not thread-safe, this class uses a lock to synchronize access.
 */
class ThreadSafeMatcher(private val pattern: Pattern) {
    private val lock = ReentrantLock()

    /**
     * Performs a thread-safe match operation on the input string.
     *
     * @param input the string to match against the pattern
     * @return true if the pattern matches the input, false otherwise
     */
    fun matches(input: String): Boolean {
        return lock.withLock {
            pattern.matcher(input).matches()
        }
    }

    /**
     * Performs a thread-safe find operation on the input string.
     *
     * @param input the string to search in
     * @return true if the pattern is found in the input, false otherwise
     */
    fun find(input: String): Boolean {
        return lock.withLock {
            pattern.matcher(input).find()
        }
    }

    /**
     * Gets the underlying pattern.
     *
     * @return the regex pattern
     */
    fun getPattern(): Pattern = pattern

    /**
     * Gets the pattern string.
     *
     * @return the pattern as a string
     */
    fun getPatternString(): String = pattern.pattern()
}
