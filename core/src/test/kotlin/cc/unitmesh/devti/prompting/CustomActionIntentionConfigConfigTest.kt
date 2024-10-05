package cc.unitmesh.devti.prompting

import cc.unitmesh.devti.util.parser.parseCodeFromString
import cc.unitmesh.devti.custom.action.CustomPromptConfig
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import java.io.File

class CustomActionIntentionConfigConfigTest {
    @Test
    fun should_serial_from_readme_string() {
        val readmeFile = File("../README.md").readText()
        val codeBlocks = parseCodeFromString(readmeFile)
        val configExample = codeBlocks.last()
        val config = CustomPromptConfig.tryParse(configExample)

        assertNotNull(config)
    }
}