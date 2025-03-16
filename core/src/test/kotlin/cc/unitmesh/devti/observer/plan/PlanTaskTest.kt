package cc.unitmesh.devti.observer.plan

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class PlanTaskTest {

    @Test
    fun should_create_task_from_text_with_completed_status() {
        // given
        val text = "[✓] Complete the project"

        // when
        val task = PlanTask.fromText(text)

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
        val task = PlanTask.fromText(text)

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
        val task = PlanTask.fromText(text)

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
        val task = PlanTask.fromText(text)

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
        val task = PlanTask.fromText(text)

        // then
        assertThat(task.step).isEqualTo("Write documentation")
        assertThat(task.completed).isFalse()
        assertThat(task.status).isEqualTo(TaskStatus.TODO)
    }

    @Test
    fun should_convert_task_to_text_with_completed_status() {
        // given
        val task = PlanTask("Complete the project", true, TaskStatus.COMPLETED)

        // when
        val text = task.toText()

        // then
        assertThat(text).isEqualTo("[✓] Complete the project")
    }

    @Test
    fun should_convert_task_to_text_with_failed_status() {
        // given
        val task = PlanTask("Fix the bug", false, TaskStatus.FAILED)

        // when
        val text = task.toText()

        // then
        assertThat(text).isEqualTo("[!] Fix the bug")
    }

    @Test
    fun should_convert_task_to_text_with_in_progress_status() {
        // given
        val task = PlanTask("Implement feature", false, TaskStatus.IN_PROGRESS)

        // when
        val text = task.toText()

        // then
        assertThat(text).isEqualTo("[*] Implement feature")
    }

    @Test
    fun should_convert_task_to_text_with_todo_status() {
        // given
        val task = PlanTask("Write tests", false, TaskStatus.TODO)

        // when
        val text = task.toText()

        // then
        assertThat(text).isEqualTo("[ ] Write tests")
    }

    @Test
    fun should_update_task_status_to_completed() {
        // given
        val task = PlanTask("Complete the project", false, TaskStatus.TODO)

        // when
        task.updateStatus(TaskStatus.COMPLETED)

        // then
        assertThat(task.status).isEqualTo(TaskStatus.COMPLETED)
        assertThat(task.completed).isTrue()
    }

    @Test
    fun should_update_task_status_to_failed() {
        // given
        val task = PlanTask("Fix the bug", false, TaskStatus.TODO)

        // when
        task.updateStatus(TaskStatus.FAILED)

        // then
        assertThat(task.status).isEqualTo(TaskStatus.FAILED)
        assertThat(task.completed).isFalse()
    }

    @Test
    fun should_update_task_status_to_in_progress() {
        // given
        val task = PlanTask("Implement feature", false, TaskStatus.TODO)

        // when
        task.updateStatus(TaskStatus.IN_PROGRESS)

        // then
        assertThat(task.status).isEqualTo(TaskStatus.IN_PROGRESS)
        assertThat(task.completed).isFalse()
    }

    @Test
    fun should_update_task_status_to_todo() {
        // given
        val task = PlanTask("Write tests", false, TaskStatus.COMPLETED)

        // when
        task.updateStatus(TaskStatus.TODO)

        // then
        assertThat(task.status).isEqualTo(TaskStatus.TODO)
        assertThat(task.completed).isFalse()
    }
}
