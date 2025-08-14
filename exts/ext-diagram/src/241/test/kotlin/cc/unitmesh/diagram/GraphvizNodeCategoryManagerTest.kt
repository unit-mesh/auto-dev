package cc.unitmesh.diagram

import cc.unitmesh.diagram.diagram.CodeTopologyNodeCategoryManager
import cc.unitmesh.diagram.model.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GraphvizNodeCategoryManagerTest {
    
    private val categoryManager = CodeTopologyNodeCategoryManager()
    
    @Test
    fun `should categorize unchanged fields correctly`() {
        val field = GraphNodeField(
            name = "id",
            type = "String",
            required = false,
            changeStatus = ChangeStatus.UNCHANGED,
            isMethodField = false
        )
        
        val categories = categoryManager.getContentCategories()
        val fieldsCategory = categories.find { it.name == "Fields" }!!
        
        assertTrue(categoryManager.isInCategory(null, field, fieldsCategory, null))
    }
    
    @Test
    fun `should categorize unchanged methods correctly`() {
        val method = GraphNodeField(
            name = "save()",
            type = "void",
            required = false,
            changeStatus = ChangeStatus.UNCHANGED,
            isMethodField = true
        )
        
        val categories = categoryManager.getContentCategories()
        val methodsCategory = categories.find { it.name == "Methods" }!!
        
        assertTrue(categoryManager.isInCategory(null, method, methodsCategory, null))
    }
    
    @Test
    fun `should categorize added fields correctly`() {
        val field = GraphNodeField(
            name = "email",
            type = "String",
            required = false,
            changeStatus = ChangeStatus.ADDED,
            isMethodField = false
        )
        
        val categories = categoryManager.getContentCategories()
        val addedFieldsCategory = categories.find { it.name == "Added Fields" }!!
        
        assertTrue(categoryManager.isInCategory(null, field, addedFieldsCategory, null))
    }
    
    @Test
    fun `should categorize added methods correctly`() {
        val method = GraphNodeField(
            name = "delete()",
            type = "void",
            required = false,
            changeStatus = ChangeStatus.ADDED,
            isMethodField = true
        )
        
        val categories = categoryManager.getContentCategories()
        val addedMethodsCategory = categories.find { it.name == "Added Methods" }!!
        
        assertTrue(categoryManager.isInCategory(null, method, addedMethodsCategory, null))
    }
    
    @Test
    fun `should categorize removed fields correctly`() {
        val field = GraphNodeField(
            name = "oldField",
            type = "String",
            required = false,
            changeStatus = ChangeStatus.REMOVED,
            isMethodField = false
        )
        
        val categories = categoryManager.getContentCategories()
        val removedFieldsCategory = categories.find { it.name == "Removed Fields" }!!
        
        assertTrue(categoryManager.isInCategory(null, field, removedFieldsCategory, null))
    }
    
    @Test
    fun `should categorize removed methods correctly`() {
        val method = GraphNodeField(
            name = "oldMethod()",
            type = "void",
            required = false,
            changeStatus = ChangeStatus.REMOVED,
            isMethodField = true
        )
        
        val categories = categoryManager.getContentCategories()
        val removedMethodsCategory = categories.find { it.name == "Removed Methods" }!!
        
        assertTrue(categoryManager.isInCategory(null, method, removedMethodsCategory, null))
    }
    
    @Test
    fun `should not categorize fields in wrong categories`() {
        val field = GraphNodeField(
            name = "name",
            type = "String",
            required = false,
            changeStatus = ChangeStatus.UNCHANGED,
            isMethodField = false
        )
        
        val categories = categoryManager.getContentCategories()
        val methodsCategory = categories.find { it.name == "Methods" }!!
        val addedFieldsCategory = categories.find { it.name == "Added Fields" }!!
        
        // Field should not be in Methods category
        assertFalse(categoryManager.isInCategory(null, field, methodsCategory, null))
        
        // Unchanged field should not be in Added Fields category
        assertFalse(categoryManager.isInCategory(null, field, addedFieldsCategory, null))
    }
    
    @Test
    fun `should categorize attributes correctly`() {
        val attribute = GraphAttributeItem("color", "red")
        
        val categories = categoryManager.getContentCategories()
        val attributesCategory = categories.find { it.name == "Attributes" }!!
        
        assertTrue(categoryManager.isInCategory(null, attribute, attributesCategory, null))
    }
    
    @Test
    fun `should have all expected categories`() {
        val categories = categoryManager.getContentCategories()
        val categoryNames = categories.map { it.name }.toSet()
        
        val expectedCategories = setOf(
            "Fields",
            "Methods", 
            "Attributes",
            "Added Fields",
            "Removed Fields",
            "Added Methods",
            "Removed Methods"
        )
        
        assertTrue(categoryNames.containsAll(expectedCategories))
    }
}
