package cc.unitmesh.devti.vcs.context

import junit.framework.TestCase.*
import org.junit.Test

class TokenBudgetTest {

    @Test
    fun testInitialBudget() {
        val budget = TokenBudget(maxTokens = 1000)
        
        assertEquals("Initial used tokens should be 0", 0, budget.used)
        assertEquals("Initial remaining should equal max", 1000, budget.remaining)
    }

    @Test
    fun testAllocateTokens() {
        val budget = TokenBudget(maxTokens = 1000)
        
        val allocated = budget.allocate(100)
        
        assertTrue("Allocation should succeed", allocated)
        assertEquals("Used tokens should be 100", 100, budget.used)
        assertEquals("Remaining should be 900", 900, budget.remaining)
    }

    @Test
    fun testAllocateMoreThanAvailable() {
        val budget = TokenBudget(maxTokens = 1000)
        
        budget.allocate(900)
        val allocated = budget.allocate(200)
        
        assertFalse("Allocation should fail", allocated)
        assertEquals("Used tokens should still be 900", 900, budget.used)
    }

    @Test
    fun testReleaseTokens() {
        val budget = TokenBudget(maxTokens = 1000)
        
        budget.allocate(500)
        budget.release(200)
        
        assertEquals("Used tokens should be 300", 300, budget.used)
        assertEquals("Remaining should be 700", 700, budget.remaining)
    }

    @Test
    fun testReset() {
        val budget = TokenBudget(maxTokens = 1000)
        
        budget.allocate(500)
        budget.reset()
        
        assertEquals("Used tokens should be 0 after reset", 0, budget.used)
        assertEquals("Remaining should be max after reset", 1000, budget.remaining)
    }

    @Test
    fun testUsagePercentage() {
        val budget = TokenBudget(maxTokens = 1000)
        
        budget.allocate(250)
        
        assertEquals("Usage should be 25%", 25.0, budget.usagePercentage())
    }

    @Test
    fun testHasCapacity() {
        val budget = TokenBudget(maxTokens = 1000)
        
        budget.allocate(900)
        
        assertTrue("Should have capacity for 50", budget.hasCapacity(50))
        assertTrue("Should have capacity for 100", budget.hasCapacity(100))
        assertFalse("Should not have capacity for 200", budget.hasCapacity(200))
    }

    @Test
    fun testFactoryMethods() {
        val gpt4Budget = TokenBudget.forGpt4()
        assertEquals("GPT-4 budget should be 8000", 8000, gpt4Budget.maxTokens)
        
        val gpt35Budget = TokenBudget.forGpt35Turbo()
        assertEquals("GPT-3.5 budget should be 4000", 4000, gpt35Budget.maxTokens)
        
        val customBudget = TokenBudget.custom(5000)
        assertEquals("Custom budget should be 5000", 5000, customBudget.maxTokens)
    }
}

