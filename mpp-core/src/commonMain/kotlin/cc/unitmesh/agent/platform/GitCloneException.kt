package cc.unitmesh.agent.platform

/**
 * Git clone error types for UI handling
 */
enum class GitCloneErrorType {
    /**
     * Git module failed to initialize
     */
    INITIALIZATION_ERROR,
    
    /**
     * CORS policy blocked the request (browser-specific)
     */
    CORS_ERROR,
    
    /**
     * Network error (timeout, connection refused, etc.)
     */
    NETWORK_ERROR,
    
    /**
     * Git clone command failed
     */
    CLONE_FAILED
}

/**
 * Exception thrown during git clone operations with detailed error type
 * 
 * This allows UI layer to provide context-specific error messages and
 * suggestions to users.
 */
class GitCloneException(
    message: String,
    val errorType: GitCloneErrorType
) : Exception(message)

