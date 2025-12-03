package cc.unitmesh.devins.ui.compose.editor.context

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * TopToolbar Component
 *
 * Displays file context management toolbar with add file button and selected files.
 * Similar to TopToolbar.tsx from mpp-vscode and IdeaTopToolbar.kt from mpp-idea.
 */
@Composable
fun TopToolbar(
    selectedFiles: List<SelectedFileItem>,
    onAddFile: (SelectedFileItem) -> Unit,
    onRemoveFile: (SelectedFileItem) -> Unit,
    onClearFiles: () -> Unit,
    autoAddCurrentFile: Boolean = true,
    onToggleAutoAdd: () -> Unit = {},
    searchProvider: FileSearchProvider = DefaultFileSearchProvider,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showFileSearch by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        // Main toolbar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Add file button with dropdown
            Box {
                if (selectedFiles.isEmpty()) {
                    // Full button when no files selected
                    TextButton(
                        onClick = { showFileSearch = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add context",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    // Icon-only button when files are selected
                    IconButton(
                        onClick = { showFileSearch = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Add,
                            contentDescription = "Add file to context",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // File search dropdown menu
                FileSearchPopup(
                    expanded = showFileSearch,
                    onDismiss = { showFileSearch = false },
                    onSelectFile = { file ->
                        onAddFile(file)
                        showFileSearch = false
                    },
                    selectedFiles = selectedFiles,
                    searchProvider = searchProvider
                )
            }

            // File chips (horizontal scroll when collapsed)
            if (selectedFiles.isNotEmpty() && !isExpanded) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    selectedFiles.take(5).forEach { file ->
                        FileChip(
                            file = file,
                            onRemove = { onRemoveFile(file) }
                        )
                    }
                    if (selectedFiles.size > 5) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "+${selectedFiles.size - 5} more",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else if (selectedFiles.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Context indicator (auto-add toggle)
            ContextIndicator(
                isActive = autoAddCurrentFile,
                onClick = onToggleAutoAdd
            )

            // Expand/Collapse button (only when multiple files)
            if (selectedFiles.size > 1) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Clear all button
            if (selectedFiles.isNotEmpty()) {
                IconButton(
                    onClick = onClearFiles,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Clear,
                        contentDescription = "Clear all files",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Expanded file list
        AnimatedVisibility(
            visible = isExpanded && selectedFiles.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedFiles.forEach { file ->
                    FileChipExpanded(
                        file = file,
                        onRemove = { onRemoveFile(file) }
                    )
                }
            }
        }
    }
}

/**
 * Context indicator showing auto-add current file status.
 */
@Composable
private fun ContextIndicator(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(24.dp)
    ) {
        Box {
            Icon(
                imageVector = AutoDevComposeIcons.InsertDriveFile,
                contentDescription = if (isActive) "Auto-add current file: ON" else "Auto-add current file: OFF",
                modifier = Modifier.size(14.dp),
                tint = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            // Active indicator dot
            if (isActive) {
                Surface(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.BottomEnd),
                    shape = RoundedCornerShape(3.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {}
            }
        }
    }
}

