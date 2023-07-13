package cc.unitmesh.devti.prompting

import cc.unitmesh.devti.parser.parseCodeFromString
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import java.io.File

class PromptConfigTest {
    @Test
    fun should_serial_from_readme_string() {
        // read from root README.md file
        val readmeFile = File("README.md").readText()
        val codeBlocks = parseCodeFromString(readmeFile)
        val configExample = codeBlocks.last()
        val config = PromptConfig.tryParse(configExample)

        assertNotNull(config)
    }
}