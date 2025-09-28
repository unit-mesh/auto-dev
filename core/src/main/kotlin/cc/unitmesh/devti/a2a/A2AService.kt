package cc.unitmesh.devti.a2a

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import io.a2a.spec.AgentCard

/**
 * Service for managing A2A agents and their integration with the Sketch system.
 */
@Service(Service.Level.PROJECT)
class A2AService(private val project: Project) {
    private var a2aClientConsumer: A2AClientConsumer? = null
    private var availableAgents: List<AgentCard> = emptyList()
    
    companion object {
        fun getInstance(project: Project): A2AService {
            return project.getService(A2AService::class.java)
        }
    }
    
    /**
     * Initialize A2A service with server configurations
     */
    fun initialize(servers: List<A2aServer>) {
        try {
            a2aClientConsumer = A2AClientConsumer()
            a2aClientConsumer?.init(servers)
            refreshAvailableAgents()
        } catch (e: Exception) {
            // Log error but don't fail
            a2aClientConsumer = null
            availableAgents = emptyList()
        }
    }
    
    /**
     * Get all available A2A agents
     */
    fun getAvailableAgents(): List<AgentCard> {
        return availableAgents
    }
    
    /**
     * Send message to a specific A2A agent
     */
    fun sendMessage(agentName: String, message: String): String? {
        return try {
            a2aClientConsumer?.sendMessage(agentName, message)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if A2A service is available and initialized
     */
    fun isAvailable(): Boolean {
        return a2aClientConsumer != null && availableAgents.isNotEmpty()
    }
    
    /**
     * Refresh the list of available agents
     */
    private fun refreshAvailableAgents() {
        availableAgents = try {
            a2aClientConsumer?.listAgents() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get A2A client consumer for direct access
     */
    fun getClientConsumer(): A2AClientConsumer? {
        return a2aClientConsumer
    }
}
