package cc.unitmesh.devti.custom.variable

import cc.unitmesh.devti.custom.compile.CustomVariable
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test


class CustomVariableTest {
    @Test
    fun should_parse_variable_from_content() {
        assertTrue(CustomVariable.hasVariable("解释一下代码：\$selection"))
        assertFalse(CustomVariable.hasVariable("解释一下代码：\$selectio"))
    }
}