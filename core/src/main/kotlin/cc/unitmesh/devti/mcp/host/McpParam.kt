package cc.unitmesh.devti.mcp.host

/**
 * Annotation to provide description for MCP tool parameters
 * @param description The description of the parameter that will be exposed to MCP clients
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class McpParam(val description: String)