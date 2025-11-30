package cc.unitmesh.devins.idea.renderer.markdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.MarkdownSuccess
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.*
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/**
 * Renders the markdown content using Jewel (IntelliJ IDEA) styles.
 *
 * This is a wrapper around the core multiplatform-markdown-renderer library
 * that provides IntelliJ-native theme colors and typography using Jewel.
 *
 * @param content The markdown content to be rendered.
 * @param modifier The [Modifier] to apply to the component.
 * @param colors The [MarkdownColors] to use for styling. Defaults to Jewel colors.
 * @param typography The [MarkdownTypography] to use for text styles. Defaults to Jewel typography.
 */
@Composable
fun JewelMarkdown(
    content: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    colors: MarkdownColors = jewelMarkdownColor(),
    typography: MarkdownTypography = jewelMarkdownTypography(),
    padding: MarkdownPadding = markdownPadding(),
    dimens: MarkdownDimens = markdownDimens(),
    flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    parser: MarkdownParser = MarkdownParser(flavour),
    imageTransformer: ImageTransformer = NoOpImageTransformerImpl(),
    annotator: MarkdownAnnotator = markdownAnnotator(),
    extendedSpans: MarkdownExtendedSpans = markdownExtendedSpans(),
    inlineContent: MarkdownInlineContent = markdownInlineContent(),
    components: MarkdownComponents = markdownComponents(),
    animations: MarkdownAnimations = markdownAnimations(),
    referenceLinkHandler: ReferenceLinkHandler = ReferenceLinkHandlerImpl(),
    loading: @Composable (modifier: Modifier) -> Unit = { Box(modifier) },
    success: @Composable (state: State.Success, components: MarkdownComponents, modifier: Modifier) -> Unit = { state, comps, mod ->
        MarkdownSuccess(state = state, components = comps, modifier = mod)
    },
    error: @Composable (modifier: Modifier) -> Unit = { Box(modifier) },
) = Markdown(
    content = content,
    colors = colors,
    typography = typography,
    modifier = modifier,
    padding = padding,
    dimens = dimens,
    flavour = flavour,
    parser = parser,
    imageTransformer = imageTransformer,
    annotator = annotator,
    extendedSpans = extendedSpans,
    inlineContent = inlineContent,
    components = components,
    animations = animations,
    referenceLinkHandler = referenceLinkHandler,
    loading = loading,
    success = success,
    error = error,
)

