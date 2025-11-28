package cc.unitmesh.indexer.clustering

import kotlinx.serialization.Serializable

/**
 * Represents a dependency graph between files/modules
 * 依赖图模型：节点为文件，边为依赖关系
 */
@Serializable
data class DependencyGraph(
    /**
     * All nodes (files) in the graph
     */
    val nodes: Set<String>,
    
    /**
     * Edges: source -> list of targets (dependencies)
     */
    val edges: Map<String, Set<String>>
) {
    /**
     * Get all dependencies of a file
     */
    fun getDependencies(file: String): Set<String> = edges[file] ?: emptySet()
    
    /**
     * Get all files that depend on this file (reverse dependencies)
     */
    fun getDependents(file: String): Set<String> {
        return edges.entries
            .filter { (_, deps) -> file in deps }
            .map { it.key }
            .toSet()
    }
    
    /**
     * Get bidirectional connections (A depends on B or B depends on A)
     */
    fun getConnected(file: String): Set<String> {
        return getDependencies(file) + getDependents(file)
    }
    
    /**
     * Calculate node degree (in + out edges)
     */
    fun getDegree(file: String): Int {
        return getDependencies(file).size + getDependents(file).size
    }
    
    /**
     * Get files with no dependencies and no dependents
     */
    fun getIsolatedNodes(): Set<String> {
        return nodes.filter { getDegree(it) == 0 }.toSet()
    }
    
    /**
     * Get hub nodes (high connectivity)
     */
    fun getHubNodes(threshold: Int = 5): Set<String> {
        return nodes.filter { getDegree(it) >= threshold }.toSet()
    }
    
    /**
     * Calculate graph density
     */
    fun getDensity(): Float {
        if (nodes.size <= 1) return 0f
        val maxEdges = nodes.size * (nodes.size - 1)
        val actualEdges = edges.values.sumOf { it.size }
        return actualEdges.toFloat() / maxEdges.toFloat()
    }
    
    /**
     * Get strongly connected component containing the given file
     * Simplified: returns files reachable within 2 hops
     */
    fun getLocalCluster(file: String, maxHops: Int = 2): Set<String> {
        val visited = mutableSetOf<String>()
        val queue = mutableListOf(file to 0)
        
        while (queue.isNotEmpty()) {
            val (current, depth) = queue.removeFirst()
            if (current in visited || depth > maxHops) continue
            
            visited.add(current)
            if (depth < maxHops) {
                getConnected(current).forEach { neighbor ->
                    if (neighbor !in visited) {
                        queue.add(neighbor to depth + 1)
                    }
                }
            }
        }
        
        return visited
    }
    
    companion object {
        fun empty(): DependencyGraph = DependencyGraph(emptySet(), emptyMap())
    }
}

/**
 * Builder for constructing dependency graphs
 */
class DependencyGraphBuilder {
    private val nodes = mutableSetOf<String>()
    private val edges = mutableMapOf<String, MutableSet<String>>()
    
    fun addNode(file: String): DependencyGraphBuilder {
        nodes.add(file)
        return this
    }
    
    fun addNodes(files: Collection<String>): DependencyGraphBuilder {
        nodes.addAll(files)
        return this
    }
    
    fun addEdge(source: String, target: String): DependencyGraphBuilder {
        nodes.add(source)
        nodes.add(target)
        edges.getOrPut(source) { mutableSetOf() }.add(target)
        return this
    }
    
    fun addEdges(source: String, targets: Collection<String>): DependencyGraphBuilder {
        nodes.add(source)
        nodes.addAll(targets)
        edges.getOrPut(source) { mutableSetOf() }.addAll(targets)
        return this
    }
    
    fun build(): DependencyGraph {
        return DependencyGraph(
            nodes = nodes.toSet(),
            edges = edges.mapValues { it.value.toSet() }
        )
    }
}

