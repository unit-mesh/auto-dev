package cc.unitmesh.devti.llms.azure

import junit.framework.TestCase.assertEquals
import org.junit.Test

class AzureOpenAIProviderTest {
    @Test
    fun should_check_host_is_endpoint() {
        assertEquals(AzureOpenAIProvider.tryFixHostUrl("https://api.openai.com"), "https://api.openai.com/")
        assertEquals(AzureOpenAIProvider.tryFixHostUrl("https://api.openai.com/"), "https://api.openai.com/")
        assertEquals(AzureOpenAIProvider.tryFixHostUrl("https://api.openai.com/v1"), "https://api.openai.com/v1/")
    }
}