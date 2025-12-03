package cc.unitmesh.devins.ui.compose.editor.context

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** File search provider interface for platform-specific implementations */
interface FileSearchProvider {
    suspend fun searchFiles(query: String): List<SelectedFileItem>
    suspend fun getRecentFiles(): List<SelectedFileItem>
}

/** Default file search provider that returns empty results */
object DefaultFileSearchProvider : FileSearchProvider {
    override suspend fun searchFiles(query: String): List<SelectedFileItem> = emptyList()
    override suspend fun getRecentFiles(): List<SelectedFileItem> = emptyList()
}

/**
 * FileSearchPopup - A dropdown menu for searching and selecting files to add to context.
 * Similar to IDEA's IdeaFileSearchPopup using DropdownMenu.
 *
 * @param expanded Whether the dropdown is expanded
 * @param onDismiss Called when the dropdown should be dismissed
 * @param onSelectFile Called when a file is selected
 * @param selectedFiles Currently selected files (to filter from results)
 * @param searchProvider Provider for file search functionality
 */
@Composable
fun FileSearchPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelectFile: (SelectedFileItem) -> Unit,
    selectedFiles: List<SelectedFileItem>,
    searchProvider: FileSearchProvider = DefaultFileSearchProvider,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SelectedFileItem>>(emptyList()) }
    var recentFiles by remember { mutableStateOf<List<SelectedFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Observe workspace changes
    val currentWorkspace by cc.unitmesh.devins.workspace.WorkspaceManager.workspaceFlow.collectAsState()

    // Observe indexing state if using WorkspaceFileSearchProvider
    val indexingState = if (searchProvider is WorkspaceFileSearchProvider) {
        searchProvider.indexingState.collectAsState().value
    } else {
        IndexingState.READY
    }

    // Build index when popup opens
    LaunchedEffect(expanded, currentWorkspace) {
        if (expanded && currentWorkspace != null && searchProvider is WorkspaceFileSearchProvider) {
            searchProvider.buildIndex()
        }
    }

    // Load recent files when popup opens
    LaunchedEffect(expanded, indexingState) {
        if (expanded && indexingState == IndexingState.READY) {
            searchQuery = ""
            searchResults = emptyList()
            isLoading = false
            recentFiles = searchProvider.getRecentFiles()
            delay(100)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Debounced search function
    fun performSearch(query: String) {
        if (query.length < 2 || currentWorkspace == null || indexingState != IndexingState.READY) {
            searchResults = emptyList()
            isLoading = false
            return
        }

        isLoading = true
        scope.launch {
            delay(150) // Debounce
            try {
                searchResults = searchProvider.searchFiles(query)
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    // Filter out already selected files
    val displayItems = remember(searchQuery, searchResults, recentFiles, selectedFiles) {
        val items = if (searchQuery.length >= 2) searchResults else recentFiles
        items.filter { item -> selectedFiles.none { it.path == item.path } }
    }

    // Separate files and folders
    val files = displayItems.filter { !it.isDirectory }
    val folders = displayItems.filter { it.isDirectory }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = 300.dp, max = 400.dp),
        offset = DpOffset(0.dp, 4.dp)
    ) {
        // Search field at top
        SearchField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                performSearch(it)
            },
            focusRequester = focusRequester,
            onDismiss = onDismiss
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Content based on state
        when {
            currentWorkspace == null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No workspace opened",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            indexingState == IndexingState.INDEXING -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Indexing files...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            indexingState == IndexingState.ERROR -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Failed to index files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            displayItems.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.length >= 2) "No files found" else "Type to search...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            searchQuery.length < 2 -> {
                // Show recent files
                SectionHeader(icon = AutoDevComposeIcons.History, title = "Recent Files")
                displayItems.take(8).forEach { item ->
                    FileMenuItem(
                        item = item,
                        showHistoryIcon = true,
                        onClick = { onSelectFile(item); onDismiss() }
                    )
                }
            }
            else -> {
                // Show search results grouped
                if (files.isNotEmpty()) {
                    SectionHeader(title = "Files (${files.size})")
                    files.take(8).forEach { file ->
                        FileMenuItem(item = file, onClick = { onSelectFile(file); onDismiss() })
                    }
                    if (files.size > 8) {
                        MoreItemsHint(count = files.size - 8)
                    }
                }
                if (folders.isNotEmpty()) {
                    if (files.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    SectionHeader(title = "Folders (${folders.size})")
                    folders.take(5).forEach { folder ->
                        FileMenuItem(item = folder, onClick = { onSelectFile(folder); onDismiss() })
                    }
                }
            }
        }
    }
}

/** Search field for the dropdown menu */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = AutoDevComposeIcons.Search,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                },
            textStyle = TextStyle(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "Search files and folders...",
                            style = TextStyle(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/** Section header for dropdown menu */
@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/** File menu item for dropdown */
@Composable
private fun FileMenuItem(
    item: SelectedFileItem,
    showHistoryIcon: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    DropdownMenuItem(
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.truncatedPath.isNotEmpty()) {
                    Text(
                        text = item.truncatedPath,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = when {
                    showHistoryIcon -> AutoDevComposeIcons.History
                    item.isDirectory -> AutoDevComposeIcons.Folder
                    else -> AutoDevComposeIcons.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        modifier = Modifier
            .hoverable(interactionSource = interactionSource)
            .background(
                if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface
            )
    )
}

/** Hint showing more items available */
@Composable
private fun MoreItemsHint(count: Int) {
    Text(
        text = "... and $count more",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}
