package cc.unitmesh.diagram

import cc.unitmesh.diagram.diagram.CodeTopologyNodeCategoryManager
import cc.unitmesh.diagram.diff.DiagramDiffUtils
import cc.unitmesh.diagram.parser.MermaidClassDiagramParser
import cc.unitmesh.diagram.model.ChangeStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class DebugDiffTest {
    
    private val parser = MermaidClassDiagramParser()
    
    @Test
    fun `debug parse and categorize simple mermaid diagram`() {
        val mermaidContent = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +save()
                    +delete()
                }
        """.trimIndent()
        
        val result = parser.parse(mermaidContent)
        
        // Assert parsed result structure
        assertEquals(1, result.entities.size, "Should parse exactly 1 entity")

        val entity = result.entities.first()
        assertNotNull(entity.getName(), "Entity name should not be null")
        assertEquals("User", entity.getName(), "Entity name should be User")

        val fields = entity.getFields()
        assertEquals(4, fields.size, "Should have 4 fields (id, name, save, delete)")

        // Verify specific fields exist (using contains since field names may include type info)
        assertTrue(fields.any { it.name.contains("id") }, "Should contain field with 'id'")
        assertTrue(fields.any { it.name.contains("name") }, "Should contain field with 'name'")
        assertTrue(fields.any { it.name.contains("save") }, "Should contain field with 'save'")
        assertTrue(fields.any { it.name.contains("delete") }, "Should contain field with 'delete'")

        // Verify field properties
        for (field in fields) {
            assertNotNull(field.name, "Field name should not be null")
            assertNotNull(field.changeStatus, "Field changeStatus should not be null")
            assertEquals(ChangeStatus.ADDED, field.changeStatus, "Parsed fields should have ADDED status")
            assertNotNull(field.getDisplayName(), "Field displayName should not be null")
        }
        
        // Test categorization
        val categoryManager = CodeTopologyNodeCategoryManager()
        val categories = categoryManager.getContentCategories()
        
        assertTrue(categories.isNotEmpty(), "Should have at least one category")

        // Verify categorization works for at least one field
        val field = fields.first()
        var foundCategory = false
        for (category in categories) {
            val isInCategory = categoryManager.isInCategory(entity, field, category, null)
            if (isInCategory) {
                foundCategory = true
                break
            }
        }
        assertTrue(foundCategory, "At least one field should be categorized")
    }
    
    @Test
    fun `debug diff analysis`() {
        val oldMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +save()
                }
        """.trimIndent()
        
        val newMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +String email
                    +save()
                    +delete()
                }
        """.trimIndent()
        
        val diffResult = DiagramDiffUtils.compareMermaidDiagrams(oldMermaid, newMermaid)
        
        // Assert diff result structure
        assertEquals(1, diffResult.entities.size, "Should have exactly 1 entity in diff result")

        val entity = diffResult.entities.first()
        assertEquals("User", entity.getName(), "Entity name should be User")

        val fields = entity.getFields()
        assertTrue(fields.size >= 4, "Should have at least 4 fields after diff")

        // Verify specific field changes (using contains since field names may include type info)
        val emailField = fields.find { it.name.contains("email") }
        assertNotNull(emailField, "Should contain field with 'email'")
        assertEquals(ChangeStatus.ADDED, emailField.changeStatus, "Email field should be marked as ADDED")

        val deleteField = fields.find { it.name.contains("delete") }
        assertNotNull(deleteField, "Should contain field with 'delete'")
        assertEquals(ChangeStatus.ADDED, deleteField.changeStatus, "Delete field should be marked as ADDED")

        // Verify unchanged fields
        val idField = fields.find { it.name.contains("id") }
        assertNotNull(idField, "Should contain field with 'id'")
        assertEquals(ChangeStatus.UNCHANGED, idField.changeStatus, "ID field should be UNCHANGED")

        val nameField = fields.find { it.name.contains("name") }
        assertNotNull(nameField, "Should contain field with 'name'")
        assertEquals(ChangeStatus.UNCHANGED, nameField.changeStatus, "Name field should be UNCHANGED")

        val saveField = fields.find { it.name.contains("save") }
        assertNotNull(saveField, "Should contain field with 'save'")
        assertEquals(ChangeStatus.UNCHANGED, saveField.changeStatus, "Save field should be UNCHANGED")

        // Verify field properties
        for (field in fields) {
            assertNotNull(field.name, "Field name should not be null")
            assertNotNull(field.changeStatus, "Field changeStatus should not be null")
            assertNotNull(field.getDisplayName(), "Field displayName should not be null")
        }
        
        // Test categorization of diff result
        val categoryManager = CodeTopologyNodeCategoryManager()
        val categories = categoryManager.getContentCategories()
        
        assertTrue(categories.isNotEmpty(), "Should have at least one category")

        // Verify categorization works for changed fields
        val addedFields = fields.filter { it.changeStatus == ChangeStatus.ADDED }
        assertTrue(addedFields.isNotEmpty(), "Should have at least one added field")

        for (field in addedFields) {
            var foundCategory = false
            for (category in categories) {
                val isInCategory = categoryManager.isInCategory(entity, field, category, null)
                if (isInCategory) {
                    foundCategory = true
                    break
                }
            }
            assertTrue(foundCategory, "Added field ${field.name} should be categorized")
        }
        
        // Generate and verify summary
        val summary = DiagramDiffUtils.generateChangeSummary(diffResult)
        assertNotNull(summary, "Summary should not be null")
        assertEquals(0, summary.addedEntities, "Should have 0 added entities")
        assertEquals(0, summary.removedEntities, "Should have 0 removed entities")
        assertTrue(summary.addedFields > 0, "Should have added fields")
        assertEquals(0, summary.removedFields, "Should have 0 removed fields")
        assertTrue(summary.addedMethods > 0, "Should have added methods")
        assertEquals(0, summary.removedMethods, "Should have 0 removed methods")
        assertTrue(summary.hasChanges(), "Summary should indicate changes exist")

        // Generate and verify report
        val report = DiagramDiffUtils.generateChangeReport(diffResult)
        assertNotNull(report, "Report should not be null")
        assertTrue(report.isNotEmpty(), "Report should not be empty")
    }
}
