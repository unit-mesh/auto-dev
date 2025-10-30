package cc.unitmesh.devins.ui.compose.editor.completion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionTriggerType
import kotlinx.coroutines.launch

/**
 * 补全弹窗组件
 */
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
    
    // 当选中项改变时，滚动到可见区域
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
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
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

/**
 * 单个补全项行
 */
@Composable
private fun CompletionItemRow(
    item: CompletionItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onHover: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标（emoji）
        item.icon?.let { icon ->
            Text(
                text = icon,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        
        // 主要内容 - 更紧凑的布局
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 显示文本
            Text(
                text = item.displayText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            
            // 描述文本 - 放在同一行右侧
            item.description?.let { description ->
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
            }
        }
    }
}

/**
 * 补全触发器 - 检测是否应该触发补全
 */
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
        // 找到最近的触发字符
        val triggerChar = when (triggerType) {
            CompletionTriggerType.AGENT -> '@'
            CompletionTriggerType.COMMAND -> '/'
            CompletionTriggerType.VARIABLE -> '$'
            CompletionTriggerType.COMMAND_VALUE -> ':'
            else -> return null
        }
        
        val triggerOffset = fullText.lastIndexOf(triggerChar, cursorPosition - 1)
        if (triggerOffset < 0) return null
        
        // 提取查询文本（触发字符后到光标的文本）
        val queryText = fullText.substring(triggerOffset + 1, cursorPosition)
        
        // 检查查询文本是否有效（不包含空白字符或换行）
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

