package cc.unitmesh.yaml

import cc.unitmesh.agent.Platform
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JavaScript platform specific YAML tests
 * Tests YAML functionality in browser and Node.js environments
 */
class JsYamlTest {
    
    @Serializable
    data class WebConfig(
        val appName: String,
        val environment: String,
        val features: List<String>,
        val apiEndpoints: Map<String, String>
    )
    
    @Test
    fun testJsPlatformDetection() {
        assertTrue(Platform.isJs, "Should be running on JavaScript platform")
        assertEquals("JavaScript", Platform.name)
        println("✅ Running on JavaScript platform: ${Platform.name}")
    }
    
    @Test
    fun testJsYamlBasicParsing() {
        val yamlContent = """
            web_app:
              name: "AutoCrud Web"
              version: "1.0.0"
              runtime: "browser"
            features:
              - "dynamic_loading"
              - "hot_reload"
              - "spa_routing"
            api:
              base_url: "https://api.example.com"
              timeout: 5000
        """.trimIndent()
        
        val data = YamlUtils.load(yamlContent)
        assertNotNull(data, "YAML should parse successfully in JavaScript")
        
        @Suppress("UNCHECKED_CAST")
        val webApp = data["web_app"] as Map<String, Any>
        assertEquals("AutoCrud Web", webApp["name"])
        assertEquals("browser", webApp["runtime"])
        
        @Suppress("UNCHECKED_CAST")
        val features = data["features"] as List<String>
        assertTrue(features.contains("dynamic_loading"))
        assertTrue(features.contains("hot_reload"))
        
        @Suppress("UNCHECKED_CAST")
        val api = data["api"] as Map<String, Any>
        assertEquals("https://api.example.com", api["base_url"])
        assertEquals(5000, api["timeout"])
        
        println("✅ JavaScript YAML basic parsing test passed")
    }
    
    @Test
    fun testJsYamlSerialization() {
        val config = WebConfig(
            appName = "JS Test App",
            environment = "development",
            features = listOf("webpack", "babel", "typescript"),
            apiEndpoints = mapOf(
                "users" to "/api/users",
                "auth" to "/api/auth",
                "data" to "/api/data"
            )
        )
        
        // Test serialization to YAML
        val yamlString = YamlUtils.dump(config, WebConfig.serializer())
        assertTrue(yamlString.isNotEmpty(), "YAML serialization should work in JavaScript")
        assertTrue(yamlString.contains("JS Test App"))
        assertTrue(yamlString.contains("development"))
        assertTrue(yamlString.contains("webpack"))
        
        // Test deserialization from YAML
        val deserializedConfig = YamlUtils.loadAs(yamlString, WebConfig.serializer())
        assertEquals(config.appName, deserializedConfig.appName)
        assertEquals(config.environment, deserializedConfig.environment)
        assertEquals(config.features, deserializedConfig.features)
        assertEquals(config.apiEndpoints, deserializedConfig.apiEndpoints)
        
        println("✅ JavaScript YAML serialization/deserialization test passed")
    }
    
    @Test
    fun testJsSpecificYamlFeatures() {
        // Test JavaScript-specific scenarios like handling undefined/null values
        val jsSpecificYaml = """
            js_config:
              module_system: "es6"
              target: "es2020"
              source_maps: true
              minify: false
              polyfills:
                - "core-js"
                - "regenerator-runtime"
              build_tools:
                bundler: "webpack"
                transpiler: "babel"
                type_checker: "typescript"
        """.trimIndent()
        
        val data = YamlUtils.load(jsSpecificYaml)
        assertNotNull(data, "JavaScript-specific YAML should parse correctly")
        
        @Suppress("UNCHECKED_CAST")
        val jsConfig = data["js_config"] as Map<String, Any>
        assertEquals("es6", jsConfig["module_system"])
        assertEquals("es2020", jsConfig["target"])
        assertEquals(true, jsConfig["source_maps"])
        assertEquals(false, jsConfig["minify"])
        
        @Suppress("UNCHECKED_CAST")
        val polyfills = jsConfig["polyfills"] as List<String>
        assertTrue(polyfills.contains("core-js"))
        assertTrue(polyfills.contains("regenerator-runtime"))
        
        @Suppress("UNCHECKED_CAST")
        val buildTools = jsConfig["build_tools"] as Map<String, Any>
        assertEquals("webpack", buildTools["bundler"])
        assertEquals("babel", buildTools["transpiler"])
        assertEquals("typescript", buildTools["type_checker"])
        
        println("✅ JavaScript-specific YAML features test passed")
    }
    
    @Test
    fun testJsYamlErrorHandling() {
        // Test error handling in JavaScript environment
        val invalidYaml = """
            invalid: [unclosed array
            malformed: yaml content
            missing: quotes "here
        """.trimIndent()
        
        try {
            YamlUtils.load(invalidYaml)
            throw AssertionError("Should have thrown YamlParseException")
        } catch (e: YamlParseException) {
            assertTrue(e.message?.contains("Failed to parse YAML") == true)
            println("✅ JavaScript YAML error handling test passed")
        }
    }
    
    @Test
    fun testJsYamlPerformance() {
        // Test performance characteristics in JavaScript
        val largeYamlContent = buildString {
            appendLine("large_config:")
            appendLine("  items:")
            repeat(100) { i ->
                appendLine("    - id: $i")
                appendLine("      name: \"Item $i\"")
                appendLine("      enabled: ${i % 2 == 0}")
                appendLine("      metadata:")
                appendLine("        created: \"2024-01-${(i % 28) + 1}\"")
                appendLine("        tags: [\"tag$i\", \"category${i % 5}\"]")
            }
        }
        
        val startTime = kotlin.js.Date.now()
        val data = YamlUtils.load(largeYamlContent)
        val endTime = kotlin.js.Date.now()
        val duration = endTime - startTime
        
        assertNotNull(data, "Large YAML should parse successfully in JavaScript")
        
        @Suppress("UNCHECKED_CAST")
        val largeConfig = data["large_config"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val items = largeConfig["items"] as List<Map<String, Any>>
        assertEquals(100, items.size)
        
        println("✅ JavaScript YAML performance test passed")
        println("   Parsed 100 items in ${duration}ms")
        assertTrue(duration < 1000, "Parsing should complete within 1 second")
    }
}
