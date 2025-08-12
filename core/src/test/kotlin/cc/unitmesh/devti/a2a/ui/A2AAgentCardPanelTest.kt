package cc.unitmesh.devti.a2a.ui

import cc.unitmesh.devti.a2a.A2AClientConsumer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.a2a.spec.AgentCard
import io.a2a.spec.AgentProvider
import io.a2a.spec.AgentSkill
import org.junit.Test
import javax.swing.SwingUtilities

class A2AAgentCardPanelTest : BasePlatformTestCase() {

    @Test
    fun `should create A2A agent card panel with basic agent info`() {
        val agentCard = AgentCard.Builder()
            .name("TestAgent")
            .description("A test agent for demonstration")
            .version("1.0.0")
            .provider(AgentProvider.Builder()
                .name("TestProvider")
                .build())
            .build()

        val a2aClientConsumer = A2AClientConsumer()
        
        SwingUtilities.invokeAndWait {
            val panel = A2AAgentCardPanel(project, agentCard, a2aClientConsumer)
            
            assertNotNull(panel)
            assertTrue(panel.isVisible)
            assertEquals("TestAgent", agentCard.name)
            assertEquals("A test agent for demonstration", agentCard.description)
        }
    }

    @Test
    fun `should create A2A agent card panel with skills`() {
        val skill1 = AgentSkill.Builder()
            .name("skill1")
            .description("First skill")
            .build()
            
        val skill2 = AgentSkill.Builder()
            .name("skill2")
            .description("Second skill")
            .build()

        val agentCard = AgentCard.Builder()
            .name("SkillfulAgent")
            .description("An agent with multiple skills")
            .version("2.0.0")
            .provider(AgentProvider.Builder()
                .name("SkillProvider")
                .build())
            .skills(listOf(skill1, skill2))
            .build()

        val a2aClientConsumer = A2AClientConsumer()
        
        SwingUtilities.invokeAndWait {
            val panel = A2AAgentCardPanel(project, agentCard, a2aClientConsumer)
            
            assertNotNull(panel)
            assertEquals(2, agentCard.skills.size)
            assertEquals("SkillfulAgent", agentCard.name)
        }
    }
}
