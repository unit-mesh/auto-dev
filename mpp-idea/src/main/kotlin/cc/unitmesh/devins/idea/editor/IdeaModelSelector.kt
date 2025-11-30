package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.Orientation

/**
 * Model selector for IntelliJ IDEA plugin.
 * Provides a dropdown for selecting LLM models with a configure option.
 *
 * Uses Jewel components for native IntelliJ IDEA look and feel.
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

    val currentConfig = remember(currentConfigName, availableConfigs) {
        availableConfigs.find { it.name == currentConfigName }
    }

    val displayText = remember(currentConfig) {
        currentConfig?.model ?: "Configure Model"
    }

    Box(modifier = modifier) {
        // Main selector button
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayText,
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
                    maxLines = 1
                )
                Icon(
                    imageVector = IdeaComposeIcons.ArrowDropDown,
                    contentDescription = null,
                    tint = JewelTheme.globalColors.text.normal,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Dropdown popup
        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = 300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JewelTheme.globalColors.panelBackground)
                        .padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        if (availableConfigs.isNotEmpty()) {
                            availableConfigs.forEach { config ->
                                IdeaDropdownMenuItem(
                                    text = "${config.provider} / ${config.model}",
                                    isSelected = config.name == currentConfigName,
                                    onClick = {
                                        onConfigSelect(config)
                                        expanded = false
                                    }
                                )
                            }

                            Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        } else {
                            IdeaDropdownMenuItem(
                                text = "No saved configs",
                                isSelected = false,
                                enabled = false,
                                onClick = {}
                            )

                            Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        }

                        // Configure button
                        IdeaDropdownMenuItem(
                            text = "Configure Model...",
                            isSelected = false,
                            leadingIcon = IdeaComposeIcons.Settings,
                            onClick = {
                                onConfigureClick()
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual menu item for IdeaModelSelector dropdown.
 */
@Composable
private fun IdeaDropdownMenuItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val backgroundColor = when {
        !enabled -> JewelTheme.globalColors.panelBackground
        isSelected -> JewelTheme.globalColors.panelBackground.copy(alpha = 0.6f)
        else -> JewelTheme.globalColors.panelBackground
    }

    val textColor = when {
        !enabled -> JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
        else -> JewelTheme.globalColors.text.normal
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = text,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 13.sp,
                color = textColor
            ),
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = IdeaComposeIcons.Check,
                contentDescription = "Selected",
                tint = JewelTheme.globalColors.text.normal,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

