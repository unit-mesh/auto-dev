package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.parser.CodeFence

/**
 * Sketch 渲染器 - 主渲染器
 *
 * 负责解析和分发不同类型的内容块到对应的子渲染器：
 * - Markdown/Text -> TextBlockRenderer
 * - Code -> CodeBlockRenderer
 * - Diff -> DiffSketchRenderer
 * - Thinking -> ThinkingBlockRenderer
 * - Walkthrough -> WalkthroughBlockRenderer
 */
object SketchRenderer : BaseContentRenderer() {
    /**
     * 渲染 LLM 响应内容（向后兼容的方法）
     *
     * @param content 要渲染的内容
     * @param isComplete 是否渲染完成
     * @param onContentUpdate 内容更新回调（返回当前渲染的块数量，用于外层滚动控制）
     * @param modifier Compose Modifier
     */
    @Composable
    fun RenderResponse(
        content: String,
        isComplete: Boolean = false,
        onContentUpdate: ((blockCount: Int) -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        // 包装为统一接口
        Render(
            content = content,
            isComplete = isComplete,
            onRenderUpdate = { metadata ->
                onContentUpdate?.invoke(metadata.blockCount)
            },
            modifier = modifier
        )
    }

    /**
     * 实现 ContentRenderer 接口的渲染方法
     */
    @Composable
    override fun Render(
        content: String,
        isComplete: Boolean,
        onRenderUpdate: ((RenderMetadata) -> Unit)?,
        modifier: Modifier
    ) {
        Column(modifier = modifier) {
            // Parse and render main content
            val codeFences = CodeFence.parseAll(content)

            // 通知外层当前渲染的块数量和最后一个块类型
            if (codeFences.isNotEmpty()) {
                notifyRenderUpdate(
                    onRenderUpdate = onRenderUpdate,
                    blockCount = codeFences.size,
                    lastBlockType = codeFences.lastOrNull()?.languageId
                )
            }

            codeFences.forEachIndexed { index, fence ->
                val isLastBlock = index == codeFences.lastIndex
                val blockIsComplete = fence.isComplete && (isComplete || !isLastBlock)

                when (fence.languageId.lowercase()) {
                    "markdown", "md", "" -> {
                        if (fence.text.isNotBlank()) {
                            TextBlockRenderer(fence.text)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "diff", "patch" -> {
                        DiffSketchRenderer.RenderDiff(
                            diffContent = fence.text,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    "thinking" -> {
                        if (fence.text.isNotBlank()) {
                            ThinkingBlockRenderer(
                                thinkingContent = fence.text,
                                isComplete = blockIsComplete,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "walkthrough" -> {
                        if (fence.text.isNotBlank()) {
                            WalkthroughBlockRenderer(
                                walkthroughContent = fence.text,
                                modifier = Modifier.fillMaxWidth(),
                                isComplete = fence.isComplete
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "mermaid", "mmd" -> {
                        if (fence.text.isNotBlank()) {
                            MermaidBlockRenderer(
                                mermaidCode = fence.text,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    else -> {
                        CodeBlockRenderer(
                            code = fence.text,
                            language = fence.languageId,
                            displayName = CodeFence.displayNameByExt(fence.extension ?: fence.languageId)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (!isComplete && content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
