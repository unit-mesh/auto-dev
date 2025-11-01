package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.parser.CodeFence

object SketchRenderer {
    @Composable
    fun RenderResponse(
        content: String,
        isComplete: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        val scrollState = rememberScrollState()

        LaunchedEffect(content) {
            if (content.isNotEmpty()) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        Column(modifier = modifier) {
            val codeFences = CodeFence.parseAll(content)

            codeFences.forEach { fence ->
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

