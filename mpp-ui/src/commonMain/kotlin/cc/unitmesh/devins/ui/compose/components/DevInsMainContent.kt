package cc.unitmesh.devins.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.state.DevInsAppState
import cc.unitmesh.devins.ui.compose.state.rememberDevInsAppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevInsMainContent() {
    val appState = rememberDevInsAppState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 工具栏
        DevInsToolbar(
            onOpenFile = { appState.openFile() },
            onOpenProject = { appState.openProject() },
            onSave = { appState.saveCurrentFile() },
            onCompile = { appState.compile() },
            onClear = { appState.clearOutput() },
            canCompile = appState.canCompile,
            isCompiling = appState.isCompiling
        )
        
        // 主内容区域
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // 文件树面板
            if (appState.showFileTree) {
                DevInsFileTree(
                    projectRoot = appState.projectRootPath,
                    fileSystem = null, // TODO: Pass actual fileSystem
                    onFileSelected = { filePath -> appState.openFileInEditor(filePath) },
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                )
            }

            // 编辑器和输出面板
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 编辑器
                DevInsEditor(
                    content = appState.editorContent,
                    onContentChange = { appState.updateEditorContent(it) },
                    currentFilePath = appState.currentFilePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (appState.showOutput) 0.7f else 1f)
                )
                
                // 输出面板
                if (appState.showOutput) {
                    DevInsOutput(
                        output = appState.output,
                        isError = appState.isOutputError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.3f)
                    )
                }
            }
        }
        
        // 状态栏
        DevInsStatusBar(
            status = appState.statusMessage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
