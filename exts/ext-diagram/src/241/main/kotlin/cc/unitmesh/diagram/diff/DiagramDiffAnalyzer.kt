package cc.unitmesh.diagram.diff

import cc.unitmesh.diagram.model.*

/**
 * Analyzer for comparing two diagram data structures and identifying changes
 * Used to track additions, deletions, and modifications in AI-generated code comparisons
 */
class DiagramDiffAnalyzer {
    
    /**
     * Compare two diagram data structures and return a new diagram with change status annotations
     */
    fun analyzeDiff(oldDiagram: GraphvizDiagramData?, newDiagram: GraphvizDiagramData): GraphvizDiagramData {
        if (oldDiagram == null) {
            // All elements in new diagram are additions
            return markAllAsAdded(newDiagram)
        }
        
        val oldEntitiesMap = oldDiagram.entities.associateBy { it.getName() }
        val newEntitiesMap = newDiagram.entities.associateBy { it.getName() }
        
        val resultEntities = mutableListOf<GraphvizEntityNodeData>()
        
        // Process entities that exist in new diagram
        for (newEntity in newDiagram.entities) {
            val oldEntity = oldEntitiesMap[newEntity.getName()]
            if (oldEntity == null) {
                // Entity is new - mark all fields as added
                val addedFields = newEntity.getFields().map { field ->
                    field.copy(changeStatus = ChangeStatus.ADDED)
                }
                resultEntities.add(GraphvizEntityNodeData(newEntity.getName(), addedFields))
            } else {
                // Entity exists in both - compare fields
                val diffFields = compareFields(oldEntity.getFields(), newEntity.getFields())
                resultEntities.add(GraphvizEntityNodeData(newEntity.getName(), diffFields))
            }
        }
        
        // Process entities that were removed (exist in old but not in new)
        for (oldEntity in oldDiagram.entities) {
            if (!newEntitiesMap.containsKey(oldEntity.getName())) {
                // Entity was removed - mark all fields as removed
                val removedFields = oldEntity.getFields().map { field ->
                    field.copy(changeStatus = ChangeStatus.REMOVED)
                }
                resultEntities.add(GraphvizEntityNodeData(oldEntity.getName(), removedFields))
            }
        }
        
        return newDiagram.copy(entities = resultEntities)
    }
    
    /**
     * Mark all elements in the diagram as added (used when there's no old diagram to compare with)
     */
    private fun markAllAsAdded(diagram: GraphvizDiagramData): GraphvizDiagramData {
        val addedEntities = diagram.entities.map { entity ->
            val addedFields = entity.getFields().map { field ->
                field.copy(changeStatus = ChangeStatus.ADDED)
            }
            GraphvizEntityNodeData(entity.getName(), addedFields)
        }
        
        return diagram.copy(entities = addedEntities)
    }
    
    /**
     * Compare fields between old and new entity versions
     */
    private fun compareFields(oldFields: List<GraphvizNodeField>, newFields: List<GraphvizNodeField>): List<GraphvizNodeField> {
        val oldFieldsMap = oldFields.associateBy { getFieldKey(it) }
        val newFieldsMap = newFields.associateBy { getFieldKey(it) }
        
        val resultFields = mutableListOf<GraphvizNodeField>()
        
        // Process fields in new version
        for (newField in newFields) {
            val key = getFieldKey(newField)
            val oldField = oldFieldsMap[key]
            
            if (oldField == null) {
                // Field is new
                resultFields.add(newField.copy(changeStatus = ChangeStatus.ADDED))
            } else {
                // Field exists in both versions
                resultFields.add(newField.copy(changeStatus = ChangeStatus.UNCHANGED))
            }
        }
        
        // Process fields that were removed (exist in old but not in new)
        for (oldField in oldFields) {
            val key = getFieldKey(oldField)
            if (!newFieldsMap.containsKey(key)) {
                // Field was removed
                resultFields.add(oldField.copy(changeStatus = ChangeStatus.REMOVED))
            }
        }
        
        return resultFields
    }
    
    /**
     * Generate a unique key for a field to enable comparison
     * Uses field name and method status to distinguish between fields and methods with same name
     */
    private fun getFieldKey(field: GraphvizNodeField): String {
        return "${field.name}:${field.isMethod()}"
    }
}
