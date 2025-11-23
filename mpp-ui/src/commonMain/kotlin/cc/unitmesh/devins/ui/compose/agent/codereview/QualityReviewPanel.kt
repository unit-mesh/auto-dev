package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Quality Review Panel - Shows related tests for changed files
 */
@Composable
fun QualityReviewPanel(
    testFiles: List<TestFileInfo>,
    modifier: Modifier = Modifier,
    onTestFileClick: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(true) }

    // Filter out test files with parse errors and filter invalid test cases
    val validTestFiles = remember(testFiles) {
        testFiles
            .filter { it.parseError == null } // Exclude files with parse errors
            .map { testFile ->
                // Filter out invalid test cases (empty names, "unknown", etc.)
                val validTestCases = testFile.testCases.filter { testCase ->
                    testCase.name.isNotBlank() &&
                    testCase.name.lowercase() != "unknown" &&
                    !testCase.name.startsWith("<") &&
                    !testCase.name.startsWith("$")
                }
                testFile.copy(testCases = validTestCases)
            }
            .filter { it.testCases.isNotEmpty() } // Only keep files with valid test cases
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.CheckCircle,
                        contentDescription = "Quality Gate",
                        tint = AutoDevColors.Green.c600,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Quality Review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (validTestFiles.isNotEmpty()) {
                        Text(
                            text = "(${validTestFiles.size} test ${if (validTestFiles.size == 1) "file" else "files"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            if (expanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                if (validTestFiles.isEmpty()) {
                    // No tests found
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Error,
                                contentDescription = "No tests",
                                tint = AutoDevColors.Amber.c600,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No related tests found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Consider adding tests for this change",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Show test files
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(validTestFiles) { testFile ->
                            TestFileCard(
                                testFile = testFile,
                                onFileClick = onTestFileClick
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a single test file and its test cases
 */
@Composable
fun TestFileCard(
    testFile: TestFileInfo,
    onFileClick: ((String) -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // File header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (testFile.exists) AutoDevComposeIcons.Description else AutoDevComposeIcons.Error,
                        contentDescription = "Test file",
                        tint = if (testFile.exists) AutoDevColors.Green.c600 else AutoDevColors.Red.c600,
                        modifier = Modifier.size(16.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = testFile.filePath.substringAfterLast("/"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = testFile.filePath.substringBeforeLast("/", ""),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    if (testFile.testCases.isNotEmpty()) {
                        Text(
                            text = "${testFile.testCases.sumOf { it.children.size + if (it.type == TestNodeType.METHOD) 1 else 0 }} tests",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View file button
                    if (testFile.exists && onFileClick != null) {
                        IconButton(
                            onClick = { onFileClick(testFile.filePath) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Visibility,
                                contentDescription = "View file",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Test tree
            if (expanded) {
                if (testFile.parseError != null) {
                    // Show parse error
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Failed to parse: ${testFile.parseError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )

                    TestTreeView(
                        testCases = testFile.testCases,
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tree view for test cases
 */
@Composable
fun TestTreeView(
    testCases: List<TestCaseNode>,
    modifier: Modifier = Modifier,
    indentLevel: Int = 0
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        testCases.forEach { node ->
            TestNodeItem(
                node = node,
                indentLevel = indentLevel
            )
        }
    }
}

/**
 * Individual test node item (class or method)
 */
@Composable
fun TestNodeItem(
    node: TestCaseNode,
    indentLevel: Int
) {
    var expanded by remember { mutableStateOf(indentLevel == 0) } // Classes expanded by default

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Node row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = node.children.isNotEmpty()) {
                    expanded = !expanded
                }
                .padding(
                    start = (indentLevel * 16).dp,
                    top = 2.dp,
                    bottom = 2.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (node.children.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(14.dp))
            }

            Icon(
                imageVector = if (node.type == TestNodeType.CLASS)
                    AutoDevComposeIcons.Folder  // Use Folder for class
                else
                    AutoDevComposeIcons.Code,  // Use Code for method
                contentDescription = node.type.name,
                tint = if (node.type == TestNodeType.CLASS)
                    AutoDevColors.Blue.c600
                else
                    AutoDevColors.Indigo.c600,  // Use Indigo instead of Purple
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = node.name,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (node.type == TestNodeType.CLASS) FontWeight.Medium else FontWeight.Normal
            )

            if (node.startLine > 0) {
                Text(
                    text = "L${node.startLine}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Children
        if (expanded && node.children.isNotEmpty()) {
            TestTreeView(
                testCases = node.children,
                indentLevel = indentLevel + 1
            )
        }
    }
}
