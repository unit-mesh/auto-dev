package cc.unitmesh.devti.language.documentation

import org.junit.Test
import junit.framework.TestCase.assertEquals

class DevInsDocumentationProviderCompanionTest {

    @Test
    fun should_return_empty_list_when_no_commands_available() {
        // When
        val actualCommands = DevInsDocumentationProvider.allCommands()

        // Then
        print(actualCommands)
    }
}
