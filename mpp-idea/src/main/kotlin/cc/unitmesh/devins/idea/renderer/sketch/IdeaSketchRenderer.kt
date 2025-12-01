package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.idea.renderer.MermaidDiagramView
import cc.unitmesh.devins.idea.renderer.markdown.SimpleJewelMarkdown
import cc.unitmesh.devins.parser.CodeFence
import com.intellij.openapi.Disposable
import org.jetbrains.jewel.ui.component.CircularProgressIndicator

/**
 * IntelliJ IDEA-specific Sketch Renderer.
 * Uses Jewel components for native IntelliJ look and feel.
 *
 * Handles various content block types:
 * - Markdown/Text -> JewelMarkdown
 * - Code -> IdeaCodeBlockRenderer
 * - Diff -> IdeaDiffRenderer
 * - Thinking -> IdeaThinkingBlockRenderer
 * - Walkthrough -> IdeaWalkthroughBlockRenderer
 * - Mermaid -> MermaidDiagramView
 * - DevIn -> IdeaDevInBlockRenderer
 */
object IdeaSketchRenderer {

    /**
     * Render LLM response content with full sketch support.
     */
    @Composable
    fun RenderResponse(
        content: String,
        isComplete: Boolean = false,
        parentDisposable: Disposable,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            val codeFences = remember(content) { CodeFence.parseAll(content) }

            codeFences.forEachIndexed { index, fence ->
                val isLastBlock = index == codeFences.lastIndex
                val blockIsComplete = fence.isComplete && (isComplete || !isLastBlock)

                when (fence.languageId.lowercase()) {
                    "markdown", "md", "" -> {
                        if (fence.text.isNotBlank()) {
                            SimpleJewelMarkdown(
                                content = fence.text,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "diff", "patch" -> {
                        if (fence.text.isNotBlank()) {
                            IdeaDiffRenderer(
                                diffContent = fence.text,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "thinking" -> {
                        if (fence.text.isNotBlank()) {
                            IdeaThinkingBlockRenderer(
                                thinkingContent = fence.text,
                                isComplete = blockIsComplete,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "walkthrough" -> {
                        if (fence.text.isNotBlank()) {
                            IdeaWalkthroughBlockRenderer(
                                walkthroughContent = fence.text,
                                isComplete = blockIsComplete,
                                parentDisposable = parentDisposable,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "mermaid", "mmd" -> {
                        if (fence.text.isNotBlank() && blockIsComplete) {
                            MermaidDiagramView(
                                mermaidCode = fence.text,
                                isDarkTheme = true, // TODO: detect theme
                                parentDisposable = parentDisposable,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "devin" -> {
                        if (fence.text.isNotBlank()) {
                            IdeaDevInBlockRenderer(
                                devinContent = fence.text,
                                isComplete = blockIsComplete,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    else -> {
                        if (fence.text.isNotBlank()) {
                            IdeaCodeBlockRenderer(
                                code = fence.text,
                                language = fence.languageId,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            if (!isComplete && content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }
        }
    }
}

