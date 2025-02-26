package cc.unitmesh.devti.bridge.command

import org.assertj.core.api.Assertions.assertThat
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test

class SCCWrapperTest {
    @Test
    @Ignore
    fun should_return_correct_output_when_scc_is_executed_successfully() {
        // Given
        val sccWrapper = SccWrapper()
        val directory = "."

        // Create a valid directory for testing
        File(directory).mkdirs()

        // When
        val result = sccWrapper.analyzeDirectory(directory, listOf())

        // Then
        assertThat(result.size).isGreaterThan(0)
    }
}
