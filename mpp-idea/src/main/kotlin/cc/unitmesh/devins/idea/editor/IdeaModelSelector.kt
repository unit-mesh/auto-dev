package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.llm.NamedModelConfig
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator

/**
 * Model selector for IntelliJ IDEA plugin.
 * Provides a dropdown for selecting LLM models with a configure option.
 *
 * Uses Jewel components for native IntelliJ IDEA look and feel.
 * Designed to blend seamlessly with the toolbar background.
 */
@Composable
fun IdeaModelSelector(
    availableConfigs: List<NamedModelConfig>,
    currentConfigName: String?,
    onConfigSelect: (NamedModelConfig) -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val currentConfig = remember(currentConfigName, availableConfigs) {
        availableConfigs.find { it.name == currentConfigName }
    }

    val displayText = remember(currentConfig) {
        currentConfig?.model ?: "Configure Model"
    }

    Box(modifier = modifier) {
        // Transparent selector that blends with background
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .hoverable(interactionSource = interactionSource)
                .background(
                    if (isHovered || expanded)
                        JewelTheme.globalColors.borders.normal.copy(alpha = 0.3f)
                    else
                        androidx.compose.ui.graphics.Color.Transparent
                )
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = IdeaComposeIcons.SmartToy,
                contentDescription = null,
                tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = displayText,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    color = JewelTheme.globalColors.text.normal
                ),
                maxLines = 1
            )
            Icon(
                imageVector = IdeaComposeIcons.ArrowDropDown,
                contentDescription = null,
                tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }

        // Dropdown popup using Jewel's PopupMenu for proper z-index handling with SwingPanel
        if (expanded) {
            PopupMenu(
                onDismissRequest = {
                    expanded = false
                    true
                },
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.widthIn(min = 200.dp, max = 300.dp)
            ) {
                if (availableConfigs.isNotEmpty()) {
                    availableConfigs.forEach { config ->
                        selectableItem(
                            selected = config.name == currentConfigName,
                            onClick = {
                                onConfigSelect(config)
                                expanded = false
                            }
                        ) {
                            Text(
                                text = "${config.provider} / ${config.model}",
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp)
                            )
                        }
                    }
                    separator()
                } else {
                    selectableItem(
                        selected = false,
                        enabled = false,
                        onClick = {}
                    ) {
                        Text(
                            text = "No saved configs",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 13.sp,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                            )
                        )
                    }
                    separator()
                }

                // Configure button
                selectableItem(
                    selected = false,
                    onClick = {
                        onConfigureClick()
                        expanded = false
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = IdeaComposeIcons.Settings,
                            contentDescription = null,
                            tint = JewelTheme.globalColors.text.normal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Configure Model...",
                            style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp)
                        )
                    }
                }
            }
        }
    }
}
