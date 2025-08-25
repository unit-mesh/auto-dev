package cc.unitmesh.diagram.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagramDiffUtilsTest {
    
    @Test
    fun `should compare mermaid diagrams and detect changes`() {
        val oldMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +String email
                }
        """.trimIndent()
        
        val newMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +String phone
                    +save()
                }
                class Product {
                    +String name
                    +Double price
                }
        """.trimIndent()
        
        val result = DiagramDiffUtils.compareMermaidDiagrams(oldMermaid, newMermaid)
        
        // Should have 2 entities
        assertEquals(2, result.entities.size)
        
        val userEntity = result.entities.find { it.getName() == "User" }!!
        val productEntity = result.entities.find { it.getName() == "Product" }!!
        
        // Check User entity changes
        val userFields = userEntity.getFields()
        assertTrue(userFields.any { it.name.contains("phone") && it.isAdded() })
        assertTrue(userFields.any { it.name.contains("save") && it.isAdded() && it.isMethod() })
        assertTrue(userFields.any { it.name.contains("email") && it.isRemoved() })
        
        // Check Product entity (should be all added)
        val productFields = productEntity.getFields()
        assertTrue(productFields.all { it.isAdded() })
    }
    
    @Test
    fun `should generate change summary correctly`() {
        val oldMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                }
        """.trimIndent()
        
        val newMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +String email
                    +save()
                }
                class Product {
                    +String name
                }
        """.trimIndent()
        
        val result = DiagramDiffUtils.compareMermaidDiagrams(oldMermaid, newMermaid)
        val summary = DiagramDiffUtils.generateChangeSummary(result)
        
        assertEquals(1, summary.addedEntities) // Product
        assertEquals(0, summary.removedEntities)
        assertEquals(1, summary.addedFields) // User.email
        assertEquals(0, summary.removedFields)
        assertEquals(1, summary.addedMethods) // User.save()
        assertEquals(0, summary.removedMethods)
        
        assertTrue(summary.hasChanges())
        assertEquals(3, summary.totalChanges())
    }
    
    @Test
    fun `should generate change report`() {
        val oldMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +String email
                }
        """.trimIndent()
        
        val newMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +String phone
                }
        """.trimIndent()
        
        val result = DiagramDiffUtils.compareMermaidDiagrams(oldMermaid, newMermaid)
        val report = DiagramDiffUtils.generateChangeReport(result)
        
        assertTrue(report.contains("Code Structure Changes"))
        assertTrue(report.contains("Added 1 field(s)"))
    }
    
    @Test
    fun `should handle null old diagram`() {
        val newMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                }
        """.trimIndent()
        
        val result = DiagramDiffUtils.compareMermaidDiagrams(null, newMermaid)
        val summary = DiagramDiffUtils.generateChangeSummary(result)
        
        assertEquals(1, summary.addedEntities)
        assertEquals(0, summary.removedEntities)
        assertTrue(summary.hasChanges())
    }
    
    @Test
    fun `should detect no changes when diagrams are identical`() {
        val mermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                }
        """.trimIndent()
        
        val result = DiagramDiffUtils.compareMermaidDiagrams(mermaid, mermaid)
        val summary = DiagramDiffUtils.generateChangeSummary(result)
        
        assertEquals(0, summary.addedEntities)
        assertEquals(0, summary.removedEntities)
        assertEquals(0, summary.addedFields)
        assertEquals(0, summary.removedFields)
        assertEquals(0, summary.addedMethods)
        assertEquals(0, summary.removedMethods)
        
        assertTrue(!summary.hasChanges())
        assertEquals(0, summary.totalChanges())
        
        val report = DiagramDiffUtils.generateChangeReport(result)
        assertTrue(report.contains("No structural changes detected"))
    }
}
