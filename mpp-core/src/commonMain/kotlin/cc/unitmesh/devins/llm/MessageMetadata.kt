package cc.unitmesh.devins.llm

import kotlinx.serialization.Serializable

/**
 * Timeline item type for message metadata
 */
@Serializable
enum class TimelineItemType {
    MESSAGE,
    COMBINED_TOOL,
    TOOL_RESULT,
    TOOL_ERROR,
    TASK_COMPLETE,
    TERMINAL_OUTPUT,
    LIVE_TERMINAL
}

/**
 * Metadata for messages to preserve timeline item structure
 * This allows messages to be reconstructed with their original UI representation
 */
@Serializable
data class MessageMetadata(
    // Timeline item type
    val itemType: TimelineItemType,

    // Common fields for tool-related items
    val toolName: String? = null,
    val description: String? = null,
    val details: String? = null,
    val fullParams: String? = null,
    val filePath: String? = null,
    val toolType: String? = null,

    // Result fields
    val success: Boolean? = null,
    val summary: String? = null,
    val output: String? = null,
    val fullOutput: String? = null,
    val executionTimeMs: Long? = null,

    // Terminal fields
    val command: String? = null,
    val exitCode: Int? = null,

    // Task complete fields
    val taskSuccess: Boolean? = null,
    val taskMessage: String? = null,

    // Token info fields (for MessageItem)
    val tokenInfoTotal: Int? = null,
    val tokenInfoInput: Int? = null,
    val tokenInfoOutput: Int? = null,

    // DocQL stats fields (for CombinedToolItem with docql)
    val docqlSearchType: String? = null,
    val docqlQuery: String? = null,
    val docqlDocumentPath: String? = null,
    val docqlChannels: String? = null,
    val docqlDocsSearched: Int? = null,
    val docqlRawResults: Int? = null,
    val docqlRerankedResults: Int? = null,
    val docqlTruncated: Boolean? = null,
    val docqlUsedFallback: Boolean? = null,
    val docqlDetailedResults: String? = null,
    val docqlSmartSummary: String? = null
)
