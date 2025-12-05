package cc.unitmesh.devti.sketch.rule

import cc.unitmesh.devti.bridge.knowledge.lookupFile
import cc.unitmesh.devti.mcp.host.readText
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

/**
 * ProjectAgentsMD provides support for https://agents.md/ specification.
 * AGENTS.md is a simple, open format for guiding coding agents, used by over 20k open-source projects.
 * Think of it as a README for agents.
 */
@Service(Service.Level.PROJECT)
class ProjectAgentsMD(private val project: Project) {
    companion object {
        private const val AGENTS_MD_FILENAME = "AGENTS.md"
        private const val AGENTS_MD_LOWERCASE = "agents.md"
    }

    /**
     * Check if AGENTS.md file exists in the project root
     */
    fun hasAgentsMD(): Boolean {
        return getAgentsMDFile() != null
    }

    /**
     * Get AGENTS.md file from project root
     */
    private fun getAgentsMDFile() = project.lookupFile(AGENTS_MD_FILENAME)
        ?: project.lookupFile(AGENTS_MD_LOWERCASE)

    /**
     * Get AGENTS.md content wrapped in XML tags for prompt injection
     */
    fun getAgentsMDContent(): String? {
        val file = getAgentsMDFile() ?: return null
        val content = file.readText()
        return if (content.isNotBlank()) {
            "<agents-md>\n$content\n</agents-md>"
        } else {
            null
        }
    }

    /**
     * Get raw AGENTS.md content without XML wrapper
     */
    fun getRawContent(): String? {
        val file = getAgentsMDFile() ?: return null
        return file.readText().takeIf { it.isNotBlank() }
    }
}