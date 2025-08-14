package cc.unitmesh.diagram

import cc.unitmesh.diagram.diagram.GraphvizNodeCategoryManager
import cc.unitmesh.diagram.diff.DiagramDiffUtils
import cc.unitmesh.diagram.model.ChangeStatus
import cc.unitmesh.diagram.parser.MermaidClassDiagramParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationDiffTest {
    
    private val parser = MermaidClassDiagramParser()
    private val categoryManager = GraphvizNodeCategoryManager()
    
    @Test
    fun `should parse and categorize mermaid diagram with changes`() {
        val oldMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +String email
                    +save()
                }
        """.trimIndent()
        
        val newMermaid = """
            classDiagram
                class User {
                    +String id
                    +String name
                    +String phone
                    +save()
                    +delete()
                }
                class Product {
                    +String name
                    +Double price
                }
        """.trimIndent()
        
        // Parse and analyze differences
        val diffResult = DiagramDiffUtils.compareMermaidDiagrams(oldMermaid, newMermaid)
        
        // Verify we have the expected entities
        assertEquals(2, diffResult.entities.size)
        
        val userEntity = diffResult.entities.find { it.getName() == "User" }!!
        val productEntity = diffResult.entities.find { it.getName() == "Product" }!!
        
        // Test User entity fields
        val userFields = userEntity.getFields()
        
        // Find specific fields
        val idField = userFields.find { it.name.contains("id") }!!
        val nameField = userFields.find { it.name.contains("name") && !it.name.contains("phone") }!!
        val emailField = userFields.find { it.name.contains("email") }!!
        val phoneField = userFields.find { it.name.contains("phone") }!!
        val saveMethod = userFields.find { it.name.contains("save") }!!
        val deleteMethod = userFields.find { it.name.contains("delete") }!!
        
        // Verify change statuses
        assertEquals(ChangeStatus.UNCHANGED, idField.changeStatus)
        assertEquals(ChangeStatus.UNCHANGED, nameField.changeStatus)
        assertEquals(ChangeStatus.REMOVED, emailField.changeStatus)
        assertEquals(ChangeStatus.ADDED, phoneField.changeStatus)
        assertEquals(ChangeStatus.UNCHANGED, saveMethod.changeStatus)
        assertEquals(ChangeStatus.ADDED, deleteMethod.changeStatus)
        
        // Verify method flags
        assertTrue(saveMethod.isMethod())
        assertTrue(deleteMethod.isMethod())
        assertTrue(!phoneField.isMethod())
        
        // Test Product entity (should be all added)
        val productFields = productEntity.getFields()
        assertTrue(productFields.all { it.changeStatus == ChangeStatus.ADDED })
        
        // Test categorization
        val categories = categoryManager.getContentCategories()
        
        // Test unchanged field categorization
        val fieldsCategory = categories.find { it.name == "Fields" }!!
        assertTrue(categoryManager.isInCategory(userEntity, idField, fieldsCategory, null))
        assertTrue(categoryManager.isInCategory(userEntity, nameField, fieldsCategory, null))
        
        // Test unchanged method categorization
        val methodsCategory = categories.find { it.name == "Methods" }!!
        assertTrue(categoryManager.isInCategory(userEntity, saveMethod, methodsCategory, null))
        
        // Test added field categorization
        val addedFieldsCategory = categories.find { it.name == "Added Fields" }!!
        assertTrue(categoryManager.isInCategory(userEntity, phoneField, addedFieldsCategory, null))
        
        // Test added method categorization
        val addedMethodsCategory = categories.find { it.name == "Added Methods" }!!
        assertTrue(categoryManager.isInCategory(userEntity, deleteMethod, addedMethodsCategory, null))
        
        // Test removed field categorization
        val removedFieldsCategory = categories.find { it.name == "Removed Fields" }!!
        assertTrue(categoryManager.isInCategory(userEntity, emailField, removedFieldsCategory, null))
    }
    
    @Test
    fun `should generate correct display names with change prefixes`() {
        val addedField = cc.unitmesh.diagram.model.GraphvizNodeField(
            name = "email",
            type = "String",
            changeStatus = ChangeStatus.ADDED,
            isMethodField = false
        )
        
        val removedMethod = cc.unitmesh.diagram.model.GraphvizNodeField(
            name = "delete()",
            type = "void",
            changeStatus = ChangeStatus.REMOVED,
            isMethodField = true
        )
        
        val unchangedField = cc.unitmesh.diagram.model.GraphvizNodeField(
            name = "id",
            type = "String",
            changeStatus = ChangeStatus.UNCHANGED,
            isMethodField = false
        )
        
        // Test display names
        assertEquals("+ email", addedField.getDisplayName())
        assertEquals("- delete()", removedMethod.getDisplayName())
        assertEquals("id", unchangedField.getDisplayName())
        
        // Test toString with prefixes
        assertTrue(addedField.toString().startsWith("+ "))
        assertTrue(removedMethod.toString().startsWith("- "))
        assertTrue(!unchangedField.toString().startsWith("+") && !unchangedField.toString().startsWith("-"))
    }
    
    @Test
    fun `should handle empty old diagram correctly`() {
        val newMermaid = """
            classDiagram
                class User {
                    +String id
                    +save()
                }
        """.trimIndent()
        
        val diffResult = DiagramDiffUtils.compareMermaidDiagrams(null, newMermaid)
        
        val userEntity = diffResult.entities.first()
        val fields = userEntity.getFields()
        
        // All fields should be marked as added
        assertTrue(fields.all { it.changeStatus == ChangeStatus.ADDED })
        
        // Test categorization for new entity
        val addedFieldsCategory = categoryManager.getContentCategories().find { it.name == "Added Fields" }!!
        val addedMethodsCategory = categoryManager.getContentCategories().find { it.name == "Added Methods" }!!
        
        val idField = fields.find { it.name.contains("id") }!!
        val saveMethod = fields.find { it.name.contains("save") }!!
        
        assertTrue(categoryManager.isInCategory(userEntity, idField, addedFieldsCategory, null))
        assertTrue(categoryManager.isInCategory(userEntity, saveMethod, addedMethodsCategory, null))
    }
}
