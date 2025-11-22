package cc.unitmesh.devins.ui.compose.agent.codereview.diff

import androidx.compose.ui.graphics.Color

/**
 * Git Graph visualization model
 * Represents the visual structure of git commit history
 */

/**
 * Graph node type for rendering
 */
enum class GitGraphNodeType {
    COMMIT,          // Regular commit
    MERGE,           // Merge commit
    BRANCH_START,    // Branch starting point
    BRANCH_END       // Branch ending point
}

/**
 * Graph line/edge between commits
 */
data class GitGraphLine(
    val fromColumn: Int,
    val toColumn: Int,
    val fromRow: Int,
    val toRow: Int,
    val color: Color,
    val isMerge: Boolean = false
)

/**
 * Graph node representing a single commit
 */
data class GitGraphNode(
    val row: Int,                    // Row index in the list
    val column: Int,                 // Column index (0 = main branch, 1+ = feature branches)
    val type: GitGraphNodeType,
    val color: Color,
    val parentColumns: List<Int> = emptyList(), // Column indices of parent commits
    val childColumns: List<Int> = emptyList()   // Column indices of child commits
)

/**
 * Complete graph structure for a commit list
 */
data class GitGraphStructure(
    val nodes: Map<Int, GitGraphNode>,  // Map from row index to node
    val lines: List<GitGraphLine>,
    val maxColumns: Int                  // Maximum number of columns needed
)

/**
 * Git Graph builder - constructs graph structure from commit list
 * 
 * This is a simplified implementation that works well for most common cases:
 * - Linear history: single column
 * - Feature branches: detected by message patterns
 * - Merge commits: detected by "Merge" keyword
 * 
 * For a full implementation with parent commit parsing, extend GitOperations
 */
object GitGraphBuilder {
    
    private val branchColors = listOf(
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF66BB6A), // Green
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF42A5F5), // Blue
        Color(0xFFAB47BC), // Purple
        Color(0xFF26C6DA), // Cyan
        Color(0xFFFFCA28), // Amber
        Color(0xFFEC407A), // Pink
    )
    
    /**
     * Build graph structure from commit messages
     * Uses heuristics to detect branches and merges
     */
    fun buildGraph(commitMessages: List<String>): GitGraphStructure {
        if (commitMessages.isEmpty()) {
            return GitGraphStructure(emptyMap(), emptyList(), 0)
        }
        
        val nodes = mutableMapOf<Int, GitGraphNode>()
        val lines = mutableListOf<GitGraphLine>()
        val columnStack = mutableListOf<BranchInfo>()
        
        // Initialize main branch
        columnStack.add(BranchInfo(0, branchColors[0], "main"))
        
        commitMessages.forEachIndexed { index, message ->
            val isMerge = message.contains("Merge", ignoreCase = true)
            val isBranchStart = message.contains("feat:", ignoreCase = true) ||
                    message.contains("feature:", ignoreCase = true) ||
                    message.contains("branch", ignoreCase = true)
            
            when {
                isMerge -> {
                    // Merge commit: merge back to main branch
                    val currentBranch = columnStack.removeLastOrNull() ?: columnStack.first()
                    val targetColumn = if (columnStack.isNotEmpty()) 0 else currentBranch.column
                    
                    // Create merge node
                    nodes[index] = GitGraphNode(
                        row = index,
                        column = targetColumn,
                        type = GitGraphNodeType.MERGE,
                        color = columnStack.firstOrNull()?.color ?: branchColors[0],
                        parentColumns = listOf(currentBranch.column, targetColumn).distinct()
                    )
                    
                    // Add merge line
                    if (currentBranch.column != targetColumn) {
                        lines.add(
                            GitGraphLine(
                                fromColumn = currentBranch.column,
                                toColumn = targetColumn,
                                fromRow = index - 1,
                                toRow = index,
                                color = currentBranch.color,
                                isMerge = true
                            )
                        )
                    }
                    
                    // Keep main branch
                    if (columnStack.isEmpty()) {
                        columnStack.add(BranchInfo(0, branchColors[0], "main"))
                    }
                }
                
                isBranchStart && index < commitMessages.size - 1 -> {
                    // Start a new branch
                    val newColumn = columnStack.size
                    val colorIndex = newColumn % branchColors.size
                    val newBranch = BranchInfo(newColumn, branchColors[colorIndex], "feature")
                    columnStack.add(newBranch)
                    
                    nodes[index] = GitGraphNode(
                        row = index,
                        column = newColumn,
                        type = GitGraphNodeType.BRANCH_START,
                        color = newBranch.color
                    )
                    
                    // Add branch line from previous commit
                    if (index > 0) {
                        val prevColumn = nodes[index - 1]?.column ?: 0
                        lines.add(
                            GitGraphLine(
                                fromColumn = prevColumn,
                                toColumn = newColumn,
                                fromRow = index - 1,
                                toRow = index,
                                color = newBranch.color
                            )
                        )
                    }
                }
                
                else -> {
                    // Regular commit on current branch
                    val currentBranch = columnStack.lastOrNull() ?: columnStack.first()
                    
                    nodes[index] = GitGraphNode(
                        row = index,
                        column = currentBranch.column,
                        type = GitGraphNodeType.COMMIT,
                        color = currentBranch.color
                    )
                }
            }
            
            // Add vertical line to next commit
            if (index < commitMessages.size - 1) {
                val currentNode = nodes[index]!!
                lines.add(
                    GitGraphLine(
                        fromColumn = currentNode.column,
                        toColumn = currentNode.column,
                        fromRow = index,
                        toRow = index + 1,
                        color = currentNode.color
                    )
                )
            }
        }
        
        val maxColumns = (nodes.values.maxOfOrNull { it.column } ?: 0) + 1
        
        return GitGraphStructure(nodes, lines, maxColumns)
    }
    
    private data class BranchInfo(
        val column: Int,
        val color: Color,
        val name: String
    )
}

