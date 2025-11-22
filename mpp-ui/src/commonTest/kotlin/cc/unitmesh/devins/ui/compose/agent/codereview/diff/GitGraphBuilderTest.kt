package cc.unitmesh.devins.ui.compose.agent.codereview.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitGraphBuilderTest {
    
    @Test
    fun testLinearHistory() {
        val commits = listOf(
            "Initial commit",
            "Add feature A",
            "Fix bug in feature A",
            "Update documentation"
        )
        
        val graph = GitGraphBuilder.buildGraph(commits)
        
        println("\n=== Test: Linear History ===")
        println(GitGraphBuilder.buildAsciiGraph(commits))
        
        // All commits should be in column 0 (main branch)
        assertEquals(1, graph.maxColumns, "Linear history should use only 1 column")
        assertEquals(4, graph.nodes.size, "Should have 4 nodes")
        
        graph.nodes.values.forEach { node ->
            assertEquals(0, node.column, "All nodes should be in column 0")
            assertEquals(GitGraphNodeType.COMMIT, node.type, "All should be regular commits")
        }
    }
    
    @Test
    fun testSimpleBranchAndMerge() {
        val commits = listOf(
            "Initial commit",
            "feat: Start new feature",
            "Implement feature logic",
            "Merge branch 'feature' into main",
            "Continue on main"
        )
        
        val graph = GitGraphBuilder.buildGraph(commits)
        
        println("\n=== Test: Simple Branch and Merge ===")
        println(GitGraphBuilder.buildAsciiGraph(commits))
        
        assertEquals(2, graph.maxColumns, "Should have 2 columns (main + feature)")
        assertEquals(5, graph.nodes.size, "Should have 5 nodes")
        
        // Check node types
        assertEquals(GitGraphNodeType.COMMIT, graph.nodes[0]?.type)
        assertEquals(GitGraphNodeType.BRANCH_START, graph.nodes[1]?.type)
        assertEquals(GitGraphNodeType.COMMIT, graph.nodes[2]?.type)
        assertEquals(GitGraphNodeType.MERGE, graph.nodes[3]?.type)
        assertEquals(GitGraphNodeType.COMMIT, graph.nodes[4]?.type)
        
        // Check columns
        assertEquals(0, graph.nodes[0]?.column, "Initial commit on main")
        assertEquals(1, graph.nodes[1]?.column, "Branch start in column 1")
        assertEquals(1, graph.nodes[2]?.column, "Feature work in column 1")
        assertEquals(0, graph.nodes[3]?.column, "Merge back to column 0")
        assertEquals(0, graph.nodes[4]?.column, "Continue on main")
    }
    
    @Test
    fun testMultipleFeatureBranches() {
        val commits = listOf(
            "Initial commit",
            "feat: Add authentication",
            "Implement OAuth",
            "Merge branch 'auth' into main",
            "feat: Add user profile",
            "Design profile page",
            "Merge branch 'profile' into main",
            "Final cleanup"
        )
        
        val graph = GitGraphBuilder.buildGraph(commits)
        
        println("\n=== Test: Multiple Feature Branches ===")
        println(GitGraphBuilder.buildAsciiGraph(commits))
        
        assertTrue(graph.maxColumns >= 2, "Should have at least 2 columns")
        assertEquals(8, graph.nodes.size, "Should have 8 nodes")
        
        // Verify merge commits are back on column 0
        assertEquals(0, graph.nodes[3]?.column, "First merge to main")
        assertEquals(0, graph.nodes[6]?.column, "Second merge to main")
    }
    
    @Test
    fun testComplexScenario() {
        val commits = listOf(
            "Initial setup",
            "feat: Start feature A",
            "Work on A - part 1",
            "Work on A - part 2",
            "Merge branch 'feature-a' into main",
            "Hotfix on main",
            "feat: Start feature B",
            "Work on B",
            "Merge branch 'feature-b' into main",
            "Final commit"
        )
        
        val graph = GitGraphBuilder.buildGraph(commits)
        
        println("\n=== Test: Complex Scenario ===")
        println(GitGraphBuilder.buildAsciiGraph(commits))
        
        assertEquals(10, graph.nodes.size, "Should have 10 nodes")
        assertTrue(graph.maxColumns >= 2, "Should use multiple columns")
        
        // Check that we have the expected number of each node type
        val nodeTypes = graph.nodes.values.groupBy { it.type }
        assertTrue(nodeTypes[GitGraphNodeType.BRANCH_START]?.size ?: 0 >= 1, "Should have branch starts")
        assertTrue(nodeTypes[GitGraphNodeType.MERGE]?.size ?: 0 >= 1, "Should have merges")
        assertTrue(nodeTypes[GitGraphNodeType.COMMIT]?.size ?: 0 >= 1, "Should have regular commits")
    }
    
    @Test
    fun testGraphStructureIntegrity() {
        val commits = listOf(
            "feat: Branch 1",
            "Work on branch 1",
            "Merge branch 1",
            "feat: Branch 2",
            "Merge branch 2"
        )
        
        val graph = GitGraphBuilder.buildGraph(commits)
        
        println("\n=== Test: Graph Structure Integrity ===")
        println(GitGraphBuilder.buildAsciiGraph(commits))
        
        // Every node should have at least one line connected to it (except possibly the last)
        graph.nodes.forEach { (index, node) ->
            val connectedLines = graph.lines.filter { 
                (it.fromRow == index && it.fromColumn == node.column) ||
                (it.toRow == index && it.toColumn == node.column)
            }
            
            if (index < commits.size - 1) {
                assertTrue(connectedLines.isNotEmpty(), "Node $index should have connected lines")
            }
        }
    }
}

