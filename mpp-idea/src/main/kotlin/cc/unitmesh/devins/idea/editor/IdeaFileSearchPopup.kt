package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
 * File search popup for adding files to workspace.
 * Similar to WorkspaceFileSearchPopup from core module but using Compose/Jewel UI.
 */
@Composable
fun IdeaFileSearchPopup(
    project: Project,
    onDismiss: () -> Unit,
    onFilesSelected: (List<VirtualFile>) -> Unit
) {
    val searchQueryState = rememberTextFieldState("")
    var searchResults by remember { mutableStateOf<List<IdeaFilePresentation>>(emptyList()) }
    var selectedFiles by remember { mutableStateOf<Set<VirtualFile>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    // Derive search query from state
    val searchQuery by remember { derivedStateOf { searchQueryState.text.toString() } }

    // Load recent files on first composition
    LaunchedEffect(Unit) {
        searchResults = loadRecentFiles(project)
        isLoading = false
    }

    // Search when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = loadRecentFiles(project)
        } else if (searchQuery.length >= 2) {
            isLoading = true
            searchResults = searchFiles(project, searchQuery)
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .height(400.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JewelTheme.globalColors.panelBackground)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Files to Context", style = JewelTheme.defaultTextStyle.copy(fontSize = 16.sp))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = IdeaComposeIcons.Close,
                        contentDescription = "Close",
                        tint = JewelTheme.globalColors.text.normal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search field using Jewel's TextField with TextFieldState
            TextField(
                state = searchQueryState,
                placeholder = { Text("Search files...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // File list
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Loading...")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(searchResults) { file ->
                        FileListItem(
                            file = file,
                            isSelected = file.virtualFile in selectedFiles,
                            onClick = {
                                selectedFiles = if (file.virtualFile in selectedFiles) {
                                    selectedFiles - file.virtualFile
                                } else {
                                    selectedFiles + file.virtualFile
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer with action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                DefaultButton(
                    onClick = { onFilesSelected(selectedFiles.toList()) },
                    enabled = selectedFiles.isNotEmpty()
                ) {
                    Text("Add ${if (selectedFiles.isNotEmpty()) "(${selectedFiles.size})" else ""}")
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    file: IdeaFilePresentation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )

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

        if (file.isRecentFile) {
            Text(
                text = "Recent",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 10.sp,
                    color = JewelTheme.globalColors.text.info
                )
            )
        }
    }
}

/**
 * File presentation data class for Compose UI.
 */
data class IdeaFilePresentation(
    val virtualFile: VirtualFile,
    val name: String,
    val path: String,
    val presentablePath: String,
    val isRecentFile: Boolean = false
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
                isRecentFile = isRecent
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

private fun searchFiles(project: Project, query: String): List<IdeaFilePresentation> {
    val results = mutableListOf<IdeaFilePresentation>()
    val scope = GlobalSearchScope.projectScope(project)

    try {
        ApplicationManager.getApplication().runReadAction {
            FilenameIndex.processFilesByName(query, false, scope) { file ->
                if (canBeAdded(project, file) && results.size < 50) {
                    results.add(IdeaFilePresentation.from(project, file))
                }
                results.size < 50
            }
        }
    } catch (e: Exception) {
        // Ignore search errors
    }

    return results.sortedBy { it.name }
}

private fun canBeAdded(project: Project, file: VirtualFile): Boolean {
    if (!file.isValid || file.isDirectory) return false

    val fileIndex = ProjectFileIndex.getInstance(project)
    if (!fileIndex.isInContent(file)) return false
    if (fileIndex.isUnderIgnored(file)) return false

    // Skip binary files
    val extension = file.extension?.lowercase() ?: ""
    val binaryExtensions = setOf("jar", "class", "exe", "dll", "so", "dylib", "png", "jpg", "jpeg", "gif", "ico", "pdf", "zip", "tar", "gz")
    if (extension in binaryExtensions) return false

    return true
}

