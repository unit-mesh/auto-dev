package cc.unitmesh.diagram.parser

import cc.unitmesh.diagram.model.GraphvizEdgeType
import cc.unitmesh.diagram.model.GraphGraphType
import cc.unitmesh.diagram.model.GraphvizNodeType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DotFileParserTest {
    
    private val parser = DotFileParser()
    
    @Test
    fun `should parse simple directed graph`() {
        val dotContent = """
            digraph G {
                A -> B;
                B -> C;
                A -> C;
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(GraphGraphType.DIGRAPH, result.graphType)
        assertEquals(3, result.nodes.size)
        assertEquals(3, result.edges.size)
        
        // Check nodes
        val nodeNames = result.nodes.map { it.getName() }.toSet()
        assertTrue(nodeNames.contains("A"))
        assertTrue(nodeNames.contains("B"))
        assertTrue(nodeNames.contains("C"))
        
        // Check edges
        val edges = result.edges.toList()
        assertTrue(edges.any { it.sourceNodeId == "A" && it.targetNodeId == "B" })
        assertTrue(edges.any { it.sourceNodeId == "B" && it.targetNodeId == "C" })
        assertTrue(edges.any { it.sourceNodeId == "A" && it.targetNodeId == "C" })
        
        // All edges should be directed
        assertTrue(edges.all { it.edgeType == GraphvizEdgeType.DIRECTED })
    }
    
    @Test
    fun `should parse graph with node attributes`() {
        val dotContent = """
            digraph G {
                A [label="Node A", shape=box, color=red];
                B [label="Node B", shape=circle];
                A -> B;
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(2, result.nodes.size)
        assertEquals(1, result.edges.size)
        
        val nodeA = result.getNodeById("A")
        assertNotNull(nodeA)
        assertEquals("Node A", nodeA?.getAttribute("label"))
        assertEquals("box", nodeA?.getAttribute("shape"))
        assertEquals("red", nodeA?.getAttribute("color"))
        
        val nodeB = result.getNodeById("B")
        assertNotNull(nodeB)
        assertEquals("Node B", nodeB?.getAttribute("label"))
        assertEquals("circle", nodeB?.getAttribute("shape"))
    }
    
    @Test
    fun `should parse graph with edge attributes`() {
        val dotContent = """
            digraph G {
                A -> B [label="edge label", color=blue, style=dashed];
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(2, result.nodes.size)
        assertEquals(1, result.edges.size)
        
        val edge = result.edges.first()
        assertEquals("A", edge.sourceNodeId)
        assertEquals("B", edge.targetNodeId)
        assertEquals("edge label", edge.label)
        assertEquals("blue", edge.getColor())
        assertEquals("dashed", edge.getStyle())
    }
    
    @Test
    fun `should handle undirected graph`() {
        val dotContent = """
            graph G {
                A -- B;
                B -- C;
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(GraphGraphType.GRAPH, result.graphType)
        assertEquals(3, result.nodes.size)
        assertEquals(2, result.edges.size)
        
        // All edges should be undirected
        assertTrue(result.edges.all { it.edgeType == GraphvizEdgeType.UNDIRECTED })
    }
    
    @Test
    fun `should handle empty graph`() {
        val dotContent = """
            digraph G {
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(GraphGraphType.DIGRAPH, result.graphType)
        assertEquals(0, result.nodes.size)
        assertEquals(0, result.edges.size)
    }
    
    @Test
    fun `should handle invalid DOT content gracefully`() {
        val invalidContent = "this is not valid DOT content"

        val result = parser.parse(invalidContent)

        // Should return empty data instead of throwing exception
        assertEquals(0, result.nodes.size)
        assertEquals(0, result.edges.size)
    }

    @Test
    fun `should parse record nodes with fields`() {
        val dotContent = """
            digraph ClassDiagram {
                User [shape=record, label="{User|id:Long|name:String|email:String}"];
                Order [shape=record, label="{Order|id:Long|userId:Long|amount:Double}"];
                User -> Order;
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(GraphGraphType.DIGRAPH, result.graphType)
        assertEquals(0, result.nodes.size) // Should be entities, not simple nodes
        assertEquals(2, result.entities.size)
        assertEquals(1, result.edges.size)

        val userEntity = result.entities.find { it.getName() == "User" }
        assertNotNull(userEntity)
        assertEquals(3, userEntity!!.getFields().size)

        val fields = userEntity.getFields()
        assertEquals("id", fields[0].name)
        assertEquals("Long", fields[0].type)
        assertEquals("name", fields[1].name)
        assertEquals("String", fields[1].type)
        assertEquals("email", fields[2].name)
        assertEquals("String", fields[2].type)
    }

    @Test
    fun `should parse mixed nodes and entities`() {
        val dotContent = """
            digraph Mixed {
                simpleNode [label="Simple"];
                User [shape=record, label="{User|name:String|age:Int}"];
                simpleNode -> User;
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(1, result.nodes.size)
        assertEquals(1, result.entities.size)
        assertEquals(1, result.edges.size)

        val simpleNode = result.nodes.first()
        assertEquals("simpleNode", simpleNode.getName())
        assertEquals(GraphvizNodeType.REGULAR, simpleNode.getNodeType())

        val userEntity = result.entities.first()
        assertEquals("User", userEntity.getName())
        assertEquals(2, userEntity.getFields().size)
    }

    @Test
    fun `should handle empty record labels`() {
        val dotContent = """
            digraph Empty {
                EmptyRecord [shape=record, label="{}"];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(1, result.nodes.size) // Should fallback to simple node
        assertEquals(0, result.entities.size)
    }

    @Test
    fun `should parse fields with ports`() {
        val dotContent = """
            digraph Ports {
                Node [shape=record, label="{<port1>field1:String|<port2>field2:Int}"];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(1, result.entities.size)
        val entity = result.entities.first()
        assertEquals(2, entity.getFields().size)

        val field1 = entity.getFields()[0]
        assertEquals("field1", field1.name)
        assertEquals("String", field1.type)

        val field2 = entity.getFields()[1]
        assertEquals("field2", field2.name)
        assertEquals("Int", field2.type)
    }

    @Test
    fun `should parse complex class diagram with methods`() {
        val dotContent = """
            digraph ClassDiagram {
                User [shape=record, label="{User|id:Long|name:String|+getName():String|+setName(name:String):void}"];
                Order [shape=record, label="{Order|id:Long|amount:Double|+getTotal():Double}"];
                User -> Order [label="has many"];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(2, result.entities.size)
        assertEquals(1, result.edges.size)

        val userEntity = result.entities.find { it.getName() == "User" }
        assertNotNull(userEntity)
        assertEquals(5, userEntity!!.getFields().size) // 2 fields + 3 methods

        val fields = userEntity.getFields()
        assertEquals("id", fields[0].name)
        assertEquals("Long", fields[0].type)
        assertEquals("name", fields[1].name)
        assertEquals("String", fields[1].type)
        assertEquals("+getName()", fields[2].name)
        assertEquals("String", fields[2].type)
    }

    @Test
    fun `should parse subgraphs and clusters`() {
        val dotContent = """
            digraph G {
                subgraph cluster_agents {
                    label="代理定义";
                    TravelAgent [label="旅行代理"];
                    FlightAgent [label="航班代理"];
                }

                subgraph cluster_parallel {
                    label="1. 并行查询";
                    TravelAgent -> FlightAgent [label="findFlights()"];
                }

                PaymentAgent -> TravelAgent;
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(GraphGraphType.DIGRAPH, result.graphType)
        assertEquals(3, result.nodes.size) // TravelAgent, FlightAgent, PaymentAgent
        assertEquals(2, result.subgraphs.size)
        assertEquals(2, result.edges.size) // 1 in subgraph + 1 main edge

        // Check subgraphs
        val agentsCluster = result.subgraphs.find { it.name == "cluster_agents" }
        assertNotNull(agentsCluster)
        assertTrue(agentsCluster!!.isCluster)
        assertEquals("代理定义", agentsCluster.getDisplayLabel())
        assertEquals(2, agentsCluster.nodes.size)
        assertTrue(agentsCluster.containsNode("TravelAgent"))
        assertTrue(agentsCluster.containsNode("FlightAgent"))

        val parallelCluster = result.subgraphs.find { it.name == "cluster_parallel" }
        assertNotNull(parallelCluster)
        assertTrue(parallelCluster!!.isCluster)
        assertEquals("1. 并行查询", parallelCluster.getDisplayLabel())
        assertEquals(1, parallelCluster.edges.size)
    }

    @Test
    fun `should parse HTML labels correctly`() {
        val dotContent = """
            digraph G {
                TravelAgent [label=<
                    <b>旅行代理</b><br/>协调预订航班、酒店和支付
                >];
                FlightAgent [label=<
                    <b>航班代理</b><br/>技能: findFlights(query: object): flightOptions
                >];
                TravelAgent -> FlightAgent;
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(2, result.nodes.size)
        assertEquals(1, result.edges.size)

        val travelAgent = result.getNodeById("TravelAgent")
        assertNotNull(travelAgent)
        val displayLabel = travelAgent!!.getDisplayLabel()
        assertTrue(displayLabel.contains("旅行代理"))
        assertTrue(displayLabel.contains("协调预订航班、酒店和支付"))

        val flightAgent = result.getNodeById("FlightAgent")
        assertNotNull(flightAgent)
        val flightLabel = flightAgent!!.getDisplayLabel()
        assertTrue(flightLabel.contains("航班代理"))
        assertTrue(flightLabel.contains("findFlights"))
    }

    @Test
    fun `should handle complex nested subgraphs`() {
        val dotContent = """
            digraph G {
                rankdir=TB;
                node [shape=box, style=rounded];

                subgraph cluster_agents {
                    label="代理定义";
                    TravelAgent [label="旅行代理"];
                    FlightAgent [label="航班代理"];

                    subgraph cluster_inner {
                        label="内部代理";
                        InnerAgent [label="内部代理"];
                    }
                }

                PaymentAgent -> TravelAgent [label="processPayment()", color=red, style=dashed];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(GraphGraphType.DIGRAPH, result.graphType)
        assertTrue(result.subgraphs.size >= 2) // At least outer and inner subgraphs
        assertEquals(1, result.edges.size) // Main edge

        // Check main cluster
        val agentsCluster = result.subgraphs.find { it.name == "cluster_agents" }
        assertNotNull(agentsCluster)
        assertTrue(agentsCluster!!.isCluster)
        assertEquals("代理定义", agentsCluster.getDisplayLabel())

        // Check nested cluster
        val innerCluster = result.subgraphs.find { it.name == "cluster_inner" }
        assertNotNull(innerCluster)
        assertTrue(innerCluster!!.isCluster)
        assertEquals("内部代理", innerCluster.getDisplayLabel())

        // Check edge attributes
        val edge = result.edges.first()
        assertEquals("PaymentAgent", edge.sourceNodeId)
        assertEquals("TravelAgent", edge.targetNodeId)
        assertEquals("processPayment()", edge.label)
        assertEquals("red", edge.getColor())
        assertEquals("dashed", edge.getStyle())
    }

    @Test
    fun `should parse the provided travel agent example correctly`() {
        val dotContent = """
            digraph G {
                rankdir=TB;
                node [shape=box, style=rounded];

                subgraph cluster_agents {
                    label="代理定义";
                    TravelAgent [label=<
                        <b>旅行代理</b><br/>协调预订航班、酒店和支付
                    >];
                    FlightAgent [label=<
                        <b>航班代理</b><br/>技能: findFlights(query: object): flightOptions
                    >];
                    HotelAgent [label=<
                        <b>酒店代理</b><br/>技能: findHotels(query: object): hotelOptions
                    >];
                    PaymentAgent [label=<
                        <b>支付代理</b><br/>技能: processPayment(amount: float): transactionID
                    >];
                    RefundHandler [label=<
                        <b>退款处理代理</b><br/>技能: initiateRefund(transactionID: string): void
                    >];
                }

                subgraph cluster_parallel {
                    label="1. 并行查询";
                    TravelAgent -> FlightAgent [label="findFlights()"];
                    TravelAgent -> HotelAgent [label="findHotels()"];
                }

                subgraph cluster_conditional {
                    label="2. 条件决策与支付";
                    TravelAgent -> PaymentAgent [label="processPayment()"];
                    TravelAgent -> User [label="无法满足所有预订要求"];
                }

                PaymentAgent -> RefundHandler [label="initiateRefund()", color=red, style=dashed];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        // Verify basic structure
        assertEquals(GraphGraphType.DIGRAPH, result.graphType)
        assertTrue(result.nodes.size >= 6) // At least 6 nodes
        assertEquals(3, result.subgraphs.size) // 3 subgraphs
        assertTrue(result.edges.size >= 4) // At least 4 edges

        // Verify subgraphs
        val agentsCluster = result.subgraphs.find { it.name == "cluster_agents" }
        assertNotNull(agentsCluster)
        assertTrue(agentsCluster!!.isCluster)
        assertEquals("代理定义", agentsCluster.getDisplayLabel())
        assertEquals(5, agentsCluster.nodes.size)
        assertTrue(agentsCluster.containsNode("TravelAgent"))
        assertTrue(agentsCluster.containsNode("FlightAgent"))
        assertTrue(agentsCluster.containsNode("HotelAgent"))
        assertTrue(agentsCluster.containsNode("PaymentAgent"))
        assertTrue(agentsCluster.containsNode("RefundHandler"))

        val parallelCluster = result.subgraphs.find { it.name == "cluster_parallel" }
        assertNotNull(parallelCluster)
        assertTrue(parallelCluster!!.isCluster)
        assertEquals("1. 并行查询", parallelCluster.getDisplayLabel())
        assertEquals(2, parallelCluster.edges.size)

        val conditionalCluster = result.subgraphs.find { it.name == "cluster_conditional" }
        assertNotNull(conditionalCluster)
        assertTrue(conditionalCluster!!.isCluster)
        assertEquals("2. 条件决策与支付", conditionalCluster.getDisplayLabel())
        assertEquals(2, conditionalCluster.edges.size)

        // Verify HTML label parsing
        val travelAgent = result.getNodeById("TravelAgent")
        assertNotNull(travelAgent)
        val displayLabel = travelAgent!!.getDisplayLabel()
        assertTrue(displayLabel.contains("旅行代理"))
        assertTrue(displayLabel.contains("协调预订航班、酒店和支付"))

        val flightAgent = result.getNodeById("FlightAgent")
        assertNotNull(flightAgent)
        val flightLabel = flightAgent!!.getDisplayLabel()
        assertTrue(flightLabel.contains("航班代理"))
        assertTrue(flightLabel.contains("findFlights"))

        // Verify main graph edge
        val mainEdge = result.edges.find { it.sourceNodeId == "PaymentAgent" && it.targetNodeId == "RefundHandler" }
        assertNotNull(mainEdge)
        assertEquals("initiateRefund()", mainEdge!!.label)
        assertEquals("red", mainEdge.getColor())
        assertEquals("dashed", mainEdge.getStyle())
    }

    @Test
    fun `should parse HTML labels with proper newlines`() {
        val dotContent = """
            digraph G {
                PaymentAgent [label=<
                    <b>支付代理</b><br/>技能: processPayment(amount: float): transactionID
                >];
                TravelAgent [label=<
                    <b>旅行代理</b><br/>协调预订航班、酒店和支付
                >];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(2, result.nodes.size)

        val paymentAgent = result.getNodeById("PaymentAgent")
        assertNotNull(paymentAgent)
        val paymentLabel = paymentAgent!!.getDisplayLabel()
        assertTrue(paymentLabel.contains("支付代理"))
        assertTrue(paymentLabel.contains("processPayment"))
        assertTrue(paymentLabel.contains("\n"), "Should contain actual newline character")

        val travelAgent = result.getNodeById("TravelAgent")
        assertNotNull(travelAgent)
        val travelLabel = travelAgent!!.getDisplayLabel()
        assertTrue(travelLabel.contains("旅行代理"))
        assertTrue(travelLabel.contains("协调预订"))
        assertTrue(travelLabel.contains("\n"), "Should contain actual newline character")
    }

    @Test
    fun `debug simple subgraph and HTML label parsing`() {
        val dotContent = """
digraph G {
    rankdir=TB;
    node [shape=box, style=rounded];

    subgraph cluster_agents {
        label="代理定义";
        TravelAgent [label=<
            <b>旅行代理</b><br/>协调预订航班、酒店和支付
        >];
        FlightAgent [label=<
            <b>航班代理</b><br/>技能: findFlights(query: object): flightOptions
        >];
    }

    subgraph cluster_parallel {
        label="1. 并行查询";
        TravelAgent -> FlightAgent [label="findFlights()"];
    }

    PaymentAgent -> TravelAgent [label="processPayment()"];
}
        """.trimIndent()

        println("=== 开始解析 ===")
        val result = parser.parse(dotContent)

        // Debug output
        println("=== 解析结果调试 ===")
        println("节点数量: ${result.nodes.size}")
        println("实体数量: ${result.entities.size}")
        println("边数量: ${result.edges.size}")
        println("子图数量: ${result.subgraphs.size}")
        println("图类型: ${result.graphType}")

        println("\n--- 所有节点 ---")
        result.nodes.forEach { node ->
            println("节点: ${node.getName()}")
            println("  显示标签: '${node.getDisplayLabel()}'")
            println("  类型: ${node.getNodeType()}")
            println("  属性: ${node.getAttributes()}")
        }

        println("\n--- 所有子图 ---")
        result.subgraphs.forEach { subgraph ->
            println("子图: ${subgraph.name}")
            println("  显示标签: '${subgraph.getDisplayLabel()}'")
            println("  是否为 Cluster: ${subgraph.isCluster}")
            println("  包含节点: ${subgraph.nodes}")
            println("  边数量: ${subgraph.edges.size}")
            println("  属性: ${subgraph.attributes}")
        }

        println("\n--- 所有边 ---")
        result.edges.forEach { edge ->
            println("边: ${edge.sourceNodeId} -> ${edge.targetNodeId}")
            println("  标签: '${edge.label}'")
        }

        // Basic assertions
        assertTrue(result.nodes.isNotEmpty(), "Should have nodes")
        assertTrue(result.subgraphs.isNotEmpty(), "Should have subgraphs")

        // Test HTML label parsing specifically
        val travelAgent = result.nodes.find { it.getName() == "TravelAgent" }
        assertNotNull(travelAgent, "Should have TravelAgent node")
        println("\n=== HTML Label 测试 ===")
        println("TravelAgent 显示标签: '${travelAgent!!.getDisplayLabel()}'")

        // Test subgraph parsing specifically
        val clusterAgents = result.subgraphs.find { it.name == "cluster_agents" }
        assertNotNull(clusterAgents, "Should have cluster_agents subgraph")
        println("\n=== Subgraph 测试 ===")
        println("cluster_agents 是否为 cluster: ${clusterAgents!!.isCluster}")
        println("cluster_agents 标签: '${clusterAgents.getDisplayLabel()}'")
    }
}
