package cc.unitmesh.diagram.diff

import cc.unitmesh.diagram.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagramDiffAnalyzerTest {
    
    private val analyzer = DiagramDiffAnalyzer()
    
    @Test
    fun `should mark all fields as added when old diagram is null`() {
        val newDiagram = createSampleDiagram()
        
        val result = analyzer.analyzeDiff(null, newDiagram)
        
        val entity = result.entities.first()
        assertTrue(entity.getFields().all { it.changeStatus == ChangeStatus.ADDED })
    }
    
    @Test
    fun `should detect added fields`() {
        val oldDiagram = GraphvizDiagramData(
            nodes = emptyList(),
            entities = listOf(
                GraphvizEntityNodeData("User", listOf(
                    GraphvizNodeField("id", "String", false, ChangeStatus.UNCHANGED, isMethodField = false),
                    GraphvizNodeField("name", "String", false, ChangeStatus.UNCHANGED, isMethodField = false)
                ))
            ),
            edges = emptyList()
        )
        
        val newDiagram = GraphvizDiagramData(
            nodes = emptyList(),
            entities = listOf(
                GraphvizEntityNodeData("User", listOf(
                    GraphvizNodeField("id", "String", false, ChangeStatus.UNCHANGED, isMethodField = false),
                    GraphvizNodeField("name", "String", false, ChangeStatus.UNCHANGED, isMethodField = false),
                    GraphvizNodeField("email", "String", false, ChangeStatus.UNCHANGED, isMethodField = false), // New field
                    GraphvizNodeField("save()", "void", false, ChangeStatus.UNCHANGED, isMethodField = true) // New method
                ))
            ),
            edges = emptyList()
        )
        
        val result = analyzer.analyzeDiff(oldDiagram, newDiagram)
        
        val entity = result.entities.first()
        val fields = entity.getFields()
        
        // Check that existing fields are unchanged
        assertEquals(ChangeStatus.UNCHANGED, fields.find { it.name == "id" }?.changeStatus)
        assertEquals(ChangeStatus.UNCHANGED, fields.find { it.name == "name" }?.changeStatus)
        
        // Check that new fields are marked as added
        assertEquals(ChangeStatus.ADDED, fields.find { it.name == "email" }?.changeStatus)
        assertEquals(ChangeStatus.ADDED, fields.find { it.name == "save()" }?.changeStatus)
    }
    
    @Test
    fun `should detect removed fields`() {
        val oldDiagram = GraphvizDiagramData(
            nodes = emptyList(),
            entities = listOf(
                GraphvizEntityNodeData("User", listOf(
                    GraphvizNodeField("id", "String", false, ChangeStatus.UNCHANGED, isMethodField = false),
                    GraphvizNodeField("name", "String", false, ChangeStatus.UNCHANGED, isMethodField = false),
                    GraphvizNodeField("email", "String", false, ChangeStatus.UNCHANGED, isMethodField = false),
                    GraphvizNodeField("delete()", "void", false, ChangeStatus.UNCHANGED, isMethodField = true)
                ))
            ),
            edges = emptyList()
        )
        
        val newDiagram = GraphvizDiagramData(
            nodes = emptyList(),
            entities = listOf(
                GraphvizEntityNodeData("User", listOf(
                    GraphvizNodeField("id", "String", false, ChangeStatus.UNCHANGED, isMethodField = false),
                    GraphvizNodeField("name", "String", false, ChangeStatus.UNCHANGED, isMethodField = false)
                ))
            ),
            edges = emptyList()
        )
        
        val result = analyzer.analyzeDiff(oldDiagram, newDiagram)
        
        val entity = result.entities.first()
        val fields = entity.getFields()
        
        // Check that remaining fields are unchanged
        assertEquals(ChangeStatus.UNCHANGED, fields.find { it.name == "id" }?.changeStatus)
        assertEquals(ChangeStatus.UNCHANGED, fields.find { it.name == "name" }?.changeStatus)
        
        // Check that removed fields are marked as removed
        assertEquals(ChangeStatus.REMOVED, fields.find { it.name == "email" }?.changeStatus)
        assertEquals(ChangeStatus.REMOVED, fields.find { it.name == "delete()" }?.changeStatus)
    }
    
    @Test
    fun `should detect new entities`() {
        val oldDiagram = GraphvizDiagramData(
            nodes = emptyList(),
            entities = listOf(
                GraphvizEntityNodeData("User", listOf(
                    GraphvizNodeField("id", "String", false, ChangeStatus.UNCHANGED, isMethodField = false)
                ))
            ),
            edges = emptyList()
        )
        
        val newDiagram = GraphvizDiagramData(
            nodes = emptyList(),
            entities = listOf(
                GraphvizEntityNodeData("User", listOf(
                    GraphvizNodeField("id", "String", false, ChangeStatus.UNCHANGED, isMethodField = false)
                )),
                GraphvizEntityNodeData("Product", listOf(
                    GraphvizNodeField("name", "String", false, ChangeStatus.UNCHANGED, isMethodField = false)
                ))
            ),
            edges = emptyList()
        )
        
        val result = analyzer.analyzeDiff(oldDiagram, newDiagram)
        
        assertEquals(2, result.entities.size)
        
        val userEntity = result.entities.find { it.getName() == "User" }!!
        val productEntity = result.entities.find { it.getName() == "Product" }!!
        
        // User entity field should be unchanged
        assertEquals(ChangeStatus.UNCHANGED, userEntity.getFields().first().changeStatus)
        
        // Product entity field should be added (new entity)
        assertEquals(ChangeStatus.ADDED, productEntity.getFields().first().changeStatus)
    }
    
    private fun createSampleDiagram(): GraphvizDiagramData {
        return GraphvizDiagramData(
            nodes = emptyList(),
            entities = listOf(
                GraphvizEntityNodeData("User", listOf(
                    GraphvizNodeField("id", "String", false, ChangeStatus.UNCHANGED, isMethodField = false),
                    GraphvizNodeField("name", "String", false, ChangeStatus.UNCHANGED, isMethodField = false)
                ))
            ),
            edges = emptyList()
        )
    }
}
