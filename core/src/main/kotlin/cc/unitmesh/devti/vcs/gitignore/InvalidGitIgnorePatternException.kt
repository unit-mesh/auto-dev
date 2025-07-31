package cc.unitmesh.devti.vcs.gitignore

/**
 * Exception thrown when a gitignore pattern is malformed or cannot be processed.
 *
 * @param originalPattern the original gitignore pattern that caused the error
 * @param message detailed error message
 * @param cause the underlying cause of the error, if any
 */
class InvalidGitIgnorePatternException(
    val originalPattern: String,
    message: String,
    cause: Throwable? = null
) : Exception("Invalid gitignore pattern '$originalPattern': $message", cause)
