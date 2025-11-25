package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.document.docql.DocQLResult
import cc.unitmesh.devins.document.docql.parseDocQL
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import kotlinx.coroutines.launch

/**
 * DocQL search bar with syntax hints, auto-complete and real-time query execution
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocQLSearchBar(
    onQueryExecute: suspend (String) -> DocQLResult,
    autoExecute: Boolean = true,
    modifier: Modifier = Modifier
) {
    var queryTextValue by remember { mutableStateOf(TextFieldValue("")) }
    var isExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isExecuting by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<DocQLSuggestion>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val autoCompleteProvider = remember { DocQLAutoCompleteProvider() }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = queryTextValue,
                    onValueChange = { newValue ->
                        val oldText = queryTextValue.text
                        queryTextValue = newValue
                        errorMessage = null

                        val text = newValue.text
                        val cursor = newValue.selection.start

                        try {
                            if (text.isNotEmpty()) {
                                parseDocQL(text)
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message
                        }

                        // Only trigger auto-complete on actual text input (not cursor movement)
                        val textChanged = text != oldText
                        if (textChanged) {
                            if (cursor > 0 && text.length >= oldText.length) {
                                // 文本增加（输入字符）
                                val lastChar = text.getOrNull(cursor - 1)
                                val beforeCursor = text.substring(0, cursor)

                                // Trigger on: . [ $ @
                                val shouldShowSuggestions = when (lastChar) {
                                    '.' -> true
                                    '[' -> true
                                    '$' -> cursor == 1  // 只在开头输入 $ 时触发
                                    '@' -> beforeCursor.contains("[?")  // 在 filter 中
                                    else -> false
                                }

                                if (shouldShowSuggestions) {
                                    suggestions = autoCompleteProvider.getSuggestions(text, cursor)
                                    showSuggestions = suggestions.isNotEmpty()
                                }
                            } else if (text.length < oldText.length) {
                                val beforeCursor = text.substring(0, minOf(cursor, text.length))
                                val hasTriggerChar = beforeCursor.endsWith(".") ||
                                    beforeCursor.endsWith("[") ||
                                    beforeCursor.endsWith("@")

                                if (!hasTriggerChar) {
                                    showSuggestions = false
                                }
                            }
                        }

                        if (autoExecute && text.isNotEmpty() && errorMessage == null && !isExecuting) {
                            scope.launch {
                                isExecuting = true
                                try {
                                    onQueryExecute(text)
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Execution error"
                                } finally {
                                    isExecuting = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "输入 DocQL 查询",
                            maxLines = 1,
                            fontSize = 12.sp,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    leadingIcon = {
                        Icon(
                            imageVector = AutoDevComposeIcons.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (isExecuting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp).padding(end = 4.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            if (queryTextValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        queryTextValue = TextFieldValue("")
                                        showSuggestions = false
                                        errorMessage = null
                                    },
                                ) {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.Close,
                                        contentDescription = "清除",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (!autoExecute) {
                    Button(
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                try {
                                    onQueryExecute(queryTextValue.text)
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Execution error"
                                } finally {
                                    isExecuting = false
                                }
                            }
                        },
                        enabled = queryTextValue.text.isNotEmpty() && errorMessage == null && !isExecuting
                    ) {
                        Text("Run")
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (isExpanded) "Hide syntax" else "Show syntax"
                    )
                }
            }

            if (showSuggestions) {
                DocQLAutoCompletePopup(
                    suggestions = suggestions,
                    onSuggestionSelected = { suggestion ->
                        val currentText = queryTextValue.text
                        val cursor = queryTextValue.selection.start

                        // Insert suggestion at cursor position
                        val newText = currentText.substring(0, cursor) + suggestion.insertText
                        val newCursor = cursor + suggestion.insertText.length

                        queryTextValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursor)
                        )
                        showSuggestions = false  // 选择后自动关闭
                    },
                    onDismiss = { showSuggestions = false }  // 点击外部或按 ESC 关闭
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Syntax hints (expandable)
        if (isExpanded) {
            DocQLSyntaxHelp(
                onQuerySelect = {
                    queryTextValue = TextFieldValue(
                        text = it,
                        selection = TextRange(it.length)
                    )
                    showSuggestions = false
                },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * DocQL syntax help panel with examples
 */
@Composable
private fun DocQLSyntaxHelp(
    onQuerySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DocQL Syntax",
                style = MaterialTheme.typography.titleSmall
            )

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // TOC queries
                item {
                    SyntaxSection(
                        title = "Table of Contents (TOC)",
                        examples = listOf(
                            "$.toc[*]" to "All TOC items",
                            "$.toc[0]" to "First TOC item",
                            "$.toc[?(@.level==1)]" to "Level 1 headings",
                            """$.toc[?(@.title~="架构")]""" to "TOC items with '架构' in title"
                        ),
                        onQuerySelect = onQuerySelect
                    )
                }

                item {
                    HorizontalDivider()
                }

                // Entity queries
                item {
                    SyntaxSection(
                        title = "Entities",
                        examples = listOf(
                            "$.entities[*]" to "All entities",
                            """$.entities[?(@.type=="Term")]""" to "Term entities",
                            """$.entities[?(@.type=="API")]""" to "API entities",
                            """$.entities[?(@.name~="User")]""" to "Entities with 'User' in name"
                        ),
                        onQuerySelect = onQuerySelect
                    )
                }

                item {
                    HorizontalDivider()
                }

                // Content queries
                item {
                    SyntaxSection(
                        title = "Content",
                        examples = listOf(
                            """$.content.heading("架构")""" to "Sections with '架构' in heading",
                            """$.content.chapter("1.2")""" to "Chapter 1.2 content",
                            """$.content.h1("Introduction")""" to "H1 with 'Introduction'",
                            """$.content.h2("Design")""" to "H2 with 'Design'",
                            """$.content.grep("keyword")""" to "Full-text search for 'keyword'"
                        ),
                        onQuerySelect = onQuerySelect
                    )
                }

                item {
                    HorizontalDivider()
                }
                
                // Code queries
                item {
                    SyntaxSection(
                        title = "Source Code (🆕 全局搜索)",
                        examples = listOf(
                            """$.content.heading("DocQLExecutor")""" to "Find class/method by name",
                            """$.entities[?(@.type=="ClassEntity")]""" to "All classes",
                            """$.entities[?(@.type=="FunctionEntity")]""" to "All methods/functions",
                            """$.entities[?(@.name~="parse")]""" to "Functions with 'parse' in name",
                            """$.toc[*]""" to "Code structure (package -> class -> method)"
                        ),
                        onQuerySelect = onQuerySelect
                    )
                }

                item {
                    HorizontalDivider()
                }

                // Operators
                item {
                    Text(
                        text = "Operators: == (equals), ~= (contains), > (greater than), < (less than)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Global search hint
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "💡 未选中文档时，查询将在所有已索引的文档中搜索（包括源代码）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Syntax section with clickable examples
 */
@Composable
private fun SyntaxSection(
    title: String,
    examples: List<Pair<String, String>>,
    onQuerySelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        examples.forEach { (query, description) ->
            SyntaxExample(
                query = query,
                description = description,
                onClick = { onQuerySelect(query) }
            )
        }
    }
}

/**
 * Clickable syntax example
 */
@Composable
private fun SyntaxExample(
    query: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = query,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

