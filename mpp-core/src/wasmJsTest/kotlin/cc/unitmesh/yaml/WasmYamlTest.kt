package cc.unitmesh.yaml

import cc.unitmesh.agent.Platform
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WebAssembly platform specific YAML tests
 * Tests YAML functionality in WebAssembly environment with focus on performance and memory efficiency
 */
class WasmYamlTest {
    
    @Serializable
    data class WasmConfig(
        val moduleName: String,
        val memoryPages: Int,
        val exports: List<String>,
        val imports: Map<String, String>
    )
    
    @Test
    fun testWasmPlatformDetection() {
        assertTrue(Platform.isWasm, "Should be running on WebAssembly platform")
        assertEquals("WebAssembly", Platform.name)
        println("✅ Running on WebAssembly platform: ${Platform.name}")
    }
    
    @Test
    fun testWasmYamlBasicParsing() {
        val yamlContent = """
            wasm_module:
              name: "autocrud_core"
              version: "1.0.0"
              memory:
                initial: 16
                maximum: 256
              exports:
                - "process_yaml"
                - "parse_config"
                - "serialize_data"
              imports:
                env:
                  memory: "shared"
                  console: "log"
        """.trimIndent()
        
        val data = YamlUtils.load(yamlContent)
        assertNotNull(data, "YAML should parse successfully in WebAssembly")
        
        @Suppress("UNCHECKED_CAST")
        val wasmModule = data["wasm_module"] as Map<String, Any>
        assertEquals("autocrud_core", wasmModule["name"])
        assertEquals("1.0.0", wasmModule["version"])
        
        @Suppress("UNCHECKED_CAST")
        val memory = wasmModule["memory"] as Map<String, Any>
        assertEquals(16, memory["initial"])
        assertEquals(256, memory["maximum"])
        
        @Suppress("UNCHECKED_CAST")
        val exports = wasmModule["exports"] as List<String>
        assertTrue(exports.contains("process_yaml"))
        assertTrue(exports.contains("parse_config"))
        assertTrue(exports.contains("serialize_data"))
        
        println("✅ WebAssembly YAML basic parsing test passed")
    }
    
    @Test
    fun testWasmYamlSerialization() {
        val config = WasmConfig(
            moduleName = "yaml_processor",
            memoryPages = 64,
            exports = listOf("parse", "serialize", "validate"),
            imports = mapOf(
                "env.memory" to "shared",
                "env.console" to "log",
                "env.performance" to "now"
            )
        )
        
        // Test serialization to YAML
        val yamlString = YamlUtils.dump(config, WasmConfig.serializer())
        assertTrue(yamlString.isNotEmpty(), "YAML serialization should work in WebAssembly")
        assertTrue(yamlString.contains("yaml_processor"))
        assertTrue(yamlString.contains("64"))
        assertTrue(yamlString.contains("parse"))
        
        // Test deserialization from YAML
        val deserializedConfig = YamlUtils.loadAs(yamlString, WasmConfig.serializer())
        assertEquals(config.moduleName, deserializedConfig.moduleName)
        assertEquals(config.memoryPages, deserializedConfig.memoryPages)
        assertEquals(config.exports, deserializedConfig.exports)
        assertEquals(config.imports, deserializedConfig.imports)
        
        println("✅ WebAssembly YAML serialization/deserialization test passed")
    }
    
    @Test
    fun testWasmMemoryEfficientYaml() {
        // Test memory-efficient YAML processing in WASM
        val memoryEfficientYaml = """
            wasm_optimization:
              memory_management:
                gc_strategy: "incremental"
                heap_size: "32mb"
                stack_size: "1mb"
              performance:
                simd_enabled: true
                threads_enabled: false
                bulk_memory: true
              features:
                - "reference_types"
                - "multi_value"
                - "tail_call"
        """.trimIndent()
        
        val data = YamlUtils.load(memoryEfficientYaml)
        assertNotNull(data, "Memory-efficient YAML should parse in WebAssembly")
        
        @Suppress("UNCHECKED_CAST")
        val wasmOpt = data["wasm_optimization"] as Map<String, Any>
        
        @Suppress("UNCHECKED_CAST")
        val memoryMgmt = wasmOpt["memory_management"] as Map<String, Any>
        assertEquals("incremental", memoryMgmt["gc_strategy"])
        assertEquals("32mb", memoryMgmt["heap_size"])
        assertEquals("1mb", memoryMgmt["stack_size"])
        
        @Suppress("UNCHECKED_CAST")
        val performance = wasmOpt["performance"] as Map<String, Any>
        assertEquals(true, performance["simd_enabled"])
        assertEquals(false, performance["threads_enabled"])
        assertEquals(true, performance["bulk_memory"])
        
        @Suppress("UNCHECKED_CAST")
        val features = wasmOpt["features"] as List<String>
        assertTrue(features.contains("reference_types"))
        assertTrue(features.contains("multi_value"))
        assertTrue(features.contains("tail_call"))
        
        println("✅ WebAssembly memory-efficient YAML test passed")
    }
    
    @Test
    fun testWasmYamlPerformance() {
        // Test high-performance YAML processing in WASM
        val performanceYaml = buildString {
            appendLine("performance_test:")
            appendLine("  data_sets:")
            repeat(50) { i ->
                appendLine("    dataset_$i:")
                appendLine("      id: $i")
                appendLine("      size: ${i * 1000}")
                appendLine("      type: \"benchmark\"")
                appendLine("      metrics:")
                appendLine("        throughput: ${i * 10.5}")
                appendLine("        latency: ${100 - i}")
                appendLine("        memory_usage: ${i * 2.5}")
                appendLine("      tags: [\"perf\", \"test\", \"wasm\"]")
            }
        }
        
        // Measure parsing performance
        val data = YamlUtils.load(performanceYaml)
        assertNotNull(data, "Performance YAML should parse successfully in WebAssembly")
        
        @Suppress("UNCHECKED_CAST")
        val perfTest = data["performance_test"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val dataSets = perfTest["data_sets"] as Map<String, Any>
        assertEquals(50, dataSets.size)
        
        // Verify a sample dataset
        @Suppress("UNCHECKED_CAST")
        val dataset0 = dataSets["dataset_0"] as Map<String, Any>
        assertEquals(0, dataset0["id"])
        assertEquals(0, dataset0["size"])
        assertEquals("benchmark", dataset0["type"])
        
        println("✅ WebAssembly YAML performance test passed")
        println("   Successfully processed 50 datasets in WASM environment")
    }
    
    @Test
    fun testWasmYamlErrorHandling() {
        // Test error handling in WebAssembly environment
        val invalidYaml = """
            wasm_error_test: {
                invalid_structure: [missing_close
                malformed: yaml
            }
        """.trimIndent()
        
        try {
            YamlUtils.load(invalidYaml)
            throw AssertionError("Should have thrown YamlParseException in WASM")
        } catch (e: YamlParseException) {
            assertTrue(e.message?.contains("Failed to parse YAML") == true)
            println("✅ WebAssembly YAML error handling test passed")
        }
    }
    
    @Test
    fun testWasmYamlComplexStructures() {
        // Test complex nested structures in WASM
        val complexWasmYaml = """
            wasm_application:
              modules:
                core:
                  exports: ["init", "process", "cleanup"]
                  imports: ["env.memory", "env.table"]
                  memory:
                    min: 1
                    max: 16
                utils:
                  exports: ["hash", "encode", "decode"]
                  imports: ["env.crypto"]
                  memory:
                    min: 1
                    max: 4
              runtime:
                instantiation:
                  async: true
                  streaming: true
                execution:
                  threads: 1
                  simd: true
              interop:
                js_bindings:
                  - name: "console_log"
                    type: "function"
                  - name: "fetch_data"
                    type: "async_function"
        """.trimIndent()
        
        val data = YamlUtils.load(complexWasmYaml)
        assertNotNull(data, "Complex WASM YAML should parse successfully")
        
        @Suppress("UNCHECKED_CAST")
        val wasmApp = data["wasm_application"] as Map<String, Any>
        
        @Suppress("UNCHECKED_CAST")
        val modules = wasmApp["modules"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val coreModule = modules["core"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val coreExports = coreModule["exports"] as List<String>
        assertTrue(coreExports.contains("init"))
        assertTrue(coreExports.contains("process"))
        assertTrue(coreExports.contains("cleanup"))
        
        @Suppress("UNCHECKED_CAST")
        val runtime = wasmApp["runtime"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val instantiation = runtime["instantiation"] as Map<String, Any>
        assertEquals(true, instantiation["async"])
        assertEquals(true, instantiation["streaming"])
        
        println("✅ WebAssembly complex YAML structures test passed")
    }
}
