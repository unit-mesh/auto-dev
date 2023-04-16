package cc.unitmesh.devti.analysis

import org.junit.Assert.assertEquals
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
}
