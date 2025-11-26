package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * Reusable TopAppBar for Agent pages (CodeReview, DocumentReader, etc.)
 *
 * Provides a consistent top bar across all agent views with:
 * - Title with optional subtitle
 * - Optional leading icon
 * - Platform-aware back navigation (shown on WASM)
 * - Custom actions slot
 *
 * @param title Main title text
 * @param subtitle Optional subtitle (e.g., workspace name)
 * @param icon Optional leading icon displayed before the title
 * @param onBack Callback for back navigation (only shown on WASM platform)
 * @param actions Composable slot for custom action buttons
 * @param modifier Modifier for the TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentTopAppBar(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (icon != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (Platform.isWasm && onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = AutoDevComposeIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

/**
 * Common action buttons that can be used across different agent pages
 */
object AgentTopAppBarActions {
    /**
     * Refresh button action
     */
    @Composable
    fun RefreshButton(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = AutoDevComposeIcons.Refresh,
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    /**
     * Settings button action
     */
    @Composable
    fun SettingsButton(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = AutoDevComposeIcons.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    /**
     * Delete/Clear button action
     */
    @Composable
    fun DeleteButton(onClick: () -> Unit, contentDescription: String = "Delete") {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Delete,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

