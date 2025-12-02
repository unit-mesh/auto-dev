package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

/**
 * Context menu popup for adding files/folders to workspace.
 * Uses Jewel's PopupMenu for native IntelliJ look and feel.
 *
 * Layout:
 * - Files (submenu with matching files)
 * - Folders (submenu with matching folders)
 * - Recently Opened Files (submenu)
 * - Clear Context
 * - Search field at bottom
 */
@Composable
fun IdeaFileSearchPopup(
    project: Project,
    onDismiss: () -> Unit,
    onFilesSelected: (List<VirtualFile>) -> Unit
) {
    val searchQueryState = rememberTextFieldState("")
    val searchQuery by remember { derivedStateOf { searchQueryState.text.toString() } }

    // Grouped search results
    var files by remember { mutableStateOf<List<IdeaFilePresentation>>(emptyList()) }
    var folders by remember { mutableStateOf<List<IdeaFilePresentation>>(emptyList()) }
    var recentFiles by remember { mutableStateOf<List<IdeaFilePresentation>>(emptyList()) }

    // Submenu expansion states
    var filesExpanded by remember { mutableStateOf(false) }
    var foldersExpanded by remember { mutableStateOf(false) }
    var recentExpanded by remember { mutableStateOf(false) }

    // Load data based on search query - run on background thread to avoid EDT blocking
    LaunchedEffect(searchQuery) {
        val results = withContext(Dispatchers.IO) {
            if (searchQuery.length >= 2) {
                searchAllItems(project, searchQuery)
            } else {
                SearchResults(emptyList(), emptyList(), loadRecentFiles(project))
            }
        }
        files = results.files
        folders = results.folders
        recentFiles = results.recentFiles
    }

    // Initial load - run on background thread
    LaunchedEffect(Unit) {
        recentFiles = withContext(Dispatchers.IO) {
            loadRecentFiles(project)
        }
    }

    PopupMenu(
        onDismissRequest = {
            onDismiss()
            true
        },
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.widthIn(min = 280.dp, max = 450.dp)
    ) {
        // Files submenu
        if (files.isNotEmpty() || searchQuery.length >= 2) {
            submenu(
                submenu = {
                    if (files.isEmpty()) {
                        passiveItem {
                            Text(
                                "No files found",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 13.sp,
                                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                                )
                            )
                        }
                    } else {
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
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = IdeaComposeIcons.InsertDriveFile,
                            contentDescription = null,
                            tint = JewelTheme.globalColors.text.normal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Files", style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp))
                    }
                }
            }
        }

        // Folders submenu
        if (folders.isNotEmpty() || searchQuery.length >= 2) {
            submenu(
                submenu = {
                    if (folders.isEmpty()) {
                        passiveItem {
                            Text(
                                "No folders found",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 13.sp,
                                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                                )
                            )
                        }
                    } else {
                        folders.take(10).forEach { folder ->
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
                }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = IdeaComposeIcons.Folder,
                        contentDescription = null,
                        tint = JewelTheme.globalColors.text.normal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Folders", style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp))
                }
            }
        }

        // Recently Opened Files submenu
        submenu(
            submenu = {
                if (recentFiles.isEmpty()) {
                    passiveItem {
                        Text(
                            "No recent files",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 13.sp,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                            )
                        )
                    }
                } else {
                    recentFiles.take(15).forEach { file ->
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
                }
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.History,
                    contentDescription = null,
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(16.dp)
                )
                Text("Recently Opened Files", style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp))
            }
        }

        separator()

        // Clear Context action
        selectableItem(
            selected = false,
            onClick = {
                onFilesSelected(emptyList())
                onDismiss()
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.Close,
                    contentDescription = null,
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(16.dp)
                )
                Text("Clear Context", style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp))
            }
        }

        separator()

        // Search field at bottom
        passiveItem {
            TextField(
                state = searchQueryState,
                placeholder = { Text("Focus context") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun FileMenuItem(file: IdeaFilePresentation) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IdeaComposeIcons.InsertDriveFile,
            contentDescription = null,
            tint = JewelTheme.globalColors.text.normal,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp),
                maxLines = 1
            )
            Text(
                text = file.presentablePath,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                ),
                maxLines = 1
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
        Text(
            text = folder.presentablePath,
            style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp),
            maxLines = 1
        )
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
        val fileList = EditorHistoryManager.getInstance(project).fileList
        fileList.take(30)
            .filter { it.isValid && !it.isDirectory && canBeAdded(project, it) }
            .forEach { file ->
                recentFiles.add(IdeaFilePresentation.from(project, file, isRecent = true))
            }
    } catch (e: Exception) {
        // Ignore errors loading recent files
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
            // Search files by name
            FilenameIndex.processFilesByName(query, false, scope) { file ->
                if (file.isDirectory) {
                    if (folders.size < 20) {
                        folders.add(IdeaFilePresentation.from(project, file))
                    }
                } else if (canBeAdded(project, file) && files.size < 50) {
                    files.add(IdeaFilePresentation.from(project, file))
                }
                files.size < 50 || folders.size < 20
            }

            // Also search for folders containing the query
            val fileIndex = ProjectFileIndex.getInstance(project)
            fileIndex.iterateContent { file ->
                if (file.isDirectory && file.name.lowercase().contains(lowerQuery)) {
                    if (folders.size < 20 && !folders.any { it.path == file.path }) {
                        folders.add(IdeaFilePresentation.from(project, file))
                    }
                }
                folders.size < 20
            }
        }
    } catch (e: Exception) {
        // Ignore search errors
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

