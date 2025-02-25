package cc.unitmesh.devti.custom.schema

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.assertj.core.api.Assertions.assertThat

class CustomLlmSchemaFileProviderTest {

    @Test
    fun should_return_true_when_file_is_valid_and_has_correct_name() {
        // Given
        val project = mock(Project::class.java)
        val file = mock(VirtualFile::class.java)
        `when`(file.isValid).thenReturn(true)
        `when`(file.name).thenReturn("autodev-custom-llms.json")

        val provider = CustomLlmSchemaFileProvider(project)

        // When
        val result = provider.isAvailable(file)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun should_return_false_when_file_is_invalid() {
        // Given
        val project = mock(Project::class.java)
        val file = mock(VirtualFile::class.java)
        `when`(file.isValid).thenReturn(false)

        val provider = CustomLlmSchemaFileProvider(project)

        // When
        val result = provider.isAvailable(file)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun should_return_false_when_file_name_is_incorrect() {
        // Given
        val project = mock(Project::class.java)
        val file = mock(VirtualFile::class.java)
        `when`(file.isValid).thenReturn(true)
        `when`(file.name).thenReturn("incorrect-file-name.json")

        val provider = CustomLlmSchemaFileProvider(project)

        // When
        val result = provider.isAvailable(file)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun should_return_correct_name_for_provider() {
        // Given
        val project = mock(Project::class.java)
        val provider = CustomLlmSchemaFileProvider(project)

        // When
        val name = provider.getName()

        // Then
        assertThat(name).isEqualTo("AutoDevCustomLlmFile")
    }

    @Test
    fun should_return_correct_schema_file() {
        // Given
        val project = mock(Project::class.java)
        val provider = CustomLlmSchemaFileProvider(project)
        val expectedFile = VfsUtil.findFileByURL(javaClass.getResource("autodev-custom-llms.json")!!)

        // When
        val schemaFile = provider.getSchemaFile()

        // Then
        assertThat(schemaFile).isEqualTo(expectedFile)
    }

    @Test
    fun should_return_correct_schema_type() {
        // Given
        val project = mock(Project::class.java)
        val provider = CustomLlmSchemaFileProvider(project)

        // When
        val schemaType = provider.getSchemaType()

        // Then
        assertThat(schemaType).isEqualTo(SchemaType.embeddedSchema)
    }
}
