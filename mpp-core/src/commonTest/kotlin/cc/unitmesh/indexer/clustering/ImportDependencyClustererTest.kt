package cc.unitmesh.indexer.clustering

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportDependencyClustererTest {
    
    private val clusterer = ImportDependencyClusterer()
    
    @Test
    fun testClusteringWithEmptyFiles() = kotlinx.coroutines.test.runTest {
        val clusters = clusterer.cluster(
            files = emptyList(),
            context = ClusteringContext()
        )
        assertTrue(clusters.isEmpty())
    }
    
    @Test
    fun testClusteringWithSingleFile() = kotlinx.coroutines.test.runTest {
        val files = listOf("src/main/kotlin/UserService.kt")
        val clusters = clusterer.cluster(
            files = files,
            context = ClusteringContext(
                includeIsolatedFiles = true,
                minClusterSize = 1
            )
        )
        
        assertEquals(1, clusters.size)
        assertEquals(1, clusters.first().files.size)
    }
    
    @Test
    fun testClusteringWithDependencies() = kotlinx.coroutines.test.runTest {
        val files = listOf(
            "src/main/kotlin/user/UserService.kt",
            "src/main/kotlin/user/UserRepository.kt",
            "src/main/kotlin/order/OrderService.kt"
        )
        
        val fileContents = mapOf(
            "src/main/kotlin/user/UserService.kt" to """
                package user
                import user.UserRepository
                
                class UserService(private val repository: UserRepository)
            """.trimIndent(),
            "src/main/kotlin/user/UserRepository.kt" to """
                package user
                
                interface UserRepository
            """.trimIndent(),
            "src/main/kotlin/order/OrderService.kt" to """
                package order
                
                class OrderService
            """.trimIndent()
        )
        
        val clusters = clusterer.cluster(
            files = files,
            context = ClusteringContext(
                fileContents = fileContents,
                minClusterSize = 1,
                includeIsolatedFiles = true
            )
        )
        
        assertTrue(clusters.isNotEmpty())
        
        // User files should be clustered together
        val userCluster = clusters.find { cluster -> 
            cluster.files.any { it.contains("UserService") } 
        }
        assertTrue(userCluster != null)
    }
    
    @Test
    fun testDomainTermExtraction() = kotlinx.coroutines.test.runTest {
        val files = listOf(
            "src/main/kotlin/user/UserController.kt",
            "src/main/kotlin/user/UserService.kt",
            "src/main/kotlin/user/UserRepository.kt"
        )
        
        val clusters = clusterer.cluster(
            files = files,
            context = ClusteringContext(
                minClusterSize = 1,
                includeIsolatedFiles = true
            )
        )
        
        // All files should be in one cluster (no dependencies, isolated)
        // Check that domain terms are extracted
        val allTerms = clusters.flatMap { it.domainTerms }
        assertTrue(allTerms.contains("User") || allTerms.any { it.contains("User") })
    }
    
    @Test
    fun testClusteringStatistics() {
        val clusters = listOf(
            ModuleCluster(
                id = "cluster-1",
                name = "User",
                files = listOf("UserService.kt", "UserRepository.kt"),
                cohesion = 0.8f,
                coupling = 0.2f
            ),
            ModuleCluster(
                id = "cluster-2",
                name = "Order",
                files = listOf("OrderService.kt"),
                cohesion = 1.0f,
                coupling = 0.0f
            )
        )
        
        val stats = clusterer.getStatistics(clusters)
        
        assertEquals(3, stats.totalFiles)
        assertEquals(2, stats.clusterCount)
        assertEquals(2, stats.maxClusterSize)
        assertEquals(1, stats.minClusterSize)
        assertTrue(stats.averageCohesion > 0)
    }
}

class DependencyGraphTest {
    
    @Test
    fun testEmptyGraph() {
        val graph = DependencyGraph.empty()
        assertTrue(graph.nodes.isEmpty())
        assertTrue(graph.edges.isEmpty())
    }
    
    @Test
    fun testGraphBuilder() {
        val graph = DependencyGraphBuilder()
            .addNode("A.kt")
            .addNode("B.kt")
            .addEdge("A.kt", "B.kt")
            .build()
        
        assertEquals(2, graph.nodes.size)
        assertTrue(graph.getDependencies("A.kt").contains("B.kt"))
        assertTrue(graph.getDependents("B.kt").contains("A.kt"))
    }
    
    @Test
    fun testConnectedNodes() {
        val graph = DependencyGraphBuilder()
            .addEdge("A.kt", "B.kt")
            .addEdge("B.kt", "C.kt")
            .build()
        
        val connected = graph.getConnected("B.kt")
        assertTrue(connected.contains("A.kt"))
        assertTrue(connected.contains("C.kt"))
    }
    
    @Test
    fun testNodeDegree() {
        val graph = DependencyGraphBuilder()
            .addEdge("A.kt", "B.kt")
            .addEdge("C.kt", "B.kt")
            .addEdge("B.kt", "D.kt")
            .build()
        
        // B has 2 incoming (from A and C) and 1 outgoing (to D)
        assertEquals(3, graph.getDegree("B.kt"))
    }
    
    @Test
    fun testIsolatedNodes() {
        val graph = DependencyGraphBuilder()
            .addNode("isolated.kt")
            .addEdge("A.kt", "B.kt")
            .build()
        
        val isolated = graph.getIsolatedNodes()
        assertTrue(isolated.contains("isolated.kt"))
        assertEquals(1, isolated.size)
    }
    
    @Test
    fun testLocalCluster() {
        val graph = DependencyGraphBuilder()
            .addEdge("A.kt", "B.kt")
            .addEdge("B.kt", "C.kt")
            .addEdge("C.kt", "D.kt")
            .build()
        
        // From B with max 1 hop, should get A, B, C
        val cluster = graph.getLocalCluster("B.kt", maxHops = 1)
        assertTrue(cluster.contains("A.kt"))
        assertTrue(cluster.contains("B.kt"))
        assertTrue(cluster.contains("C.kt"))
        assertEquals(3, cluster.size)
    }
    
    @Test
    fun testHubNodes() {
        val graph = DependencyGraphBuilder()
            .addEdge("A.kt", "Hub.kt")
            .addEdge("B.kt", "Hub.kt")
            .addEdge("C.kt", "Hub.kt")
            .addEdge("Hub.kt", "D.kt")
            .addEdge("Hub.kt", "E.kt")
            .build()
        
        val hubs = graph.getHubNodes(threshold = 4)
        assertTrue(hubs.contains("Hub.kt"))
        assertEquals(1, hubs.size)
    }
}

