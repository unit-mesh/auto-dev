package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer

/**
 * AI 输出区域
 */
@Composable
fun ChatOutputSection(
    llmOutput: String,
    isLLMProcessing: Boolean,
    projectPath: String?,
    fileSystem: ProjectFileSystem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 显示 LLM 输出
        if (llmOutput.isNotEmpty() || isLLMProcessing) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤖 AI Response:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (isLLMProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (llmOutput.isEmpty()) {
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    } else {
                        SketchRenderer.RenderResponse(
                            content = llmOutput,
                            isComplete = !isLLMProcessing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 显示项目路径提示
        ProjectInfoFooter(
            projectPath = projectPath,
            fileSystem = fileSystem
        )
    }
}

/**
 * 项目信息底部
 */
@Composable
private fun ProjectInfoFooter(
    projectPath: String?,
    fileSystem: ProjectFileSystem
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (projectPath != null) "📁 Project: $projectPath" else "⚠️ No project selected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        
        if (projectPath != null) {
            val commandCount = remember(fileSystem) {
                try {
                    cc.unitmesh.devins.command.SpecKitCommand.loadAll(fileSystem).size
                } catch (e: Exception) {
                    0
                }
            }
            
            Text(
                text = "✨ $commandCount SpecKit commands",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

