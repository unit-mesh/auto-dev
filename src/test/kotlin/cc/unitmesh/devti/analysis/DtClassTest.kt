package cc.unitmesh.devti.analysis

import org.junit.Test

class DtClassTest {
    @Test
    fun should_get_class_name() {
        val dtClass = DtClass(
            name = "Test",
            methods = listOf()
        )
        assert(dtClass.name == "Test")
    }

    @Test
    fun should_format_class() {
        val dtClass = DtClass(
            name = "Test",
            methods = listOf(
                DtMethod(
                    name = "<init>",
                    returnType = "void",
                    parameters = listOf(
                        DtParameter(
                            name = "a",
                            type = "Int"
                        ),
                        DtParameter(
                            name = "b",
                            type = "String"
                        )
                    )
                ),
                DtMethod(
                    name = "method1",
                    returnType = "String",
                    parameters = listOf(
                        DtParameter(
                            name = "a",
                            type = "Int"
                        ),
                        DtParameter(
                            name = "b",
                            type = "String"
                        )
                    )
                ),
                DtMethod(
                    name = "method2",
                    returnType = "String",
                    parameters = listOf(
                        DtParameter(
                            name = "a",
                            type = "Int"
                        ),
                        DtParameter(
                            name = "b",
                            type = "String"
                        )
                    )
                )
            )
        )
        val output = dtClass.format()
        assert(output == "class Test\nconstructor(a: Int, b: String)\n- method1(a: Int, b: String): String, method2(a: Int, b: String): String")
    }
}