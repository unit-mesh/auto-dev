package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.idea.renderer.MermaidDiagramView
import cc.unitmesh.devins.idea.renderer.markdown.JewelMarkdownRenderer
import cc.unitmesh.devins.parser.CodeFence
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.ui.component.CircularProgressIndicator

/**
 * IntelliJ IDEA-specific Sketch Renderer.
 * Uses Jewel components for native IntelliJ look and feel.
 *
 * Handles various content block types:
 * - Markdown/Text -> JewelMarkdown
 * - Code -> IdeaCodeBlockRenderer
 * - Diff -> IdeaDiffRenderer (with action buttons when project is provided)
 * - Thinking -> IdeaThinkingBlockRenderer
 * - Walkthrough -> IdeaWalkthroughBlockRenderer
 * - Mermaid -> MermaidDiagramView
 * - DevIn -> IdeaDevInBlockRenderer
 * 
 * Related GitHub Issue: https://github.com/phodal/auto-dev/issues/25
 */
object IdeaSketchRenderer {

    /**
     * Render LLM response content with full sketch support.
     * 
     * @param content The content to render
     * @param isComplete Whether the content is complete (not streaming)
     * @param parentDisposable Parent disposable for resource cleanup
     * @param project Optional project for action buttons (Accept/Reject/View Diff)
     * @param modifier Compose modifier
     */
    @Composable
    fun RenderResponse(
        content: String,
        isComplete: Boolean = false,
        parentDisposable: Disposable,
        project: Project? = null,
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
                            JewelMarkdownRenderer(
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
                                project = project,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    "plan" -> {
                        if (fence.text.isNotBlank()) {
                            IdeaPlanRenderer(
                                planContent = fence.text,
                                project = project,
                                isComplete = blockIsComplete,
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
                                project = project,
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
