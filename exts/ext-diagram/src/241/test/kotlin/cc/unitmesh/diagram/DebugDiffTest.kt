package cc.unitmesh.diagram

import cc.unitmesh.diagram.diff.DiagramDiffUtils
import cc.unitmesh.diagram.parser.MermaidClassDiagramParser
import kotlin.test.Test

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
        
        println("=== Parsing Mermaid Content ===")
        println(mermaidContent)
        
        val result = parser.parse(mermaidContent)
        
        println("\n=== Parsed Result ===")
        println("Entities count: ${result.entities.size}")
        
        for (entity in result.entities) {
            println("\nEntity: ${entity.getName()}")
            println("Fields count: ${entity.getFields().size}")
            
            for (field in entity.getFields()) {
                println("  Field: ${field.name}")
                println("    Type: ${field.type}")
                println("    IsMethod: ${field.isMethod()}")
                println("    ChangeStatus: ${field.changeStatus}")
                println("    DisplayName: ${field.getDisplayName()}")
                println("    ToString: ${field}")
            }
        }
        
        // Test categorization
        val categoryManager = GraphvizNodeCategoryManager()
        val categories = categoryManager.getContentCategories()
        
        println("\n=== Categories ===")
        for (category in categories) {
            println("Category: ${category.name}")
        }
        
        println("\n=== Categorization Test ===")
        val entity = result.entities.first()
        val fields = entity.getFields()
        
        for (field in fields) {
            println("\nField: ${field.name}")
            for (category in categories) {
                val isInCategory = categoryManager.isInCategory(entity, field, category, null)
                if (isInCategory) {
                    println("  -> In category: ${category.name}")
                }
            }
        }
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
        
        println("=== Old Mermaid ===")
        println(oldMermaid)
        
        println("\n=== New Mermaid ===")
        println(newMermaid)
        
        val diffResult = DiagramDiffUtils.compareMermaidDiagrams(oldMermaid, newMermaid)
        
        println("\n=== Diff Result ===")
        val entity = diffResult.entities.first()
        println("Entity: ${entity.getName()}")
        
        for (field in entity.getFields()) {
            println("  Field: ${field.name}")
            println("    ChangeStatus: ${field.changeStatus}")
            println("    IsMethod: ${field.isMethod()}")
            println("    DisplayName: ${field.getDisplayName()}")
        }
        
        // Test categorization of diff result
        val categoryManager = GraphvizNodeCategoryManager()
        val categories = categoryManager.getContentCategories()
        
        println("\n=== Diff Categorization ===")
        for (field in entity.getFields()) {
            println("\nField: ${field.name} (${field.changeStatus})")
            for (category in categories) {
                val isInCategory = categoryManager.isInCategory(entity, field, category, null)
                if (isInCategory) {
                    println("  -> In category: ${category.name}")
                }
            }
        }
        
        // Generate summary
        val summary = DiagramDiffUtils.generateChangeSummary(diffResult)
        println("\n=== Change Summary ===")
        println("Added entities: ${summary.addedEntities}")
        println("Removed entities: ${summary.removedEntities}")
        println("Added fields: ${summary.addedFields}")
        println("Removed fields: ${summary.removedFields}")
        println("Added methods: ${summary.addedMethods}")
        println("Removed methods: ${summary.removedMethods}")
        
        // Generate report
        val report = DiagramDiffUtils.generateChangeReport(diffResult)
        println("\n=== Change Report ===")
        println(report)
    }
}
