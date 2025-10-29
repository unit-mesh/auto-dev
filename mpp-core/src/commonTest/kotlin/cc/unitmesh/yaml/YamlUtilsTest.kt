package cc.unitmesh.yaml

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class YamlUtilsTest {
    
    @Test
    fun testLoadSimpleYaml() {
        val yamlContent = """
            name: "Test"
            version: 1
            enabled: true
        """.trimIndent()
        
        val result = YamlUtils.load(yamlContent)
        
        assertNotNull(result)
        assertEquals("Test", result["name"])
        assertEquals(1, result["version"])
        assertEquals(true, result["enabled"])
    }
    
    @Test
    fun testLoadNestedYaml() {
        val yamlContent = """
            config:
              database:
                host: "localhost"
                port: 5432
              features:
                - "feature1"
                - "feature2"
        """.trimIndent()
        
        val result = YamlUtils.load(yamlContent)
        
        assertNotNull(result)
        val config = result["config"] as Map<String, Any>
        val database = config["database"] as Map<String, Any>
        assertEquals("localhost", database["host"])
        assertEquals(5432, database["port"])
        
        val features = config["features"] as List<String>
        assertEquals(2, features.size)
        assertTrue(features.contains("feature1"))
        assertTrue(features.contains("feature2"))
    }
    
    @Test
    fun testLoadEmptyYaml() {
        val result = YamlUtils.load("")
        assertEquals(null, result)
    }
    
    @Test
    fun testLoadInvalidYaml() {
        val invalidYaml = """
            invalid: [unclosed array
            malformed yaml
        """.trimIndent()
        
        assertFailsWith<YamlParseException> {
            YamlUtils.load(invalidYaml)
        }
    }
    
    @Serializable
    data class TestConfig(
        val name: String,
        val version: Int,
        val enabled: Boolean
    )
    
    @Test
    fun testLoadAsSpecificType() {
        val yamlContent = """
            name: "Test Config"
            version: 2
            enabled: false
        """.trimIndent()

        val result = YamlUtils.loadAs(yamlContent, TestConfig.serializer())

        assertEquals("Test Config", result.name)
        assertEquals(2, result.version)
        assertEquals(false, result.enabled)
    }

    @Test
    fun testDumpToYaml() {
        val config = TestConfig("Test", 1, true)

        val yamlString = YamlUtils.dump(config, TestConfig.serializer())

        assertTrue(yamlString.contains("name: \"Test\""))
        assertTrue(yamlString.contains("version: 1"))
        assertTrue(yamlString.contains("enabled: true"))
    }
    
    @Test
    fun testYamlCompatClass() {
        val yamlContent = """
            target_file: "test.kt"
            instructions: "Test instructions"
        """.trimIndent()
        
        val yamlCompat = YamlCompat()
        val result = yamlCompat.load<Map<String, Any>>(yamlContent)
        
        assertNotNull(result)
        assertEquals("test.kt", result["target_file"])
        assertEquals("Test instructions", result["instructions"])
    }
}
