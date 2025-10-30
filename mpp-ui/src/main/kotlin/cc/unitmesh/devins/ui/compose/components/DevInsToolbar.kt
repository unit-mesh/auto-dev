package cc.unitmesh.devins.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevInsToolbar(
    onOpenFile: () -> Unit,
    onOpenProject: () -> Unit,
    onSave: () -> Unit,
    onCompile: () -> Unit,
    onClear: () -> Unit,
    canCompile: Boolean,
    isCompiling: Boolean,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("AutoDev Desktop") },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                // 文件操作按钮
                ToolbarButton(
                    icon = Icons.Default.FolderOpen,
                    text = "Open Project",
                    onClick = onOpenProject
                )
                
                ToolbarButton(
                    icon = Icons.Default.InsertDriveFile,
                    text = "Open File",
                    onClick = onOpenFile
                )
                
                ToolbarButton(
                    icon = Icons.Default.Save,
                    text = "Save",
                    onClick = onSave
                )
                
                Divider(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                )
                
                // 编译操作按钮
                ToolbarButton(
                    icon = Icons.Default.PlayArrow,
                    text = if (isCompiling) "Compiling..." else "Compile",
                    onClick = onCompile,
                    enabled = canCompile
                )
                
                ToolbarButton(
                    icon = Icons.Default.Clear,
                    text = "Clear",
                    onClick = onClear
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
