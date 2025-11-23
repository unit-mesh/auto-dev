package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

/**
 * DocQL 自动补全建议
 */
data class DocQLSuggestion(
    val text: String,
    val description: String,
    val insertText: String = text,
    val category: String = ""
)

/**
 * DocQL 自动补全提供器
 */
class DocQLAutoCompleteProvider {
    
    private val rootSuggestions = listOf(
        DocQLSuggestion("$.toc", "查询目录结构", "$.toc[*]", "Root"),
        DocQLSuggestion("$.entities", "查询实体列表", "$.entities[*]", "Root"),
        DocQLSuggestion("$.content", "查询文档内容", "$.content.", "Root")
    )
    
    private val tocSuggestions = listOf(
        DocQLSuggestion("[*]", "所有目录项", "[*]", "Array"),
        DocQLSuggestion("[0]", "第一个目录项", "[0]", "Array"),
        DocQLSuggestion("[?(@.level==1)]", "一级标题", "[?(@.level==1)]", "Filter"),
        DocQLSuggestion("[?(@.level==2)]", "二级标题", "[?(@.level==2)]", "Filter"),
        DocQLSuggestion("[?(@.title~=\"\")", "标题包含关键词", "[?(@.title~=\"\")]", "Filter")
    )
    
    private val entitySuggestions = listOf(
        DocQLSuggestion("[*]", "所有实体", "[*]", "Array"),
        DocQLSuggestion("[?(@.type==\"Term\")]", "术语实体", "[?(@.type==\"Term\")]", "Filter"),
        DocQLSuggestion("[?(@.type==\"API\")]", "API 实体", "[?(@.type==\"API\")]", "Filter"),
        DocQLSuggestion("[?(@.type==\"ClassEntity\")]", "类实体", "[?(@.type==\"ClassEntity\")]", "Filter"),
        DocQLSuggestion("[?(@.type==\"FunctionEntity\")]", "函数实体", "[?(@.type==\"FunctionEntity\")]", "Filter"),
        DocQLSuggestion("[?(@.name~=\"\")]", "名称包含关键词", "[?(@.name~=\"\")]", "Filter")
    )
    
    private val contentSuggestions = listOf(
        DocQLSuggestion("heading(\"\")", "查询标题", "heading(\"\")", "Function"),
        DocQLSuggestion("chapter(\"\")", "查询章节", "chapter(\"\")", "Function"),
        DocQLSuggestion("h1(\"\")", "查询 H1", "h1(\"\")", "Function"),
        DocQLSuggestion("h2(\"\")", "查询 H2", "h2(\"\")", "Function"),
        DocQLSuggestion("h3(\"\")", "查询 H3", "h3(\"\")", "Function"),
        DocQLSuggestion("grep(\"\")", "全文搜索", "grep(\"\")", "Function"),
        DocQLSuggestion("code[*]", "所有代码块", "code[*]", "Array"),
        DocQLSuggestion("table[*]", "所有表格", "table[*]", "Array")
    )
    
    /**
     * 获取自动补全建议
     * @param query 完整查询字符串
     * @param cursorPosition 光标位置
     * @param filterText 用户输入的过滤文本（可选）
     */
    fun getSuggestions(
        query: String, 
        cursorPosition: Int,
        filterText: String = ""
    ): List<DocQLSuggestion> {
        if (query.isEmpty() || cursorPosition == 0) {
            return rootSuggestions
        }
        
        val beforeCursor = query.substring(0, cursorPosition)
        
        val baseSuggestions = when {
            // 刚输入 $ 或为空
            beforeCursor == "$" -> rootSuggestions
            
            // $.toc 后面（输入 . 或 [）
            beforeCursor.matches(Regex("""\$\.toc[.\[]?\s*$""")) -> tocSuggestions
            
            // $.entities 后面（输入 . 或 [）
            beforeCursor.matches(Regex("""\$\.entities[.\[]?\s*$""")) -> entitySuggestions
            
            // $.content. 后面（输入 .）
            beforeCursor.matches(Regex("""\$\.content\.\s*$""")) -> contentSuggestions
            
            // 在 filter 中输入 @（输入 @ 后）
            beforeCursor.endsWith("@") && beforeCursor.contains("[?") -> {
                // 自动添加 . 的建议
                when {
                    beforeCursor.contains("$.toc") -> getTocFilterProperties()
                    beforeCursor.contains("$.entities") -> getEntityFilterProperties()
                    else -> emptyList()
                }
            }
            
            // 默认：不显示建议（需要明确触发）
            else -> emptyList()
        }
        
        // 如果有过滤文本，进行过滤
        return if (filterText.isNotEmpty()) {
            baseSuggestions.filter { 
                it.text.contains(filterText, ignoreCase = true) ||
                it.description.contains(filterText, ignoreCase = true)
            }
        } else {
            baseSuggestions
        }
    }
    
    private fun getTocFilterProperties() = listOf(
        DocQLSuggestion("@.level", "目录级别", "@.level", "Property"),
        DocQLSuggestion("@.title", "目录标题", "@.title", "Property"),
        DocQLSuggestion("@.page", "页码", "@.page", "Property")
    )
    
    private fun getEntityFilterProperties() = listOf(
        DocQLSuggestion("@.name", "实体名称", "@.name", "Property"),
        DocQLSuggestion("@.type", "实体类型", "@.type", "Property")
    )
    
    private fun getContextSuggestions(query: String): List<DocQLSuggestion> {
        return when {
            query.contains("$.toc") -> tocSuggestions
            query.contains("$.entities") -> entitySuggestions
            query.contains("$.content") -> contentSuggestions
            else -> rootSuggestions
        }
    }
}

/**
 * 自动补全下拉框
 */
@Composable
fun DocQLAutoCompletePopup(
    suggestions: List<DocQLSuggestion>,
    onSuggestionSelected: (DocQLSuggestion) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) {
        onDismiss()
        return
    }
    
    // 点击外部或按 ESC 关闭
    Popup(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.width(400.dp).heightIn(max = 300.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                // 提示信息
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "按 ESC 关闭，点击选择",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = { 
                                onSuggestionSelected(suggestion)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: DocQLSuggestion,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = suggestion.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (suggestion.category.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = suggestion.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            if (suggestion.description.isNotEmpty()) {
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    
    HorizontalDivider()
}

