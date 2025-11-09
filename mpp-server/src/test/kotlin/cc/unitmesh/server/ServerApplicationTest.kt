package cc.unitmesh.server

import cc.unitmesh.server.model.HealthResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerApplicationTest {
    
    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val health = Json.decodeFromString<HealthResponse>(response.body())
        assertEquals("ok", health.status)
        assertEquals("1.0.0", health.version)
    }
    
    @Test
    fun testProjectsEndpoint() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/api/projects")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}

