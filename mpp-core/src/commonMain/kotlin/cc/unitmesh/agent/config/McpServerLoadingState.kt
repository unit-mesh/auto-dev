package cc.unitmesh.agent.config

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Platform-specific function to get current time in milliseconds
 */
fun getCurrentTimeMillis() = Clock.System.now().toEpochMilliseconds()

/**
 * Represents the loading state of an individual MCP server
 */
@Serializable
enum class McpServerLoadingStatus {
    /** Server has not started loading yet */
    AVAILABLE,

    /** Server is currently loading tools */
    LOADING,

    /** Server has successfully loaded tools */
    LOADED,

    /** Server failed to load tools */
    ERROR,

    /** Server is disabled */
    DISABLED
}

/**
 * Represents the loading state and tools for a single MCP server
 */
@Serializable
data class McpServerState(
    val serverName: String,
    val status: McpServerLoadingStatus,
    val tools: List<ToolItem> = emptyList(),
    val errorMessage: String? = null,
    val loadingStartTime: Long? = null,
    val loadingEndTime: Long? = null
) {
    val isLoading: Boolean get() = status == McpServerLoadingStatus.LOADING
    val isLoaded: Boolean get() = status == McpServerLoadingStatus.LOADED
    val hasError: Boolean get() = status == McpServerLoadingStatus.ERROR
    val isDisabled: Boolean get() = status == McpServerLoadingStatus.DISABLED

    val loadingDuration: Long?
        get() = if (loadingStartTime != null && loadingEndTime != null) {
            loadingEndTime - loadingStartTime
        } else null
}

/**
 * Represents the overall loading state of all MCP servers
 */
@Serializable
data class McpLoadingState(
    val servers: Map<String, McpServerState> = emptyMap(),
    val builtinToolsLoaded: Boolean = false
) {
    val allServersLoaded: Boolean
        get() = servers.values.all { it.status != McpServerLoadingStatus.LOADING }

    val hasAnyErrors: Boolean
        get() = servers.values.any { it.hasError }

    val loadingServers: List<String>
        get() = servers.values.filter { it.isLoading }.map { it.serverName }

    val loadedServers: List<String>
        get() = servers.values.filter { it.isLoaded }.map { it.serverName }

    val errorServers: List<String>
        get() = servers.values.filter { it.hasError }.map { it.serverName }

    val totalTools: Int
        get() = servers.values.sumOf { it.tools.size }

    val enabledTools: Int
        get() = servers.values.sumOf { serverState ->
            serverState.tools.count { it.enabled }
        }

    fun getServerState(serverName: String): McpServerState? = servers[serverName]

    fun updateServerState(serverName: String, newState: McpServerState): McpLoadingState {
        return copy(servers = servers + (serverName to newState))
    }

    fun updateServerStatus(
        serverName: String,
        status: McpServerLoadingStatus,
        errorMessage: String? = null
    ): McpLoadingState {
        val currentState = servers[serverName] ?: McpServerState(serverName, McpServerLoadingStatus.AVAILABLE)
        val updatedState = currentState.copy(
            status = status,
            errorMessage = errorMessage,
            loadingStartTime = if (status == McpServerLoadingStatus.LOADING) getCurrentTimeMillis() else currentState.loadingStartTime,
            loadingEndTime = if (status == McpServerLoadingStatus.LOADED || status == McpServerLoadingStatus.ERROR) getCurrentTimeMillis() else null
        )
        return updateServerState(serverName, updatedState)
    }

    fun updateServerTools(serverName: String, tools: List<ToolItem>): McpLoadingState {
        val currentState = servers[serverName] ?: McpServerState(serverName, McpServerLoadingStatus.AVAILABLE)
        val updatedState = currentState.copy(
            tools = tools,
            status = McpServerLoadingStatus.LOADED,
            loadingEndTime = getCurrentTimeMillis()
        )
        return updateServerState(serverName, updatedState)
    }
}

/**
 * Callback interface for MCP loading state updates
 */
interface McpLoadingStateCallback {
    /**
     * Called when a server's loading state changes
     */
    fun onServerStateChanged(serverName: String, state: McpServerState)

    /**
     * Called when the overall loading state changes
     */
    fun onLoadingStateChanged(loadingState: McpLoadingState)

    /**
     * Called when builtin tools are loaded
     */
    fun onBuiltinToolsLoaded(tools: List<ToolItem>)
}
