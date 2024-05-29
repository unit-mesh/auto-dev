package cc.unitmesh.devti.actions.chat;

import cc.unitmesh.devti.actions.chat.base.getCanonicalName
import org.junit.Test
import org.junit.Assert.*

// Test class
class FixThisActionTest {

    @Test
    fun `should return empty array when no match found`() {
        // Given
        val input = "No match here"

        // When
        val result = getCanonicalName(input)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should extract single canonical name from item`() {
        // Given
        val input = "'setAuthor(cc.unitmesh.untitled.demo.entity.Author)'"
        val expected = listOf("cc.unitmesh.untitled.demo.entity.Author")

        // When
        val result = getCanonicalName(input)

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun `should extract multiple canonical names from item`() {
        // Given
        val input = "setAuthor(cc.unitmesh.untitled.demo.entity.Author) and another(cc.unitmesh.untitled.demo.entity.Second)"
        val expected = listOf("cc.unitmesh.untitled.demo.entity.Author", "cc.unitmesh.untitled.demo.entity.Second")

        // When
        val result = getCanonicalName(input)

        // Then
        assertEquals(expected, result)
    }
}
