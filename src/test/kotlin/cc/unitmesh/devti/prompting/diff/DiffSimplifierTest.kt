package cc.unitmesh.devti.prompting.diff;

import junit.framework.TestCase.assertEquals
import org.junit.Test

class DiffSimplifierTest {
    @Test
    fun testSimplify() {
        val sampleDiff = """
        new file mode 100644
        --- /dev/null	(date 1704768267000)
        +++ b/server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/LogicModuleDTO.kt	(date 1704768267000)
        @@ -0,0 +1,17 @@
        +package com.thoughtworks.archguard.code.module.infrastructure
        """.trimIndent()

        val postProcess = DiffSimplifier.postProcess(sampleDiff)
        assertEquals(
            postProcess,
            """new file server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/LogicModuleDTO.kt
+package com.thoughtworks.archguard.code.module.infrastructure"""
        )
    }
}