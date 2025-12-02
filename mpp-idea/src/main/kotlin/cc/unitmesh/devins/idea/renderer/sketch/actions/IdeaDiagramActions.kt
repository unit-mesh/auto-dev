package cc.unitmesh.devins.idea.renderer.sketch.actions

import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileWrapper
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

/**
 * Business logic actions for Diagram operations (Mermaid, PlantUML, Graphviz) in mpp-idea.
 * 
 * Related GitHub Issue: https://github.com/phodal/auto-dev/issues/25
 */
object IdeaDiagramActions {
    
    /**
     * Copy diagram source code to clipboard
     */
    fun copySourceToClipboard(source: String): Boolean {
        return try {
            val selection = StringSelection(source)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, null)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Save diagram to file (PNG, SVG, etc.)
     */
    fun saveDiagramToFile(
        project: Project?,
        bytes: ByteArray,
        format: String,
        defaultFileName: String = "diagram"
    ): Boolean {
        if (project == null) return false
        
        return try {
            val descriptor = FileSaverDescriptor(
                "Save Diagram",
                "Save diagram as $format file",
                format
            )
            
            val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
            val wrapper: VirtualFileWrapper? = dialog.save("$defaultFileName.$format")
            
            if (wrapper != null) {
                val file = wrapper.file
                file.writeBytes(bytes)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Save diagram bytes to a specific file path
     */
    fun saveDiagramToPath(bytes: ByteArray, filePath: String): Boolean {
        return try {
            File(filePath).writeBytes(bytes)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate Mermaid diagram syntax (basic check)
     */
    fun validateMermaidSyntax(code: String): Pair<Boolean, String> {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "Empty diagram code")
        }
        
        val validStarts = listOf(
            "graph", "flowchart", "sequenceDiagram", "classDiagram",
            "stateDiagram", "erDiagram", "journey", "gantt", "pie",
            "gitGraph", "mindmap", "timeline", "quadrantChart",
            "requirementDiagram", "C4Context", "sankey"
        )
        
        val hasValidStart = validStarts.any { 
            trimmed.startsWith(it, ignoreCase = true) 
        }
        
        return if (hasValidStart) {
            Pair(true, "")
        } else {
            Pair(false, "Unknown diagram type. Expected: ${validStarts.joinToString(", ")}")
        }
    }
    
    /**
     * Validate PlantUML diagram syntax (basic check)
     */
    fun validatePlantUmlSyntax(code: String): Pair<Boolean, String> {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "Empty diagram code")
        }
        
        val hasStart = trimmed.contains("@startuml", ignoreCase = true) ||
                       trimmed.contains("@startmindmap", ignoreCase = true) ||
                       trimmed.contains("@startgantt", ignoreCase = true) ||
                       trimmed.contains("@startwbs", ignoreCase = true) ||
                       trimmed.contains("@startjson", ignoreCase = true) ||
                       trimmed.contains("@startyaml", ignoreCase = true)
        
        return if (hasStart) {
            Pair(true, "")
        } else {
            Pair(false, "Missing @startuml or similar directive")
        }
    }
    
    /**
     * Validate Graphviz DOT syntax (basic check)
     */
    fun validateDotSyntax(code: String): Pair<Boolean, String> {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "Empty diagram code")
        }
        
        val hasValidStart = trimmed.startsWith("digraph", ignoreCase = true) ||
                           trimmed.startsWith("graph", ignoreCase = true) ||
                           trimmed.startsWith("strict", ignoreCase = true)
        
        return if (hasValidStart) {
            Pair(true, "")
        } else {
            Pair(false, "Expected 'digraph', 'graph', or 'strict' keyword")
        }
    }
    
    /**
     * Get diagram type from language identifier
     */
    fun getDiagramType(language: String): DiagramType {
        return when (language.lowercase()) {
            "mermaid", "mmd" -> DiagramType.MERMAID
            "plantuml", "puml", "uml" -> DiagramType.PLANTUML
            "dot", "graphviz", "gv" -> DiagramType.GRAPHVIZ
            else -> DiagramType.UNKNOWN
        }
    }
}

enum class DiagramType {
    MERMAID,
    PLANTUML,
    GRAPHVIZ,
    UNKNOWN
}

