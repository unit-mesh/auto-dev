package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

/**
 * Context menu popup for adding files/folders to workspace.
 * Uses Jewel's PopupMenu for native IntelliJ look and feel.
 *
 * This component includes both the trigger button and the popup menu.
 * The popup is positioned relative to the trigger button.
 *
 * Layout:
 * - Recently Opened Files (direct items)
 * - Files (submenu with matching files, only when searching)
 * - Folders (submenu with matching folders, only when searching)
 * - Search field at bottom
 */
@Composable
fun IdeaFileSearchPopup(
    project: Project,
    showPopup: Boolean,
    onShowPopupChange: (Boolean) -> Unit,
    onFilesSelected: (List<VirtualFile>) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(modifier = modifier) {
        // Trigger button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .hoverable(interactionSource = interactionSource)
                .background(
                    if (isHovered || showPopup)
                        JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f)
                    else
                        androidx.compose.ui.graphics.Color.Transparent
                )
                .clickable { onShowPopupChange(true) }
                .padding(4.dp)
        ) {
            Tooltip(tooltip = { Text("Add File to Context") }) {
                Icon(
                    imageVector = IdeaComposeIcons.Add,
                    contentDescription = "Add File",
                    modifier = Modifier.size(16.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            }
        }

        // Popup menu
        if (showPopup) {
            FileSearchPopupContent(
                project = project,
                onDismiss = { onShowPopupChange(false) },
                onFilesSelected = onFilesSelected
            )
        }
    }
}

@Composable
private fun FileSearchPopupContent(
    project: Project,
    onDismiss: () -> Unit,
    onFilesSelected: (List<VirtualFile>) -> Unit
) {
    val searchQueryState = rememberTextFieldState("")
    val searchQuery by remember { derivedStateOf { searchQueryState.text.toString() } }

    // Load recent files immediately (not in LaunchedEffect)
    val recentFiles = remember(project) { loadRecentFiles(project) }

    // Search results - only computed when query is long enough
    val searchResults = remember(searchQuery, project) {
        if (searchQuery.length >= 2) {
            searchAllItems(project, searchQuery)
        } else {
            null
        }
    }

    val files = searchResults?.files ?: emptyList()
    val folders = searchResults?.folders ?: emptyList()
    val filteredRecentFiles = if (searchQuery.length >= 2) {
        searchResults?.recentFiles ?: emptyList()
    } else {
        recentFiles
    }

    PopupMenu(
        onDismissRequest = {
            onDismiss()
            true
        },
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.widthIn(min = 300.dp, max = 480.dp)
    ) {
        // Search field at top with improved styling
        passiveItem {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.Search,
                    contentDescription = null,
                    tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                TextField(
                    state = searchQueryState,
                    placeholder = {
                        Text(
                            "Search files and folders...",
                            style = JewelTheme.defaultTextStyle.copy(
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        separator()

        // Show search results if searching
        if (searchQuery.length >= 2) {
            // Files from search
            if (files.isNotEmpty()) {
                passiveItem {
                    Text(
                        "Files (${files.size})",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                        )
                    )
                }
                files.take(10).forEach { file ->
                    selectableItem(
                        selected = false,
                        onClick = {
                            onFilesSelected(listOf(file.virtualFile))
                            onDismiss()
                        }
                    ) {
                        FileMenuItem(file)
                    }
                }
                if (files.size > 10) {
                    passiveItem {
                        Text(
                            "... and ${files.size - 10} more",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 11.sp,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Folders from search
            if (folders.isNotEmpty()) {
                if (files.isNotEmpty()) separator()
                passiveItem {
                    Text(
                        "Folders (${folders.size})",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                        )
                    )
                }
                folders.take(5).forEach { folder ->
                    selectableItem(
                        selected = false,
                        onClick = {
                            onFilesSelected(listOf(folder.virtualFile))
                            onDismiss()
                        }
                    ) {
                        FolderMenuItem(folder)
                    }
                }
            }

            // No results message
            if (files.isEmpty() && folders.isEmpty()) {
                passiveItem {
                    Text(
                        "No files or folders found",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 13.sp,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        } else {
            // Show recent files when not searching
            if (filteredRecentFiles.isNotEmpty()) {
                passiveItem {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = IdeaComposeIcons.History,
                            contentDescription = null,
                            tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Recent Files",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                filteredRecentFiles.take(15).forEach { file ->
                    selectableItem(
                        selected = false,
                        onClick = {
                            onFilesSelected(listOf(file.virtualFile))
                            onDismiss()
                        }
                    ) {
                        FileMenuItem(file)
                    }
                }
            } else {
                passiveItem {
                    Text(
                        "No recent files. Type to search...",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 13.sp,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * File menu item with improved layout:
 * - Icon on the left
 * - Bold file name
 * - Truncated path in gray (e.g., "...cc/unitmesh/devins/idea/editor")
 * - History icon for recent files
 */
@Composable
private fun FileMenuItem(file: IdeaFilePresentation) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File icon or history icon for recent files
        Icon(
            imageVector = if (file.isRecentFile) IdeaComposeIcons.History else IdeaComposeIcons.InsertDriveFile,
            contentDescription = null,
            tint = JewelTheme.globalColors.text.normal,
            modifier = Modifier.size(16.dp)
        )

        // File name (bold) and truncated path (gray) in a row
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bold file name
            Text(
                text = file.name,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Truncated path in gray
            Text(
                text = file.truncatedPath,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Composable
private fun FolderMenuItem(folder: IdeaFilePresentation) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IdeaComposeIcons.Folder,
            contentDescription = null,
            tint = JewelTheme.globalColors.text.normal,
            modifier = Modifier.size(16.dp)
        )

        // Folder name (bold) and truncated path
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = folder.name,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = folder.truncatedPath,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

/**
 * Search results grouped by type.
 */
data class SearchResults(
    val files: List<IdeaFilePresentation>,
    val folders: List<IdeaFilePresentation>,
    val recentFiles: List<IdeaFilePresentation>
)

/**
 * File presentation data class for Compose UI.
 */
data class IdeaFilePresentation(
    val virtualFile: VirtualFile,
    val name: String,
    val path: String,
    val presentablePath: String,
    val isRecentFile: Boolean = false,
    val isDirectory: Boolean = false
) {
    /**
     * Truncated path for display, e.g., "...cc/unitmesh/devins/idea/editor"
     * Shows the parent directory path without the file name, truncated if too long.
     */
    val truncatedPath: String
        get() {
            val parentPath = presentablePath.substringBeforeLast("/", "")
            if (parentPath.isEmpty()) return ""

            // If path is short enough, show it as-is
            if (parentPath.length <= 40) return parentPath

            // Truncate from the beginning with "..."
            val parts = parentPath.split("/")
            if (parts.size <= 2) return "...$parentPath"

            // Keep the last 3-4 parts of the path
            val keepParts = parts.takeLast(4)
            return "...${keepParts.joinToString("/")}"
        }

    companion object {
        fun from(project: Project, file: VirtualFile, isRecent: Boolean = false): IdeaFilePresentation {
            val basePath = project.basePath ?: ""
            val relativePath = if (file.path.startsWith(basePath)) {
                file.path.removePrefix(basePath).removePrefix("/")
            } else {
                file.path
            }

            return IdeaFilePresentation(
                virtualFile = file,
                name = file.name,
                path = file.path,
                presentablePath = relativePath,
                isRecentFile = isRecent,
                isDirectory = file.isDirectory
            )
        }
    }
}

private fun loadRecentFiles(project: Project): List<IdeaFilePresentation> {
    val recentFiles = mutableListOf<IdeaFilePresentation>()

    try {
        ApplicationManager.getApplication().runReadAction {
            val fileList = EditorHistoryManager.getInstance(project).fileList
            fileList.take(30)
                .filter { it.isValid && !it.isDirectory && canBeAdded(project, it) }
                .forEach { file ->
                    recentFiles.add(IdeaFilePresentation.from(project, file, isRecent = true))
                }
        }
    } catch (e: Exception) {
        com.intellij.openapi.diagnostic.Logger.getInstance("IdeaFileSearchPopup")
            .warn("Error loading recent files: ${e.message}", e)
    }

    return recentFiles
}

private fun searchAllItems(project: Project, query: String): SearchResults {
    val files = mutableListOf<IdeaFilePresentation>()
    val folders = mutableListOf<IdeaFilePresentation>()
    val scope = GlobalSearchScope.projectScope(project)
    val lowerQuery = query.lowercase()

    try {
        ApplicationManager.getApplication().runReadAction {
            val fileIndex = ProjectFileIndex.getInstance(project)

            // Search files by exact name match using FilenameIndex
            FilenameIndex.processFilesByName(query, false, scope) { file ->
                if (file.isDirectory) {
                    if (folders.size < 20) {
                        folders.add(IdeaFilePresentation.from(project, file))
                    }
                } else if (canBeAdded(project, file) && files.size < 50) {
                    files.add(IdeaFilePresentation.from(project, file))
                }
                files.size < 50 && folders.size < 20
            }

            // Also do fuzzy search by iterating project content
            val existingFilePaths = files.map { it.path }.toSet()
            val existingFolderPaths = folders.map { it.path }.toSet()

            fileIndex.iterateContent { file ->
                val nameLower = file.name.lowercase()
                if (nameLower.contains(lowerQuery)) {
                    if (file.isDirectory) {
                        if (folders.size < 20 && file.path !in existingFolderPaths) {
                            folders.add(IdeaFilePresentation.from(project, file))
                        }
                    } else if (canBeAdded(project, file) && files.size < 50 && file.path !in existingFilePaths) {
                        files.add(IdeaFilePresentation.from(project, file))
                    }
                }
                files.size < 50 && folders.size < 20
            }
        }
    } catch (e: Exception) {
        // Log error for debugging
        com.intellij.openapi.diagnostic.Logger.getInstance("IdeaFileSearchPopup")
            .warn("Error searching files: ${e.message}", e)
    }

    // Filter recent files by query
    val recentFiles = loadRecentFiles(project).filter {
        it.name.lowercase().contains(lowerQuery) || it.presentablePath.lowercase().contains(lowerQuery)
    }

    return SearchResults(
        files = files.sortedBy { it.name },
        folders = folders.sortedBy { it.presentablePath },
        recentFiles = recentFiles
    )
}

private fun canBeAdded(project: Project, file: VirtualFile): Boolean {
    if (!file.isValid) return false
    if (file.isDirectory) return true // Allow directories

    val fileIndex = ProjectFileIndex.getInstance(project)
    if (!fileIndex.isInContent(file)) return false
    if (fileIndex.isUnderIgnored(file)) return false

    // Skip binary files
    val extension = file.extension?.lowercase() ?: ""
    val binaryExtensions = setOf("jar", "class", "exe", "dll", "so", "dylib", "png", "jpg", "jpeg", "gif", "ico", "pdf", "zip", "tar", "gz")
    if (extension in binaryExtensions) return false

    return true
}

