package cc.unitmesh.devti.command

/**
 * Exception thrown when parsing edit requests fails
 */
sealed class ParseException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Thrown when the content format is invalid or unrecognized
     */
    class InvalidFormatException(message: String, cause: Throwable? = null) : ParseException(message, cause)
    
    /**
     * Thrown when required fields are missing
     */
    class MissingFieldException(fieldName: String, cause: Throwable? = null) : 
        ParseException("Required field '$fieldName' is missing", cause)
    
    /**
     * Thrown when field values are invalid
     */
    class InvalidFieldException(fieldName: String, value: String, reason: String, cause: Throwable? = null) : 
        ParseException("Invalid value for field '$fieldName': '$value' - $reason", cause)
    
    /**
     * Thrown when YAML parsing fails
     */
    class YamlParseException(message: String, cause: Throwable? = null) : ParseException(message, cause)
    
    /**
     * Thrown when regex parsing fails
     */
    class RegexParseException(message: String, cause: Throwable? = null) : ParseException(message, cause)
    
    /**
     * Thrown when quote parsing fails
     */
    class QuoteParseException(message: String, cause: Throwable? = null) : ParseException(message, cause)
}
