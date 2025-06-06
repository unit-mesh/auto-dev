package cc.unitmesh.devti.command

import kotlin.test.Test
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertFalse
import kotlin.test.Ignore

class EditApplyTest {
    private val editApply = EditApply()

    @Test
    fun `should recognize existing code markers`() {
        assertTrue(editApply.isExistingCodeMarker("// ... existing code ..."))
        assertTrue(editApply.isExistingCodeMarker("// existing code"))
        assertTrue(editApply.isExistingCodeMarker("// ... existing getters and setters ..."))
        assertTrue(editApply.isExistingCodeMarker("// ... existing methods ..."))
        assertTrue(editApply.isExistingCodeMarker("// ... existing fields ..."))
        assertTrue(editApply.isExistingCodeMarker("// ... existing properties ..."))
        assertTrue(editApply.isExistingCodeMarker("// ... existing constructors ..."))
        assertTrue(editApply.isExistingCodeMarker("// ... existing imports ..."))
        
        // Case insensitive
        assertTrue(editApply.isExistingCodeMarker("// ... EXISTING METHODS ..."))
        assertTrue(editApply.isExistingCodeMarker("// ... Existing Getters And Setters ..."))
        
        // Generic patterns
        assertTrue(editApply.isExistingCodeMarker("// ... existing variables ..."))
        assertTrue(editApply.isExistingCodeMarker("// ... existing ..."))
        
        // Should not match non-markers
        assertFalse(editApply.isExistingCodeMarker("// This is a regular comment"))
        assertFalse(editApply.isExistingCodeMarker("fun someMethod()"))
        assertFalse(editApply.isExistingCodeMarker("val someProperty = value"))
    }

    @Test
    fun `should handle simple existing code marker`() {
        val original = """
            class MyClass {
                val existingField = "value"
                
                fun existingMethod() {
                    println("existing")
                }
            }
        """.trimIndent()

        val edit = """
            class MyClass {
                val newField = "new"
                
                // ... existing code ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)
        
        assertTrue(result.contains("val newField = \"new\""))
        assertTrue(result.contains("val existingField = \"value\""))
        assertTrue(result.contains("fun existingMethod()"))
        assertFalse(result.contains("// ... existing code ..."))
    }

    @Test
    @Ignore
    fun `should handle existing getters and setters marker`() {
        val original = """
            class Person {
                private var name: String = ""
                
                fun getName(): String {
                    return name
                }
                
                fun setName(value: String) {
                    this.name = value
                }
                
                fun someOtherMethod() {
                    println("other")
                }
            }
        """.trimIndent()

        val edit = """
            class Person {
                private var name: String = ""
                private var age: Int = 0
                
                // ... existing getters and setters ...
                
                fun someOtherMethod() {
                    println("other")
                }
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)
        
        assertTrue(result.contains("private var age: Int = 0"))
        assertTrue(result.contains("fun getName(): String"))
        assertTrue(result.contains("fun setName(value: String)"))
        assertTrue(result.contains("fun someOtherMethod()"))
        assertFalse(result.contains("// ... existing getters and setters ..."))
    }

    @Test
    @Ignore
    fun `should handle existing methods marker`() {
        val original = """
            class Calculator {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }
                
                fun subtract(a: Int, b: Int): Int {
                    return a - b
                }
                
                fun multiply(a: Int, b: Int): Int {
                    return a * b
                }
            }
        """.trimIndent()

        val edit = """
            class Calculator {
                fun divide(a: Int, b: Int): Int {
                    return a / b
                }
                
                // ... existing methods ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)
        
        assertTrue(result.contains("fun divide(a: Int, b: Int): Int"))
        assertTrue(result.contains("fun add(a: Int, b: Int): Int"))
        assertTrue(result.contains("fun subtract(a: Int, b: Int): Int"))
        assertTrue(result.contains("fun multiply(a: Int, b: Int): Int"))
        assertFalse(result.contains("// ... existing methods ..."))
    }

    @Test
    @Ignore
    fun `should handle multiple markers in sequence`() {
        val original = """
            class ComplexClass {
                private val field1 = "value1"
                private val field2 = "value2"
                
                fun getField1(): String = field1
                fun setField1(value: String) { /* setter logic */ }
                
                fun businessMethod1() {
                    println("business1")
                }
                
                fun businessMethod2() {
                    println("business2")
                }
            }
        """.trimIndent()

        val edit = """
            class ComplexClass {
                private val newField = "new"
                
                // ... existing fields ...
                
                // ... existing getters and setters ...
                
                fun newBusinessMethod() {
                    println("new business")
                }
                
                // ... existing methods ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)
        
        assertTrue(result.contains("private val newField = \"new\""))
        assertTrue(result.contains("private val field1 = \"value1\""))
        assertTrue(result.contains("private val field2 = \"value2\""))
        assertTrue(result.contains("fun getField1(): String"))
        assertTrue(result.contains("fun setField1(value: String)"))
        assertTrue(result.contains("fun newBusinessMethod()"))
        assertTrue(result.contains("fun businessMethod1()"))
        assertTrue(result.contains("fun businessMethod2()"))
        assertFalse(result.contains("// ... existing"))
    }

    @Test
    fun `should handle marker at the end of file`() {
        val original = """
            class MyClass {
                fun method1() {}
                fun method2() {}
            }
        """.trimIndent()

        val edit = """
            class MyClass {
                fun newMethod() {}
                
                // ... existing methods ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)
        
        assertTrue(result.contains("fun newMethod() {}"))
        assertTrue(result.contains("fun method1() {}"))
        assertTrue(result.contains("fun method2() {}"))
    }

    @Test
    fun `should handle marker at the beginning of file`() {
        val original = """
            import java.util.*
            import java.io.*
            
            class MyClass {
                fun method() {}
            }
        """.trimIndent()

        val edit = """
            // ... existing imports ...
            import java.net.*
            
            class MyClass {
                fun method() {}
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)
        
        assertTrue(result.contains("import java.util.*"))
        assertTrue(result.contains("import java.io.*"))
        assertTrue(result.contains("import java.net.*"))
        assertTrue(result.contains("class MyClass"))
    }

    @Test
    fun `should handle no changes when edit matches original`() {
        val original = """
            class MyClass {
                fun method() {}
            }
        """.trimIndent()

        val edit = """
            class MyClass {
                fun method() {}
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)
        
        assertEquals(original, result)
    }

    @Test
    @Ignore
    fun `should handle empty original content`() {
        val original = ""

        val edit = """
            class NewClass {
                fun newMethod() {}
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)
        
        assertEquals(edit, result)
    }

    @Test
    @Ignore
    fun `should handle empty edit content`() {
        val original = """
            class MyClass {
                fun method() {}
            }
        """.trimIndent()

        val edit = ""

        val result = editApply.applyEdit(original, edit)
        
        assertEquals(original, result)
    }

    @Test
    fun `should preserve indentation and formatting`() {
        val original = """
            class MyClass {
                private val field = "value"

                fun method() {
                    println("test")
                }
            }
        """.trimIndent()

        val edit = """
            class MyClass {
                private val newField = "new"

                // ... existing code ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)

        assertTrue(result.contains("    private val field = \"value\""))
        assertTrue(result.contains("    private val newField = \"new\""))
        assertTrue(result.contains("        println(\"test\")"))
    }

    @Test
    @Ignore
    fun `should handle complex method bodies with braces`() {
        val original = """
            class Service {
                fun complexMethod() {
                    if (condition) {
                        doSomething()
                    } else {
                        doSomethingElse()
                    }
                }

                fun anotherMethod() {
                    println("another")
                }
            }
        """.trimIndent()

        val edit = """
            class Service {
                fun newMethod() {
                    println("new")
                }

                // ... existing methods ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)

        assertTrue(result.contains("fun newMethod()"))
        assertTrue(result.contains("fun complexMethod()"))
        assertTrue(result.contains("if (condition)"))
        assertTrue(result.contains("doSomething()"))
        assertTrue(result.contains("fun anotherMethod()"))
    }

    @Test
    @Ignore
    fun `should handle nested classes and complex structures`() {
        val original = """
            class OuterClass {
                private val outerField = "outer"

                inner class InnerClass {
                    fun innerMethod() {
                        println("inner")
                    }
                }

                fun outerMethod() {
                    println("outer")
                }
            }
        """.trimIndent()

        val edit = """
            class OuterClass {
                private val newField = "new"

                // ... existing code ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)

        assertTrue(result.contains("private val newField = \"new\""))
        assertTrue(result.contains("private val outerField = \"outer\""))
        assertTrue(result.contains("inner class InnerClass"))
        assertTrue(result.contains("fun innerMethod()"))
        assertTrue(result.contains("fun outerMethod()"))
    }

    @Test
    fun `should handle mixed content with comments and annotations`() {
        val original = """
            /**
             * This is a documented class
             */
            @Deprecated("Use NewClass instead")
            class OldClass {
                // This is a field comment
                @JvmField
                val field = "value"

                /**
                 * This method does something
                 */
                @Override
                fun method() {
                    // Method implementation
                    println("implementation")
                }
            }
        """.trimIndent()

        val edit = """
            /**
             * This is a documented class
             */
            @Deprecated("Use NewClass instead")
            class OldClass {
                val newField = "new"

                // ... existing code ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)

        assertTrue(result.contains("val newField = \"new\""))
        assertTrue(result.contains("// This is a field comment"))
        assertTrue(result.contains("@JvmField"))
        assertTrue(result.contains("val field = \"value\""))
        assertTrue(result.contains("/**"))
        assertTrue(result.contains("@Override"))
        assertTrue(result.contains("// Method implementation"))
    }

    @Test
    fun `should handle string literals with special characters`() {
        val original = """
            class StringClass {
                val message = "This contains \"quotes\" and 'apostrophes'"
                val multiline = '''
                    This is a multiline string
                    with various characters
                '''
            }
        """.trimIndent()

        val edit = """
            class StringClass {
                val newMessage = "New message with \"special\" chars"

                // ... existing code ...
            }
        """.trimIndent()

        val result = editApply.applyEdit(original, edit)

        assertTrue(result.contains("val newMessage = \"New message with \\\"special\\\" chars\""))
        assertTrue(result.contains("val message = \"This contains \\\"quotes\\\" and 'apostrophes'\""))
        assertTrue(result.contains("val multiline = '''"))
    }
}
