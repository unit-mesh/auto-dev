package cc.unitmesh.devti.pair.tasking;

import junit.framework.TestCase.assertEquals
import org.junit.Test

class TaskingTest {
    @Test
    fun `should create Tasking object from valid markdown`() {
        // given
        val markdown = """
            - [ ] Task 1
            - [ ] Task 2
        """.trimIndent()

        // when
        val tasking = Tasking.fromMarkdown(markdown)[0]

        // then
        assertEquals("Task 1", tasking.name)
        assertEquals(TaskingStatus.TODO, tasking.status)
    }

    @Test
    fun `should create Tasking object with status DONE from valid markdown`() {
        // given
        val markdown = """
            - [x] Task 1
            - [ ] Task 2
        """.trimIndent()

        // when
        val tasks = Tasking.fromMarkdown(markdown)
        val tasking = tasks[0]

        // then
        assertEquals("Task 1", tasking.name)
        assertEquals(TaskingStatus.DONE, tasking.status)

        val tasking2 = tasks[1]
        assertEquals("Task 2", tasking2.name)
        assertEquals(TaskingStatus.TODO, tasking2.status)
    }
}
