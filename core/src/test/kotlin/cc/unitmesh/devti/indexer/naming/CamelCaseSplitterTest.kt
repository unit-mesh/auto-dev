package cc.unitmesh.devti.indexer.naming

import org.junit.Test
import kotlin.test.assertEquals

class CamelCaseSplitterTest {
    
    @Test
    fun testCamelCaseSplitting() {
        assertEquals(
            listOf("User", "Management", "Service"),
            CamelCaseSplitter.split("UserManagementService")
        )
    }
    
    @Test
    fun testSimpleCamelCase() {
        assertEquals(
            listOf("User", "Service"),
            CamelCaseSplitter.split("UserService")
        )
    }
    
    @Test
    fun testLowerCamelCase() {
        assertEquals(
            listOf("create", "User"),
            CamelCaseSplitter.split("createUser")
        )
    }
    
    @Test
    fun testSnakeCase() {
        assertEquals(
            listOf("user", "service"),
            CamelCaseSplitter.split("user_service")
        )
    }
    
    @Test
    fun testHyphenCase() {
        assertEquals(
            listOf("user", "service"),
            CamelCaseSplitter.split("user-service")
        )
    }
    
    @Test
    fun testSingleWord() {
        assertEquals(
            listOf("User"),
            CamelCaseSplitter.split("User")
        )
    }
    
    @Test
    fun testEmpty() {
        assertEquals(
            emptyList(),
            CamelCaseSplitter.split("")
        )
    }
    
    @Test
    fun testNormalize() {
        assertEquals(
            "User Management Service",
            CamelCaseSplitter.normalize("UserManagementService")
        )
    }
    
    @Test
    fun testComplexName() {
        // Real world example from code
        assertEquals(
            listOf("get", "User", "Name", "By", "User", "Id"),
            CamelCaseSplitter.split("getUserNameByUserId")
        )
    }
}
