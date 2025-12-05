package cc.unitmesh.devti.observer.plan

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class PlanTaskTest {

    @Test
    fun should_create_task_from_text_with_completed_status() {
        // given
        val text = "[✓] Complete the project"

        // when
        val task = AgentPlanStep.fromText(text)

        // then
        assertThat(task.step).isEqualTo("Complete the project")
        assertThat(task.completed).isTrue()
        assertThat(task.status).isEqualTo(TaskStatus.COMPLETED)
    }

    @Test
    fun should_create_task_from_text_with_failed_status() {
        // given
        val text = "[!] Fix the bug"

        // when
        val task = AgentPlanStep.fromText(text)

        // then
        assertThat(task.step).isEqualTo("Fix the bug")
        assertThat(task.completed).isFalse()
        assertThat(task.status).isEqualTo(TaskStatus.FAILED)
    }

    @Test
    fun should_create_task_from_text_with_in_progress_status() {
        // given
        val text = "[*] Implement feature"

        // when
        val task = AgentPlanStep.fromText(text)

        // then
        assertThat(task.step).isEqualTo("Implement feature")
        assertThat(task.completed).isFalse()
        assertThat(task.status).isEqualTo(TaskStatus.IN_PROGRESS)
    }

    @Test
    fun should_create_task_from_text_with_todo_status() {
        // given
        val text = "[ ] Write tests"

        // when
        val task = AgentPlanStep.fromText(text)

        // then
        assertThat(task.step).isEqualTo("Write tests")
        assertThat(task.completed).isFalse()
        assertThat(task.status).isEqualTo(TaskStatus.TODO)
    }

    @Test
    fun should_create_task_from_text_without_status_marker() {
        // given
        val text = "Write documentation"

        // when
        val task = AgentPlanStep.fromText(text)

        // then
        assertThat(task.step).isEqualTo("Write documentation")
        assertThat(task.completed).isFalse()
        assertThat(task.status).isEqualTo(TaskStatus.TODO)
    }

    @Test
    fun should_convert_task_to_text_with_completed_status() {
        // given
        val task = AgentPlanStep("Complete the project", true, TaskStatus.COMPLETED, mutableListOf(), emptyList())

        // when
        val text = task.toText()

        // then
        assertThat(text).isEqualTo("[✓] Complete the project")
    }

    @Test
    fun should_convert_task_to_text_with_failed_status() {
        // given
        val task = AgentPlanStep("Fix the bug", false, TaskStatus.FAILED, mutableListOf(), emptyList())

        // when
        val text = task.toText()

        // then
        assertThat(text).isEqualTo("[!] Fix the bug")
    }

    @Test
    fun should_convert_task_to_text_with_in_progress_status() {
        // given
        val task = AgentPlanStep("Implement feature", false, TaskStatus.IN_PROGRESS, mutableListOf(), emptyList())

        // when
        val text = task.toText()

        // then
        assertThat(text).isEqualTo("[*] Implement feature")
    }

    @Test
    fun should_convert_task_to_text_with_todo_status() {
        // given
        val task = AgentPlanStep("Write tests", false, TaskStatus.TODO, mutableListOf(), emptyList())

        // when
        val text = task.toText()

        // then
        assertThat(text).isEqualTo("[ ] Write tests")
    }

    @Test
    fun should_update_task_status_to_completed() {
        // given
        val task = AgentPlanStep("Complete the project", false, TaskStatus.TODO, mutableListOf(), emptyList())

        // when
        task.updateStatus(TaskStatus.COMPLETED)

        // then
        assertThat(task.status).isEqualTo(TaskStatus.COMPLETED)
        assertThat(task.completed).isTrue()
    }

    @Test
    fun should_update_task_status_to_failed() {
        // given
        val task = AgentPlanStep("Fix the bug", false, TaskStatus.TODO, mutableListOf(), emptyList())

        // when
        task.updateStatus(TaskStatus.FAILED)

        // then
        assertThat(task.status).isEqualTo(TaskStatus.FAILED)
        assertThat(task.completed).isFalse()
    }

    @Test
    fun should_update_task_status_to_in_progress() {
        // given
        val task = AgentPlanStep("Implement feature", false, TaskStatus.TODO, mutableListOf(), emptyList())

        // when
        task.updateStatus(TaskStatus.IN_PROGRESS)

        // then
        assertThat(task.status).isEqualTo(TaskStatus.IN_PROGRESS)
        assertThat(task.completed).isFalse()
    }

    @Test
    fun should_update_task_status_to_todo() {
        // given
        val task = AgentPlanStep("Write tests", false, TaskStatus.COMPLETED, mutableListOf(), emptyList())

        // when
        task.updateStatus(TaskStatus.TODO)

        // then
        assertThat(task.status).isEqualTo(TaskStatus.TODO)
        assertThat(task.completed).isFalse()
    }
}
