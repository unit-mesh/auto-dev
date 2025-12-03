package cc.unitmesh.devins.idea.toolwindow.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.DiffUtils
import cc.unitmesh.agent.diff.FileChange
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import org.jetbrains.jewel.bridge.compose
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.Dimension
import javax.swing.JComponent

/**
 * Dialog for displaying file change diff using IntelliJ's DialogWrapper.
 * Uses Jewel Compose for the content rendering.
 */
@Composable
fun IdeaFileChangeDiffDialog(
    project: Project,
    change: FileChange,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit
) {
    // Show the dialog using DialogWrapper
    IdeaFileChangeDiffDialogWrapper.show(
        project = project,
        change = change,
        onUndo = onUndo,
        onKeep = onKeep,
        onDismiss = onDismiss
    )
}

/**
 * DialogWrapper implementation for file change diff dialog.
 */
class IdeaFileChangeDiffDialogWrapper(
    private val project: Project,
    private val change: FileChange,
    private val onUndoCallback: () -> Unit,
    private val onKeepCallback: () -> Unit,
    private val onDismissCallback: () -> Unit
) : DialogWrapper(project) {

    init {
        title = "Diff: ${change.getFileName()}"
        init()
        contentPanel.border = JBUI.Borders.empty()
        rootPane.border = JBUI.Borders.empty()
    }

    override fun createSouthPanel(): JComponent? = null

    override fun createCenterPanel(): JComponent {
        val dialogPanel = compose {
            DiffDialogContent(
                change = change,
                onDismiss = {
                    onDismissCallback()
                    close(CANCEL_EXIT_CODE)
                },
                onUndo = {
                    onUndoCallback()
                    close(OK_EXIT_CODE)
                },
                onKeep = {
                    onKeepCallback()
                    close(OK_EXIT_CODE)
                }
            )
        }
        dialogPanel.preferredSize = Dimension(800, 600)
        return dialogPanel
    }

    override fun doCancelAction() {
        onDismissCallback()
        super.doCancelAction()
    }

    companion object {
        fun show(
            project: Project,
            change: FileChange,
            onUndo: () -> Unit,
            onKeep: () -> Unit,
            onDismiss: () -> Unit
        ): Boolean {
            val dialog = IdeaFileChangeDiffDialogWrapper(
                project = project,
                change = change,
                onUndoCallback = onUndo,
                onKeepCallback = onKeep,
                onDismissCallback = onDismiss
            )
            return dialog.showAndGet()
        }
    }
}

@Composable
private fun DiffDialogContent(
    change: FileChange,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit
) {
    val scrollState = rememberScrollState()
    val diffContent = DiffUtils.generateUnifiedDiff(
        oldContent = change.originalContent ?: "",
        newContent = change.newContent ?: "",
        filePath = change.filePath
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(16.dp)
    ) {
        // Header
        DiffDialogHeader(change = change)

        Spacer(modifier = Modifier.height(12.dp))

        // Diff content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(AutoDevColors.Neutral.c900)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                diffContent.lines().forEach { line ->
                    DiffLine(line = line)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        DiffDialogActions(
            onDismiss = onDismiss,
            onUndo = onUndo,
            onKeep = onKeep
        )
    }
}

@Composable
private fun DiffDialogHeader(change: FileChange) {
    val diffStats = change.getDiffStats()
    val iconKey = when (change.changeType) {
        ChangeType.CREATE -> AllIconsKeys.General.Add
        ChangeType.EDIT -> AllIconsKeys.Actions.Edit
        ChangeType.DELETE -> AllIconsKeys.General.Remove
        ChangeType.RENAME -> AllIconsKeys.Actions.Edit // Use Edit as fallback for Rename
    }
    val iconColor = when (change.changeType) {
        ChangeType.CREATE -> AutoDevColors.Green.c400
        ChangeType.EDIT -> AutoDevColors.Blue.c400
        ChangeType.DELETE -> AutoDevColors.Red.c400
        ChangeType.RENAME -> AutoDevColors.Indigo.c400 // Use Indigo instead of Purple
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                key = iconKey,
                contentDescription = change.changeType.name,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
            Column {
                Text(
                    text = change.getFileName(),
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = change.filePath,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        color = AutoDevColors.Neutral.c400
                    )
                )
            }
        }

        // Diff stats
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "+${diffStats.addedLines}",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    color = AutoDevColors.Green.c400,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = "-${diffStats.deletedLines}",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    color = AutoDevColors.Red.c400,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun DiffLine(line: String) {
    val backgroundColor: Color
    val textColor: Color
    
    when {
        line.startsWith("+") && !line.startsWith("+++") -> {
            backgroundColor = AutoDevColors.Green.c900.copy(alpha = 0.3f)
            textColor = AutoDevColors.Green.c300
        }
        line.startsWith("-") && !line.startsWith("---") -> {
            backgroundColor = AutoDevColors.Red.c900.copy(alpha = 0.3f)
            textColor = AutoDevColors.Red.c300
        }
        line.startsWith("@@") -> {
            backgroundColor = AutoDevColors.Blue.c900.copy(alpha = 0.3f)
            textColor = AutoDevColors.Blue.c300
        }
        else -> {
            backgroundColor = Color.Transparent
            textColor = AutoDevColors.Neutral.c300
        }
    }

    Text(
        text = line,
        style = JewelTheme.defaultTextStyle.copy(
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = textColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

@Composable
private fun DiffDialogActions(
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text("Close")
        }

        OutlinedButton(
            onClick = onUndo,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Icon(
                key = AllIconsKeys.Actions.Rollback,
                contentDescription = "Undo",
                modifier = Modifier.size(14.dp),
                tint = AutoDevColors.Red.c400
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Undo", color = AutoDevColors.Red.c400)
        }

        DefaultButton(
            onClick = onKeep
        ) {
            Icon(
                key = AllIconsKeys.Actions.Checked,
                contentDescription = "Keep",
                modifier = Modifier.size(14.dp),
                tint = AutoDevColors.Green.c400
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Keep")
        }
    }
}
