package cc.unitmesh.devins.idea.toolwindow.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

/**
 * Main content view for Knowledge Agent in IntelliJ IDEA.
 * Provides document browsing, search, and AI-powered document querying.
 *
 * Layout:
 * - Left: Document list with search
 * - Center: Document content viewer
 * - Right: AI Chat interface
 */
@Composable
fun IdeaKnowledgeContent(
    viewModel: IdeaKnowledgeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val timeline by viewModel.renderer.timeline.collectAsState()
    val streamingOutput by viewModel.renderer.currentStreamingOutput.collectAsState()

    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // Left panel: Document list with search
        DocumentListPanel(
            documents = state.filteredDocuments,
            selectedDocument = state.selectedDocument,
            searchQuery = state.searchQuery,
            isLoading = state.isLoading,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onDocumentSelect = { viewModel.selectDocument(it) },
            onRefresh = { viewModel.refreshDocuments() },
            modifier = Modifier.width(250.dp)
        )

        Divider(Orientation.Vertical, modifier = Modifier.fillMaxHeight().width(1.dp))

        // Center panel: Document content viewer
        DocumentContentPanel(
            document = state.selectedDocument,
            content = state.documentContent ?: state.parsedContent,
            isLoading = state.isLoading,
            targetLineNumber = state.targetLineNumber,
            highlightedText = state.highlightedText,
            onTocItemClick = { viewModel.navigateToTocItem(it) },
            modifier = Modifier.weight(1f)
        )

        Divider(Orientation.Vertical, modifier = Modifier.fillMaxHeight().width(1.dp))

        // Right panel: AI Chat interface
        AIChatPanel(
            timeline = timeline,
            streamingOutput = streamingOutput,
            isGenerating = state.isGenerating,
            onSendMessage = { viewModel.sendMessage(it) },
            onStopGeneration = { viewModel.stopGeneration() },
            onClearHistory = { viewModel.clearChatHistory() },
            modifier = Modifier.width(400.dp)
        )
    }
}

/**
 * Document list panel with search functionality
 */
@Composable
private fun DocumentListPanel(
    documents: List<IdeaDocumentFile>,
    selectedDocument: IdeaDocumentFile?,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onDocumentSelect: (IdeaDocumentFile) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(8.dp)
    ) {
        // Header with title and refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Documents",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = IdeaComposeIcons.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(16.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search input
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search documents...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Document count
        Text(
            text = "${documents.size} documents",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                color = JewelTheme.globalColors.text.info
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Document list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", style = JewelTheme.defaultTextStyle)
            }
        } else if (documents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No documents found",
                    style = JewelTheme.defaultTextStyle.copy(
                        color = JewelTheme.globalColors.text.info
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(documents, key = { it.path }) { doc ->
                    DocumentListItem(
                        document = doc,
                        isSelected = doc.path == selectedDocument?.path,
                        onClick = { onDocumentSelect(doc) }
                    )
                }
            }
        }
    }
}

/**
 * Single document list item
 */
@Composable
private fun DocumentListItem(
    document: IdeaDocumentFile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f)
    } else {
        JewelTheme.globalColors.panelBackground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // File icon based on type
        val icon = when (document.metadata.formatType) {
            "MARKDOWN" -> IdeaComposeIcons.Description
            "PDF" -> IdeaComposeIcons.Description
            "SOURCE_CODE" -> IdeaComposeIcons.Code
            else -> IdeaComposeIcons.Description
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isSelected) AutoDevColors.Blue.c400 else JewelTheme.globalColors.text.info
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.name,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1
            )
            Text(
                text = document.path,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = JewelTheme.globalColors.text.info
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Document content viewer panel
 */
@Composable
private fun DocumentContentPanel(
    document: IdeaDocumentFile?,
    content: String?,
    isLoading: Boolean,
    targetLineNumber: Int?,
    highlightedText: String?,
    onTocItemClick: (cc.unitmesh.devins.document.TOCItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        if (document == null) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = IdeaComposeIcons.Description,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = JewelTheme.globalColors.text.info.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Select a document to view",
                        style = JewelTheme.defaultTextStyle.copy(
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                }
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading document...", style = JewelTheme.defaultTextStyle)
            }
        } else {
            // Document header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = document.name,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = document.path,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                }
            }

            Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

            // TOC panel if available
            if (document.toc.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Table of Contents (${document.toc.size})",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 150.dp)
                    ) {
                        items(document.toc) { tocItem ->
                            Text(
                                text = "${"  ".repeat(tocItem.level - 1)}• ${tocItem.title}",
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTocItemClick(tocItem) }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))
            }

            // Content viewer
            if (content != null) {
                val listState = rememberLazyListState()
                val lines = remember(content) { content.lines() }

                // Auto-scroll to target line
                LaunchedEffect(targetLineNumber) {
                    targetLineNumber?.let { lineNum ->
                        if (lineNum > 0 && lineNum <= lines.size) {
                            listState.animateScrollToItem(lineNum - 1)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(lines.size) { index ->
                        val lineNumber = index + 1
                        val line = lines[index]
                        val isHighlighted = targetLineNumber == lineNumber ||
                            (highlightedText != null && line.contains(highlightedText))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isHighlighted) AutoDevColors.Yellow.c400.copy(alpha = 0.2f)
                                    else JewelTheme.globalColors.panelBackground
                                )
                        ) {
                            // Line number
                            Text(
                                text = lineNumber.toString().padStart(4, ' '),
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 12.sp,
                                    color = JewelTheme.globalColors.text.info
                                ),
                                modifier = Modifier.width(40.dp)
                            )
                            // Line content
                            Text(
                                text = line,
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No content available",
                        style = JewelTheme.defaultTextStyle.copy(
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                }
            }
        }
    }
}

/**
 * AI Chat panel for document queries
 */
@Composable
private fun AIChatPanel(
    timeline: List<JewelRenderer.TimelineItem>,
    streamingOutput: String,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(timeline.size, streamingOutput) {
        if (timeline.isNotEmpty() || streamingOutput.isNotEmpty()) {
            val targetIndex = if (streamingOutput.isNotEmpty()) timeline.size else timeline.lastIndex.coerceAtLeast(0)
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Knowledge Assistant",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            IconButton(onClick = onClearHistory) {
                Icon(
                    imageVector = IdeaComposeIcons.Delete,
                    contentDescription = "Clear history",
                    modifier = Modifier.size(16.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (timeline.isEmpty() && streamingOutput.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ask questions about your documents",
                                style = JewelTheme.defaultTextStyle.copy(
                                    color = JewelTheme.globalColors.text.info
                                )
                            )
                            Text(
                                text = "Examples:",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "• What is the main topic of this document?\n• Summarize the architecture section\n• Find all mentions of 'API'",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 12.sp,
                                    color = JewelTheme.globalColors.text.info
                                )
                            )
                        }
                    }
                }
            } else {
                items(timeline, key = { it.id }) { item ->
                    ChatMessageItem(item)
                }

                // Show streaming output
                if (streamingOutput.isNotEmpty()) {
                    item {
                        StreamingMessageItem(streamingOutput)
                    }
                }
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask about your documents...") },
                modifier = Modifier.weight(1f),
                enabled = !isGenerating
            )

            if (isGenerating) {
                OutlinedButton(onClick = onStopGeneration) {
                    Text("Stop")
                }
            } else {
                DefaultButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}

/**
 * Chat message item renderer
 */
@Composable
private fun ChatMessageItem(item: JewelRenderer.TimelineItem) {
    when (item) {
        is JewelRenderer.TimelineItem.MessageItem -> {
            val isUser = item.role == JewelRenderer.MessageRole.USER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 350.dp)
                        .background(
                            if (isUser) AutoDevColors.Blue.c400.copy(alpha = 0.2f)
                            else JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.content,
                        style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp)
                    )
                }
            }
        }

        is JewelRenderer.TimelineItem.ToolCallItem -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 350.dp)
                        .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusIcon = when (item.success) {
                                true -> "✓"
                                false -> "✗"
                                null -> "⏳"
                            }
                            val statusColor = when (item.success) {
                                true -> AutoDevColors.Green.c400
                                false -> AutoDevColors.Red.c400
                                null -> JewelTheme.globalColors.text.info
                            }
                            Text(
                                text = statusIcon,
                                style = JewelTheme.defaultTextStyle.copy(color = statusColor)
                            )
                            Text(
                                text = item.toolName,
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        if (item.output != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.output.take(200) + if (item.output.length > 200) "..." else "",
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }

        is JewelRenderer.TimelineItem.ErrorItem -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AutoDevColors.Red.c400.copy(alpha = 0.2f))
                    .padding(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = IdeaComposeIcons.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(16.dp),
                        tint = AutoDevColors.Red.c400
                    )
                    Text(
                        text = item.message,
                        style = JewelTheme.defaultTextStyle.copy(
                            color = AutoDevColors.Red.c400,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        is JewelRenderer.TimelineItem.TaskCompleteItem -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (item.success) AutoDevColors.Green.c400.copy(alpha = 0.2f)
                        else AutoDevColors.Red.c400.copy(alpha = 0.2f)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.message} (${item.iterations} iterations)",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }

        is JewelRenderer.TimelineItem.TerminalOutputItem -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AutoDevColors.Neutral.c900)
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "$ ${item.command}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            color = AutoDevColors.Cyan.c400,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = item.output.take(500) + if (item.output.length > 500) "..." else "",
                        style = JewelTheme.defaultTextStyle.copy(
                            color = AutoDevColors.Neutral.c300,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Streaming message item
 */
@Composable
private fun StreamingMessageItem(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 350.dp)
                .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Text(
                text = content + "▌",
                style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp)
            )
        }
    }
}

