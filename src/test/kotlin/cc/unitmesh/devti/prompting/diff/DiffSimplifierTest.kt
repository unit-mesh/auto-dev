package cc.unitmesh.devti.prompting.diff;

import junit.framework.TestCase.assertEquals
import org.junit.Test

class DiffSimplifierTest {
    @Test
    fun testSimplifyNewFile() {
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

    @Test
    fun testSimplifyRenameFile() {
        val diff = """
            Index: server/src/main/kotlin/com/thoughtworks/archguard/code/module/domain/model/LeafManger.kt
            rename from server/src/main/kotlin/com/thoughtworks/archguard/code/module/domain/model/LeafManger.kt
            rename to server/metric-service/src/main/kotlin/org/archguard/arch/LeafManger.kt
            --- a/server/src/main/kotlin/com/thoughtworks/archguard/code/module/domain/model/LeafManger.kt	
            +++ b/server/metric-service/src/main/kotlin/org/archguard/arch/LeafManger.kt	(date 1704768226000)
            @@ -1,7 +1,5 @@
            """.trimIndent()

        val postProcess = DiffSimplifier.postProcess(diff)
        assertEquals(
            postProcess,
            """rename file server/src/main/kotlin/com/thoughtworks/archguard/code/module/domain/model/LeafManger.kt server/metric-service/src/main/kotlin/org/archguard/arch/LeafManger.kt""".trimMargin()
        )
    }
}
