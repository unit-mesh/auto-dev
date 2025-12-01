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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.agent.render.TimelineItem
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.idea.components.IdeaResizableSplitPane
import cc.unitmesh.devins.idea.components.IdeaVerticalResizableSplitPane
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

/**
 * Main content view for Knowledge Agent in IntelliJ IDEA.
 * Provides document browsing, search, and AI-powered document querying.
 *
 * Layout (using resizable split panes):
 * - Left: Document list with search (resizable)
 * - Center: Document content viewer + Structured info pane (vertical split)
 * - Right: AI Chat interface (resizable)
 */
@Composable
fun IdeaKnowledgeContent(
    viewModel: IdeaKnowledgeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val timeline by viewModel.renderer.timeline.collectAsState()
    val streamingOutput by viewModel.renderer.currentStreamingOutput.collectAsState()

    // Left panel + (Center + Right) split
    IdeaResizableSplitPane(
        modifier = modifier.fillMaxSize(),
        initialSplitRatio = 0.18f,
        minRatio = 0.12f,
        maxRatio = 0.35f,
        first = {
            // Left panel: Document list with search
            DocumentListPanel(
                documents = state.filteredDocuments,
                selectedDocument = state.selectedDocument,
                searchQuery = state.searchQuery,
                isLoading = state.isLoading,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onDocumentSelect = { viewModel.selectDocument(it) },
                onRefresh = { viewModel.refreshDocuments() },
                modifier = Modifier.fillMaxSize()
            )
        },
        second = {
            // Center + Right split
            IdeaResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.65f,
                minRatio = 0.4f,
                maxRatio = 0.85f,
                first = {
                    // Center panel: Document content viewer + Structured info (vertical split)
                    IdeaVerticalResizableSplitPane(
                        modifier = Modifier.fillMaxSize(),
                        initialSplitRatio = 0.7f,
                        minRatio = 0.3f,
                        maxRatio = 0.9f,
                        top = {
                            DocumentContentPanel(
                                document = state.selectedDocument,
                                content = state.documentContent ?: state.parsedContent,
                                isLoading = state.isLoading,
                                targetLineNumber = state.targetLineNumber,
                                highlightedText = state.highlightedText,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        bottom = {
                            // Structured info pane (TOC + Entities)
                            IdeaStructuredInfoPane(
                                toc = state.selectedDocument?.toc ?: emptyList(),
                                entities = state.selectedDocument?.entities ?: emptyList(),
                                onTocSelected = { viewModel.navigateToTocItem(it) },
                                onEntitySelected = { viewModel.navigateToEntity(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                },
                second = {
                    // Right panel: AI Chat interface
                    AIChatPanel(
                        timeline = timeline,
                        streamingOutput = streamingOutput,
                        isGenerating = state.isGenerating,
                        onSendMessage = { viewModel.sendMessage(it) },
                        onStopGeneration = { viewModel.stopGeneration() },
                        onClearHistory = { viewModel.clearChatHistory() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }
    )
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
    val searchTextFieldState = rememberTextFieldState(searchQuery)

    // Sync text field state changes to callback
    LaunchedEffect(Unit) {
        snapshotFlow { searchTextFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { onSearchQueryChange(it) }
    }

    // Sync external searchQuery changes to text field state
    LaunchedEffect(searchQuery) {
        if (searchTextFieldState.text.toString() != searchQuery) {
            searchTextFieldState.setTextAndPlaceCursorAtEnd(searchQuery)
        }
    }

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
            state = searchTextFieldState,
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
        AutoDevColors.Blue.c400.copy(alpha = 0.15f)
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
 * Document content viewer panel (TOC moved to IdeaStructuredInfoPane)
 */
@Composable
private fun DocumentContentPanel(
    document: IdeaDocumentFile?,
    content: String?,
    isLoading: Boolean,
    targetLineNumber: Int?,
    highlightedText: String?,
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
                                    if (isHighlighted) AutoDevColors.Amber.c400.copy(alpha = 0.2f)
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
    timeline: List<TimelineItem>,
    streamingOutput: String,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputTextFieldState = rememberTextFieldState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Sync text field state to inputText
    LaunchedEffect(Unit) {
        snapshotFlow { inputTextFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { inputText = it }
    }

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
                state = inputTextFieldState,
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
                            inputTextFieldState.edit { replace(0, length, "") }
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
private fun ChatMessageItem(item: TimelineItem) {
    when (item) {
        is TimelineItem.MessageItem -> {
            val isUser = item.role == MessageRole.USER
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

        is TimelineItem.ToolCallItem -> {
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
                        item.output?.let { output ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = output.take(200) + if (output.length > 200) "..." else "",
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }

        is TimelineItem.ErrorItem -> {
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

        is TimelineItem.TaskCompleteItem -> {
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

        is TimelineItem.TerminalOutputItem -> {
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

        is TimelineItem.LiveTerminalItem -> {
            // Live terminal not supported in knowledge content, show placeholder
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
                        text = "[Live terminal session: ${item.sessionId}]",
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

