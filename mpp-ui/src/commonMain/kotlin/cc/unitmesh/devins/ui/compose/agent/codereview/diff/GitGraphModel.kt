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
 * Lane represents a visual column that commits flow through
 */
private data class Lane(
    val column: Int,
    val color: Color,
    val branchName: String = "",
    var isActive: Boolean = true
)

/**
 * Git Graph builder - constructs graph structure from commit list
 *
 * Algorithm:
 * 1. Start with main branch in column 0
 * 2. When branch detected: create new lane, branch off from current position
 * 3. When merge detected: merge branch lane back to target lane
 * 4. Track active lanes to manage column assignments
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

        // Track active lanes (columns)
        val lanes = mutableListOf<Lane>()
        lanes.add(Lane(0, branchColors[0], "main", true))

        var currentLane = lanes[0]
        var nextAvailableColumn = 1

        commitMessages.forEachIndexed { index, message ->
            val isMerge = detectMerge(message)
            val isBranchStart = detectBranchStart(message) && index < commitMessages.size - 1

            when {
                isMerge -> {
                    // Merge commit: current branch merges back to main
                    val targetLane = lanes.firstOrNull { it.column == 0 } ?: lanes.first()

                    // Create merge node on target lane
                    nodes[index] = GitGraphNode(
                        row = index,
                        column = targetLane.column,
                        type = GitGraphNodeType.MERGE,
                        color = targetLane.color,
                        parentColumns = listOf(currentLane.column, targetLane.column).distinct()
                    )

                    // Draw merge line from current lane to target lane
                    if (currentLane.column != targetLane.column && index > 0) {
                        lines.add(
                            GitGraphLine(
                                fromColumn = currentLane.column,
                                toColumn = targetLane.column,
                                fromRow = index - 1,
                                toRow = index,
                                color = currentLane.color,
                                isMerge = true
                            )
                        )
                    }

                    // Also draw vertical line from target lane if needed
                    if (index > 0 && currentLane.column != targetLane.column) {
                        lines.add(
                            GitGraphLine(
                                fromColumn = targetLane.column,
                                toColumn = targetLane.column,
                                fromRow = index - 1,
                                toRow = index,
                                color = targetLane.color,
                                isMerge = false
                            )
                        )
                    }

                    // Remove merged branch lane if it's not main
                    if (currentLane.column != 0) {
                        lanes.removeAll { it.column == currentLane.column }
                    }

                    currentLane = targetLane
                }

                isBranchStart -> {
                    // Start a new branch
                    val colorIndex = nextAvailableColumn % branchColors.size
                    val newLane = Lane(
                        nextAvailableColumn,
                        branchColors[colorIndex],
                        "branch-$nextAvailableColumn",
                        true
                    )
                    lanes.add(newLane)
                    nextAvailableColumn++

                    // Create branch start node
                    nodes[index] = GitGraphNode(
                        row = index,
                        column = newLane.column,
                        type = GitGraphNodeType.BRANCH_START,
                        color = newLane.color
                    )

                    // Draw branch line from parent lane to new lane
                    if (index > 0) {
                        lines.add(
                            GitGraphLine(
                                fromColumn = currentLane.column,
                                toColumn = newLane.column,
                                fromRow = index - 1,
                                toRow = index,
                                color = newLane.color,
                                isMerge = false
                            )
                        )

                        // Continue parent lane vertically
                        lines.add(
                            GitGraphLine(
                                fromColumn = currentLane.column,
                                toColumn = currentLane.column,
                                fromRow = index - 1,
                                toRow = index,
                                color = currentLane.color,
                                isMerge = false
                            )
                        )
                    }

                    // Switch to new branch lane
                    currentLane = newLane
                }

                else -> {
                    // Regular commit on current lane
                    nodes[index] = GitGraphNode(
                        row = index,
                        column = currentLane.column,
                        type = GitGraphNodeType.COMMIT,
                        color = currentLane.color
                    )

                    // Draw vertical line to next commit (unless it's the last)
                    if (index < commitMessages.size - 1) {
                        lines.add(
                            GitGraphLine(
                                fromColumn = currentLane.column,
                                toColumn = currentLane.column,
                                fromRow = index,
                                toRow = index + 1,
                                color = currentLane.color,
                                isMerge = false
                            )
                        )
                    }
                }
            }
        }

        val maxColumns = (nodes.values.maxOfOrNull { it.column } ?: 0) + 1

        return GitGraphStructure(nodes, lines, maxColumns)
    }

    /**
     * Detect if message indicates a merge commit
     */
    private fun detectMerge(message: String): Boolean {
        return message.contains("Merge", ignoreCase = true) ||
            message.contains("merge", ignoreCase = true)
    }

    /**
     * Detect if message indicates start of a new branch
     */
    private fun detectBranchStart(message: String): Boolean {
        return message.contains("feat:", ignoreCase = true) ||
            message.contains("feature:", ignoreCase = true) ||
            message.contains("branch", ignoreCase = true) &&
            !message.contains("Merge", ignoreCase = true)
    }

    /**
     * Generate ASCII representation of the graph for debugging
     */
    fun buildAsciiGraph(commitMessages: List<String>): String {
        val graph = buildGraph(commitMessages)
        val builder = StringBuilder()

        builder.appendLine("Git Graph ASCII Visualization")
        builder.appendLine("=".repeat(50))
        builder.appendLine()

        commitMessages.forEachIndexed { index, message ->
            val node = graph.nodes[index]
            if (node == null) {
                builder.appendLine("Row $index: [ERROR] No node")
                return@forEachIndexed
            }

            // Build the graph line representation
            val lineChars = CharArray(graph.maxColumns * 2 + 1) { ' ' }

            // Draw lines connected to this row
            graph.lines.filter { it.toRow == index || it.fromRow == index }
                .forEach { line ->
                    if (line.fromRow == index - 1 && line.toRow == index) {
                        // Incoming line
                        if (line.fromColumn == line.toColumn) {
                            // Vertical line
                            lineChars[line.fromColumn * 2] = '|'
                        } else if (line.isMerge) {
                            // Merge line
                            val minCol = minOf(line.fromColumn, line.toColumn)
                            val maxCol = maxOf(line.fromColumn, line.toColumn)
                            for (col in minCol..maxCol) {
                                lineChars[col * 2] = if (col == line.fromColumn || col == line.toColumn) '|' else '-'
                            }
                        } else {
                            // Branch line
                            val minCol = minOf(line.fromColumn, line.toColumn)
                            val maxCol = maxOf(line.fromColumn, line.toColumn)
                            for (col in minCol..maxCol) {
                                lineChars[col * 2] =
                                    if (col == line.toColumn) '/' else if (col == line.fromColumn) '|' else ' '
                            }
                        }
                    }
                }

            // Place the commit node
            val nodeChar = when (node.type) {
                GitGraphNodeType.COMMIT -> '*'
                GitGraphNodeType.MERGE -> 'M'
                GitGraphNodeType.BRANCH_START -> 'B'
                GitGraphNodeType.BRANCH_END -> 'E'
            }
            lineChars[node.column * 2] = nodeChar

            // Build output line
            val graphStr = lineChars.concatToString()
            val shortMessage = if (message.length > 40) message.take(37) + "..." else message
            builder.appendLine("$graphStr  $shortMessage")
        }

        builder.appendLine()
        builder.appendLine("Legend: * = commit, M = merge, B = branch start, / = branch line, | = continue")
        builder.appendLine("Columns: ${graph.maxColumns}, Nodes: ${graph.nodes.size}, Lines: ${graph.lines.size}")

        return builder.toString()
    }
}
