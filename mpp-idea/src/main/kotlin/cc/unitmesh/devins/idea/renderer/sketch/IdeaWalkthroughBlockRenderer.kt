package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.unitmesh.devins.idea.renderer.IdeaMarkdownRenderer
import com.intellij.openapi.Disposable

/**
 * Walkthrough block renderer for IntelliJ IDEA.
 *
 * Renders <!-- walkthrough_start --> ... <!-- walkthrough_end --> blocks
 * containing structured code review summaries with:
 * - Walkthrough section (2-3 paragraphs)
 * - Changes table (markdown table)
 * - Optional sequence diagrams
 */
@Composable
fun IdeaWalkthroughBlockRenderer(
    walkthroughContent: String,
    isComplete: Boolean = true,
    parentDisposable: Disposable,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        IdeaMarkdownRenderer(
            content = walkthroughContent,
            isComplete = isComplete,
            parentDisposable = parentDisposable,
            modifier = modifier
        )
    }
}

