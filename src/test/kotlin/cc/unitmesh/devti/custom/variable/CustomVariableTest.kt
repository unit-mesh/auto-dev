package cc.unitmesh.devti.custom.variable

import io.kotest.matchers.shouldBe
import org.junit.Test


class CustomVariableTest {
    @Test
    fun should_parse_variable_from_content() {
        CustomVariable.hasVariable("解释一下代码：\${selection}") shouldBe true
        CustomVariable.hasVariable("解释一下代码：\${selection") shouldBe false
    }
}