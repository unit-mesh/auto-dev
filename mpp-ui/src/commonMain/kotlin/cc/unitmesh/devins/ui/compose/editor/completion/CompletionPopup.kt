package cc.unitmesh.devins.ui.compose.editor.completion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionTriggerType
import kotlinx.coroutines.launch

@Composable
fun CompletionPopup(
    items: List<CompletionItem>,
    selectedIndex: Int,
    offset: IntOffset,
    onItemSelected: (CompletionItem) -> Unit,
    onSelectedIndexChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        onDismiss()
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices) {
            scope.launch {
                listState.animateScrollToItem(selectedIndex)
            }
        }
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties =
            PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
    ) {
        Surface(
            modifier =
                modifier
                    .width(450.dp)
                    .heightIn(max = 280.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ),
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 4.dp,
            tonalElevation = 1.dp
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(items) { index, item ->
                    CompletionItemRow(
                        item = item,
                        isSelected = index == selectedIndex,
                        onClick = { onItemSelected(item) },
                        onHover = { onSelectedIndexChanged(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionItemRow(
    item: CompletionItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onHover: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display icon using Material Icons instead of emoji text
        item.icon?.let { iconName ->
            val iconVector = CompletionIconMapper.getIcon(iconName)
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayText,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            item.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (item.description != "Directory") {
            val fileExtension = item.text.substringAfterLast('.', "")
            if (fileExtension.isNotEmpty() && fileExtension.length <= 4) {
                Text(
                    text = fileExtension.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    modifier =
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

object CompletionTrigger {
    fun shouldTrigger(char: Char): Boolean {
        return char in setOf('@', '/', '$', ':', '`')
    }

    fun getTriggerType(char: Char): CompletionTriggerType {
        return when (char) {
            '@' -> CompletionTriggerType.AGENT
            '/' -> CompletionTriggerType.COMMAND
            '$' -> CompletionTriggerType.VARIABLE
            ':' -> CompletionTriggerType.COMMAND_VALUE
            '`' -> CompletionTriggerType.CODE_FENCE
            else -> CompletionTriggerType.NONE
        }
    }

    fun buildContext(
        fullText: String,
        cursorPosition: Int,
        triggerType: CompletionTriggerType
    ): CompletionContext? {
        val triggerChar =
            when (triggerType) {
                CompletionTriggerType.AGENT -> '@'
                CompletionTriggerType.COMMAND -> '/'
                CompletionTriggerType.VARIABLE -> '$'
                CompletionTriggerType.COMMAND_VALUE -> ':'
                else -> return null
            }

        val triggerOffset = fullText.lastIndexOf(triggerChar, cursorPosition - 1)
        if (triggerOffset < 0) return null

        val queryText = fullText.substring(triggerOffset + 1, cursorPosition)

        if (queryText.contains('\n') || queryText.contains(' ')) {
            return null
        }

        return CompletionContext(
            fullText = fullText,
            cursorPosition = cursorPosition,
            triggerType = triggerType,
            triggerOffset = triggerOffset,
            queryText = queryText
        )
    }
}
