package cc.unitmesh.devti.llm2.model

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomRequestTest {

    @Test
    fun `should parse boolean stream value true`() {
        val requestFormat = """{"customFields": {"model": "test-model", "stream": true}}"""
        val result = CustomRequest.fromLegacyFormat(requestFormat)
        
        assertTrue(result.stream)
        assertEquals(JsonPrimitive(true), result.body["stream"])
    }

    @Test
    fun `should parse boolean stream value false`() {
        val requestFormat = """{"customFields": {"model": "test-model", "stream": false}}"""
        val result = CustomRequest.fromLegacyFormat(requestFormat)
        
        assertFalse(result.stream)
        assertEquals(JsonPrimitive(false), result.body["stream"])
    }

    @Test
    fun `should parse numeric stream value 1 as true`() {
        val requestFormat = """{"customFields": {"model": "test-model", "stream": 1}}"""
        val result = CustomRequest.fromLegacyFormat(requestFormat)
        
        assertTrue(result.stream)
        assertEquals(JsonPrimitive(1), result.body["stream"])
    }

    @Test
    fun `should parse numeric stream value 0 as false`() {
        val requestFormat = """{"customFields": {"model": "test-model", "stream": 0}}"""
        val result = CustomRequest.fromLegacyFormat(requestFormat)
        
        assertFalse(result.stream)
        assertEquals(JsonPrimitive(0), result.body["stream"])
    }

    @Test
    fun `should parse string stream value true`() {
        val requestFormat = """{"customFields": {"model": "test-model", "stream": "true"}}"""
        val result = CustomRequest.fromLegacyFormat(requestFormat)
        
        assertTrue(result.stream)
        assertEquals(JsonPrimitive("true"), result.body["stream"])
    }

    @Test
    fun `should parse string stream value false`() {
        val requestFormat = """{"customFields": {"model": "test-model", "stream": "false"}}"""
        val result = CustomRequest.fromLegacyFormat(requestFormat)
        
        assertFalse(result.stream)
        assertEquals(JsonPrimitive("false"), result.body["stream"])
    }

    @Test
    fun `should default to true when stream is missing`() {
        val requestFormat = """{"customFields": {"model": "test-model"}}"""
        val result = CustomRequest.fromLegacyFormat(requestFormat)
        
        assertTrue(result.stream)
    }

    @Test
    fun `should parse custom headers correctly`() {
        val requestFormat = """{"customHeaders": {"X-Custom": "value"}, "customFields": {"model": "test"}}"""
        val result = CustomRequest.fromLegacyFormat(requestFormat)
        
        assertEquals("value", result.headers["X-Custom"])
    }
}
