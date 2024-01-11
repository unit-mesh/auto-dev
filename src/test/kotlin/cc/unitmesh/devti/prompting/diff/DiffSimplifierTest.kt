package cc.unitmesh.devti.prompting.diff;

import com.intellij.openapi.vcs.changes.Change
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.nio.file.PathMatcher

class DiffSimplifierTest {
    @Test
    fun testSimplify() {
        val sampleDiff = """
        Index: server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/LogicModuleDTO.kt
        new file mode 100644
        --- /dev/null	(date 1704768267000)
        +++ b/server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/LogicModuleDTO.kt	(date 1704768267000)
        @@ -0,0 +1,17 @@
        +package com.thoughtworks.archguard.code.module.infrastructure
        """.trimIndent()

        val postProcess = DiffSimplifier.postProcess(sampleDiff)
        println(postProcess)
        assertEquals(
            postProcess,
            """Index: server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/LogicModuleDTO.kt
new file mode 100644
+++ b/server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/LogicModuleDTO.kt	(date 1704768267000)
+package com.thoughtworks.archguard.code.module.infrastructure"""
        )
    }
}