package cc.unitmesh.agent.tool

import kotlinx.serialization.Serializable

/**
 * Enumeration of tool error types, similar to Gemini CLI's ToolErrorType
 */
@Serializable
enum class ToolErrorType(val code: String, val description: String) {
    // File system errors
    FILE_NOT_FOUND("FILE_NOT_FOUND", "The specified file was not found"),
    FILE_ACCESS_DENIED("FILE_ACCESS_DENIED", "Access to the file was denied"),
    DIRECTORY_NOT_FOUND("DIRECTORY_NOT_FOUND", "The specified directory was not found"),
    PATH_INVALID("PATH_INVALID", "The specified path is invalid"),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "The file is too large to process"),
    
    // Execution errors
    COMMAND_NOT_FOUND("COMMAND_NOT_FOUND", "The specified command was not found"),
    COMMAND_FAILED("COMMAND_FAILED", "The command execution failed"),
    TIMEOUT("TIMEOUT", "The operation timed out"),
    PERMISSION_DENIED("PERMISSION_DENIED", "Permission denied for the operation"),
    
    // Parameter errors
    INVALID_PARAMETERS("INVALID_PARAMETERS", "The provided parameters are invalid"),
    MISSING_REQUIRED_PARAMETER("MISSING_REQUIRED_PARAMETER", "A required parameter is missing"),
    PARAMETER_OUT_OF_RANGE("PARAMETER_OUT_OF_RANGE", "A parameter value is out of the valid range"),
    
    // Pattern/search errors
    INVALID_PATTERN("INVALID_PATTERN", "The provided pattern is invalid"),
    PATTERN_TOO_COMPLEX("PATTERN_TOO_COMPLEX", "The pattern is too complex to process"),
    
    // General errors
    UNKNOWN("UNKNOWN", "An unknown error occurred"),
    INTERNAL_ERROR("INTERNAL_ERROR", "An internal error occurred"),
    NOT_SUPPORTED("NOT_SUPPORTED", "The operation is not supported on this platform"),
    CANCELLED("CANCELLED", "The operation was cancelled"),
    
    // Network/external errors
    NETWORK_ERROR("NETWORK_ERROR", "A network error occurred"),
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", "An external service error occurred");
    
    companion object {
        fun fromCode(code: String): ToolErrorType {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}

/**
 * Exception class for tool-related errors
 */
class ToolException(
    message: String,
    val errorType: ToolErrorType = ToolErrorType.UNKNOWN,
    cause: Throwable? = null
) : Exception(message, cause) {

    constructor(errorType: ToolErrorType, cause: Throwable? = null)
        : this(errorType.description, errorType, cause)
    
    fun toToolResult(): ToolResult.Error {
        return ToolResult.Error(
            message = message ?: errorType.description,
            errorType = errorType.code
        )
    }
}

/**
 * Utility functions for error handling
 */
object ToolErrorUtils {
    
    /**
     * Wraps a potentially throwing operation and converts exceptions to ToolResult.Error
     */
    inline fun <T> safeExecute(
        errorType: ToolErrorType = ToolErrorType.UNKNOWN,
        operation: () -> T
    ): ToolResult {
        return try {
            val result = operation()
            when (result) {
                is ToolResult -> result
                is String -> ToolResult.Success(result)
                else -> ToolResult.Success(result.toString())
            }
        } catch (e: ToolException) {
            e.toToolResult()
        } catch (e: Exception) {
            ToolResult.Error(
                message = e.message ?: "Unknown error occurred",
                errorType = errorType.code
            )
        }
    }
    
    /**
     * Converts a generic exception to a ToolException with appropriate error type
     */
    fun mapException(exception: Throwable): ToolException {
        return when {
            exception is ToolException -> exception
            exception.message?.contains("not found", ignoreCase = true) == true -> 
                ToolException(ToolErrorType.FILE_NOT_FOUND, exception)
            exception.message?.contains("permission", ignoreCase = true) == true -> 
                ToolException(ToolErrorType.PERMISSION_DENIED, exception)
            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                ToolException(ToolErrorType.TIMEOUT, exception)
            else -> ToolException(ToolErrorType.UNKNOWN, exception)
        }
    }
}
