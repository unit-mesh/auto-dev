package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.agenttool.RipgrepSearcher
import org.junit.Test

class RipgrepSearcherTest {
    @Test
    fun should_return_search_error_when_ripgrep_binary_not_found() {
        val binary = RipgrepSearcher.findRipgrepBinary()
        if (binary == null) {
            println("Ripgrep binary not found")
        } else {
            val currentTestPath = System.getProperty("user.dir")
            println("currentTestPath: $currentTestPath")

            val commandLine = RipgrepSearcher.getCommandLine(binary, "blogpost")
            val toProcessBuilder = commandLine.toProcessBuilder()

            val process = toProcessBuilder.start()
            val result = process.inputStream.bufferedReader().readText()

            println(result)
        }
    }
}
