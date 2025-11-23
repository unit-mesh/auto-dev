package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.document.docql.DocQLResult
import cc.unitmesh.devins.document.docql.parseDocQL
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import kotlinx.coroutines.launch

/**
 * DocQL search bar with syntax hints and query execution
 */
@Composable
fun DocQLSearchBar(
    onQueryExecute: suspend (String) -> DocQLResult,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isExecuting by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Column(modifier = modifier) {
        // Search input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { 
                    queryText = it
                    errorMessage = null
                    
                    // Real-time syntax validation
                    try {
                        if (it.isNotEmpty()) {
                            parseDocQL(it)
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter DocQL query (e.g., $.toc[*])") },
                singleLine = true,
                isError = errorMessage != null,
                leadingIcon = {
                    Icon(
                        imageVector = AutoDevComposeIcons.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (queryText.isNotEmpty()) {
                        IconButton(onClick = { queryText = "" }) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Close,
                                contentDescription = "Clear"
                            )
                        }
                    }
                }
            )
            
            // Execute button
            Button(
                onClick = {
                    scope.launch {
                        isExecuting = true
                        try {
                            onQueryExecute(queryText)
                            errorMessage = null
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Execution error"
                        } finally {
                            isExecuting = false
                        }
                    }
                },
                enabled = queryText.isNotEmpty() && errorMessage == null && !isExecuting
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Run")
                }
            }
            
            // Syntax help toggle
            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (isExpanded) "Hide syntax" else "Show syntax"
                )
            }
        }
        
        // Error message
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
                onQuerySelect = { queryText = it },
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "DocQL Syntax",
                style = MaterialTheme.typography.titleSmall
            )
            
            // TOC queries
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
            
            HorizontalDivider()
            
            // Entity queries
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
            
            HorizontalDivider()
            
            // Content queries
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
            
            HorizontalDivider()
            
            // Operators
            Text(
                text = "Operators: == (equals), ~= (contains), > (greater than), < (less than)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

