package cc.unitmesh.yaml

import cc.unitmesh.agent.Platform
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cross-platform YAML functionality tests
 * Ensures YAML processing works consistently across JVM, JS, and WASM platforms
 */
class CrossPlatformYamlTest {
    
    @Serializable
    data class ProjectConfig(
        val name: String,
        val version: String,
        val platforms: List<String>,
        val features: Map<String, Boolean>
    )
    
    @Test
    fun testPlatformSpecificYamlProcessing() {
        val yamlContent = """
            name: "AutoCrud MPP"
            version: "1.0.0"
            platforms:
              - "jvm"
              - "js"
              - "wasm"
            features:
              yaml_support: true
              multiplatform: true
              serialization: true
        """.trimIndent()
        
        // Test basic YAML loading
        val data = YamlUtils.load(yamlContent)
        assertNotNull(data, "YAML should be parsed successfully on ${Platform.name}")
        
        assertEquals("AutoCrud MPP", data["name"])
        assertEquals("1.0.0", data["version"])
        
        @Suppress("UNCHECKED_CAST")
        val platforms = data["platforms"] as List<String>
        assertTrue(platforms.contains("jvm"))
        assertTrue(platforms.contains("js"))
        assertTrue(platforms.contains("wasm"))
        
        @Suppress("UNCHECKED_CAST")
        val features = data["features"] as Map<String, Any>
        assertEquals(true, features["yaml_support"])
        assertEquals(true, features["multiplatform"])
        assertEquals(true, features["serialization"])
        
        println("✅ YAML parsing test passed on ${Platform.name}")
    }
    
    @Test
    fun testSerializableYamlProcessing() {
        val config = ProjectConfig(
            name = "Test Project",
            version = "2.0.0",
            platforms = listOf("jvm", "js", "wasm"),
            features = mapOf(
                "yaml_support" to true,
                "cross_platform" to true
            )
        )
        
        // Test serialization
        val yamlString = YamlUtils.dump(config, ProjectConfig.serializer())
        assertTrue(yamlString.isNotEmpty(), "YAML serialization should produce output on ${Platform.name}")
        assertTrue(yamlString.contains("Test Project"))
        assertTrue(yamlString.contains("2.0.0"))
        
        // Test deserialization
        val deserializedConfig = YamlUtils.loadAs(yamlString, ProjectConfig.serializer())
        assertEquals(config.name, deserializedConfig.name)
        assertEquals(config.version, deserializedConfig.version)
        assertEquals(config.platforms, deserializedConfig.platforms)
        assertEquals(config.features, deserializedConfig.features)
        
        println("✅ YAML serialization/deserialization test passed on ${Platform.name}")
    }
    
    @Test
    fun testComplexYamlStructures() {
        val complexYaml = """
            project:
              metadata:
                name: "Complex Project"
                tags: ["kotlin", "multiplatform", "yaml"]
              build:
                targets:
                  jvm:
                    enabled: true
                    version: "17"
                  js:
                    enabled: true
                    mode: "IR"
                  wasm:
                    enabled: true
                    experimental: true
              dependencies:
                - name: "kotlinx-serialization"
                  version: "1.6.3"
                - name: "kaml"
                  version: "0.61.0"
        """.trimIndent()
        
        val data = YamlUtils.load(complexYaml)
        assertNotNull(data, "Complex YAML should be parsed on ${Platform.name}")
        
        @Suppress("UNCHECKED_CAST")
        val project = data["project"] as Map<String, Any>
        
        @Suppress("UNCHECKED_CAST")
        val metadata = project["metadata"] as Map<String, Any>
        assertEquals("Complex Project", metadata["name"])
        
        @Suppress("UNCHECKED_CAST")
        val tags = metadata["tags"] as List<String>
        assertTrue(tags.contains("kotlin"))
        assertTrue(tags.contains("multiplatform"))
        assertTrue(tags.contains("yaml"))
        
        @Suppress("UNCHECKED_CAST")
        val build = project["build"] as Map<String, Any>
        
        @Suppress("UNCHECKED_CAST")
        val targets = build["targets"] as Map<String, Any>
        
        @Suppress("UNCHECKED_CAST")
        val jvmTarget = targets["jvm"] as Map<String, Any>
        assertEquals(true, jvmTarget["enabled"])
        // Handle both string and number types for version
        val version = jvmTarget["version"]
        assertTrue(version == "17" || version == 17, "Version should be '17' or 17, but was: $version")
        
        println("✅ Complex YAML structure test passed on ${Platform.name}")
    }
    
    @Test
    fun testPlatformCompatibility() {
        // Test that demonstrates platform-specific behavior if any
        val testYaml = """
            platform_test:
              current_platform: "${Platform.name}"
              is_jvm: ${Platform.isJvm}
              is_js: ${Platform.isJs}
              is_wasm: ${Platform.isWasm}
        """.trimIndent()
        
        val data = YamlUtils.load(testYaml)
        assertNotNull(data)
        
        @Suppress("UNCHECKED_CAST")
        val platformTest = data["platform_test"] as Map<String, Any>
        
        assertEquals(Platform.name, platformTest["current_platform"])
        assertEquals(Platform.isJvm, platformTest["is_jvm"])
        assertEquals(Platform.isJs, platformTest["is_js"])
        assertEquals(Platform.isWasm, platformTest["is_wasm"])
        
        println("✅ Platform compatibility test passed on ${Platform.name}")
        println("   Platform: ${Platform.name}")
        println("   JVM: ${Platform.isJvm}, JS: ${Platform.isJs}, WASM: ${Platform.isWasm}")
    }
}
