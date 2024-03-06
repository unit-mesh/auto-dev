package cc.unitmesh.devti.custom.variable

import cc.unitmesh.devti.custom.compile.CustomVariable
import io.kotest.matchers.shouldBe
import org.junit.Test


class CustomVariableTest {
    @Test
    fun should_parse_variable_from_content() {
        CustomVariable.hasVariable("解释一下代码：\${selection}") shouldBe true
        CustomVariable.hasVariable("解释一下代码：\${selection") shouldBe false
    }
}