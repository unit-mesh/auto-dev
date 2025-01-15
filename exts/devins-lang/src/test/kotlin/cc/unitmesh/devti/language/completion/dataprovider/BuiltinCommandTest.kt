package cc.unitmesh.devti.language.completion.dataprovider

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import junit.framework.TestCase.assertEquals
import org.junit.Test

class BuiltinCommandTest {
    @Test
    fun shouldEnableGetBuiltinExamples() {
        val commandList = BuiltinCommand.all()
        val map = commandList.map {
            BuiltinCommand.example(it)
        }

        assertEquals(commandList.size, map.size)
    }
}