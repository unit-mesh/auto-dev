package cc.unitmesh.diagram.diff

import cc.unitmesh.diagram.model.*
import cc.unitmesh.diagram.parser.MermaidClassDiagramParser

/**
 * Utility functions for diagram diff operations
 */
object DiagramDiffUtils {
    
    private val analyzer = DiagramDiffAnalyzer()
    private val mermaidParser = MermaidClassDiagramParser()
    
    /**
     * Compare two Mermaid class diagram strings and return a diff result
     */
    fun compareMermaidDiagrams(oldMermaid: String?, newMermaid: String): GraphDiagramData {
        val oldDiagram = if (oldMermaid != null) {
            mermaidParser.parse(oldMermaid)
        } else {
            null
        }
        
        val newDiagram = mermaidParser.parse(newMermaid)
        
        return analyzer.analyzeDiff(oldDiagram, newDiagram)
    }
    
    /**
     * Generate a summary of changes between two diagrams
     */
    fun generateChangeSummary(diffResult: GraphDiagramData): ChangeSummary {
        var addedEntities = 0
        var removedEntities = 0
        var addedFields = 0
        var removedFields = 0
        var addedMethods = 0
        var removedMethods = 0
        
        for (entity in diffResult.entities) {
            val fields = entity.getFields()

            // Check if entire entity is new or removed
            val allFieldsAdded = fields.isNotEmpty() && fields.all { it.changeStatus == ChangeStatus.ADDED }
            val allFieldsRemoved = fields.isNotEmpty() && fields.all { it.changeStatus == ChangeStatus.REMOVED }

            if (allFieldsAdded) {
                addedEntities++
                // Don't count individual fields for new entities
                continue
            } else if (allFieldsRemoved) {
                removedEntities++
                // Don't count individual fields for removed entities
                continue
            }

            // Count individual field/method changes for existing entities
            for (field in fields) {
                when (field.changeStatus) {
                    ChangeStatus.ADDED -> {
                        if (field.isMethod()) {
                            addedMethods++
                        } else {
                            addedFields++
                        }
                    }
                    ChangeStatus.REMOVED -> {
                        if (field.isMethod()) {
                            removedMethods++
                        } else {
                            removedFields++
                        }
                    }
                    ChangeStatus.UNCHANGED -> {
                        // No action needed for unchanged items
                    }
                }
            }
        }
        
        return ChangeSummary(
            addedEntities = addedEntities,
            removedEntities = removedEntities,
            addedFields = addedFields,
            removedFields = removedFields,
            addedMethods = addedMethods,
            removedMethods = removedMethods
        )
    }
    
    /**
     * Generate a human-readable change report
     */
    fun generateChangeReport(diffResult: GraphDiagramData): String {
        val summary = generateChangeSummary(diffResult)
        
        return buildString {
            appendLine("## Code Structure Changes")
            appendLine()

            if (!summary.hasChanges()) {
                appendLine("No structural changes detected.")
            } else {
                when {
                    summary.addedEntities > 0 -> {
                        appendLine("✅ **Added ${summary.addedEntities} class(es)**")
                    }

                    summary.removedEntities > 0 -> {
                        appendLine("❌ **Removed ${summary.removedEntities} class(es)**")
                    }

                    summary.addedFields > 0 -> {
                        appendLine("✅ **Added ${summary.addedFields} field(s)**")
                    }

                    summary.removedFields > 0 -> {
                        appendLine("❌ **Removed ${summary.removedFields} field(s)**")
                    }

                    summary.addedMethods > 0 -> {
                        appendLine("✅ **Added ${summary.addedMethods} method(s)**")
                    }

                    summary.removedMethods > 0 -> {
                        appendLine("❌ **Removed ${summary.removedMethods} method(s)**")
                    }

                    else -> {
                        appendLine()
                        appendLine("### Detailed Changes")

                        for (entity in diffResult.entities) {
                            val fields = entity.getFields()
                            val changedFields = fields.filter { it.changeStatus != ChangeStatus.UNCHANGED }

                            if (changedFields.isNotEmpty()) {
                                appendLine()
                                appendLine("**${entity.getName()}:**")

                                for (field in changedFields) {
                                    val prefix = when (field.changeStatus) {
                                        ChangeStatus.ADDED -> "  + "
                                        ChangeStatus.REMOVED -> "  - "
                                        ChangeStatus.UNCHANGED -> "    "
                                    }
                                    val type = if (field.isMethod()) "method" else "field"
                                    appendLine("$prefix${field.name} ($type)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary of changes between two diagrams
 */
data class ChangeSummary(
    val addedEntities: Int = 0,
    val removedEntities: Int = 0,
    val addedFields: Int = 0,
    val removedFields: Int = 0,
    val addedMethods: Int = 0,
    val removedMethods: Int = 0
) {
    fun hasChanges(): Boolean {
        return addedEntities > 0 || removedEntities > 0 || 
               addedFields > 0 || removedFields > 0 || 
               addedMethods > 0 || removedMethods > 0
    }
    
    fun totalChanges(): Int {
        return addedEntities + removedEntities + addedFields + removedFields + addedMethods + removedMethods
    }
}
