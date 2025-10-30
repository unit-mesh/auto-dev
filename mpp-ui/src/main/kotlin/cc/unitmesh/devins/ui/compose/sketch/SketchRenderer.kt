package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.parser.CodeFence

/**
 * Sketch 渲染器 - 根据内容类型渲染不同样式的输出
 * 参考 AutoDev IDEA 版本的 LangSketch 设计
 * 使用 CodeFence 作为核心解析器
 */
object SketchRenderer {
    
    /**
     * 渲染 LLM 响应内容
     * 使用 CodeFence.parseAll() 解析 Markdown 代码块
     */
    @Composable
    fun RenderResponse(
        content: String,
        isComplete: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        val scrollState = rememberScrollState()
        
        // 自动滚动到底部
        LaunchedEffect(content) {
            if (content.isNotEmpty()) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
        
        Column(
            modifier = modifier
        ) {
            // 使用 CodeFence 解析内容
            val codeFences = CodeFence.parseAll(content)
            
            codeFences.forEach { fence ->
                when (fence.languageId.lowercase()) {
                    "markdown", "md", "" -> {
                        // 文本块
                        if (fence.text.isNotBlank()) {
                            TextBlockRenderer(fence.text)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    else -> {
                        // 代码块
                        CodeBlockRenderer(
                            code = fence.text,
                            language = fence.languageId,
                            displayName = CodeFence.displayNameByExt(fence.extension ?: fence.languageId)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            // 如果未完成，显示加载指示器
            if (!isComplete && content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    /**
     * 渲染代码块
     */
    @Composable
    private fun CodeBlockRenderer(code: String, language: String, displayName: String = language) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 语言标签（显示友好的名称）
                if (displayName.isNotEmpty() && displayName != "markdown") {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // 代码内容
                SelectionContainer {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    /**
     * 渲染普通文本块（Markdown 文本）
     */
    @Composable
    private fun TextBlockRenderer(text: String) {
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

