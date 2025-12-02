package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.sketch.actions.IdeaCodeActions
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Code block renderer for IntelliJ IDEA with Jewel styling.
 */
@Composable
fun IdeaCodeBlockRenderer(
    code: String,
    language: String,
    project: Project? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
    ) {
        // Toolbar with language label and actions
        CodeBlockToolbar(
            code = code,
            language = language,
            project = project
        )

        // Code content
        Text(
            text = code,
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
    }
}

@Composable
private fun CodeBlockToolbar(
    code: String,
    language: String,
    project: Project?
) {
    var copied by remember { mutableStateOf(false) }
    var inserted by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Language label
        if (language.isNotBlank()) {
            Text(
                text = language,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AutoDevColors.Blue.c400
                )
            )
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Copy button
            CodeActionButton(
                tooltip = if (copied) "Copied!" else "Copy to Clipboard",
                iconKey = if (copied) AllIconsKeys.Actions.Checked else AllIconsKeys.Actions.Copy,
                onClick = {
                    if (IdeaCodeActions.copyToClipboard(code)) {
                        copied = true
                    }
                }
            )

            // Insert at cursor button (only if project is available)
            if (project != null) {
                val canInsert = remember(project) { IdeaCodeActions.canInsertAtCursor(project) }
                CodeActionButton(
                    tooltip = if (inserted) "Inserted!" else "Insert at Cursor",
                    iconKey = if (inserted) AllIconsKeys.Actions.Checked else AllIconsKeys.Actions.MoveDown,
                    enabled = canInsert,
                    onClick = {
                        if (IdeaCodeActions.insertAtCursor(project, code)) {
                            inserted = true
                        }
                    }
                )

                // Save to file button
                CodeActionButton(
                    tooltip = "Save to File",
                    iconKey = AllIconsKeys.Actions.MenuSaveall,
                    onClick = {
                        val fileName = IdeaCodeActions.getSuggestedFileName(language)
                        IdeaCodeActions.saveToFile(project, code, fileName)
                    }
                )
            }
        }
    }
}

@Composable
private fun CodeActionButton(
    tooltip: String,
    iconKey: org.jetbrains.jewel.ui.icon.IconKey,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Tooltip(tooltip = { Text(tooltip) }) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(24.dp)
                .hoverable(interactionSource)
                .background(
                    if (isHovered && enabled) AutoDevColors.Neutral.c700.copy(alpha = 0.3f)
                    else Color.Transparent
                )
        ) {
            Icon(
                key = iconKey,
                contentDescription = tooltip,
                modifier = Modifier.size(16.dp),
                tint = if (enabled) AutoDevColors.Neutral.c300 else AutoDevColors.Neutral.c600
            )
        }
    }
}

