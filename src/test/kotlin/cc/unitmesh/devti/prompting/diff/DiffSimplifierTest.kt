package cc.unitmesh.devti.prompting.diff;

import cc.unitmesh.devti.vcs.DiffSimplifier
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
            """rename file from server/src/main/kotlin/com/thoughtworks/archguard/code/module/domain/model/LeafManger.kt to server/metric-service/src/main/kotlin/org/archguard/arch/LeafManger.kt""".trimMargin()
        )
    }

    @Test
    fun testHandleForDelete() {
        val diff = """
            Index: server/src/main/kotlin/com/thoughtworks/archguard/metrics/domain/dfms/ModuleDfms.kt
            deleted file mode 100644
            --- a/server/src/main/kotlin/com/thoughtworks/archguard/metrics/domain/dfms/ModuleDfms.kt	
            +++ /dev/null	
            @@ -1,13 +0,0 @@
            -package com.thoughtworks.archguard.metrics.domain.dfms
            -
            Index: server/src/main/kotlin/com/thoughtworks/archguard/metrics/domain/dfms/ClassDfms.kt
            """.trimIndent()

        val postProcess = DiffSimplifier.postProcess(diff)
        assertEquals(
            postProcess,
            """delete file server/src/main/kotlin/com/thoughtworks/archguard/metrics/domain/dfms/ModuleDfms.kt""".trimMargin()
        )
    }

    @Test
    fun testModifyImportChange() {
        val code = """
            Index: server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/dubbo/DubboConfigRepositoryImpl.kt
            --- a/server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/dubbo/DubboConfigRepositoryImpl.kt	
            +++ b/server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/dubbo/DubboConfigRepositoryImpl.kt	(date 1704766567000)
            @@ -2,7 +2,7 @@
             
             import com.thoughtworks.archguard.code.module.domain.dubbo.DubboConfigRepository
             import org.archguard.protocol.dubbo.ReferenceConfig
            -import com.thoughtworks.archguard.code.module.domain.dubbo.ServiceConfig
            +import org.archguard.protocol.dubbo.ServiceConfig
             import org.archguard.protocol.dubbo.SubModuleDubbo
             import org.assertj.core.api.Assertions.assertThat
             import org.junit.jupiter.api.Test
        """.trimIndent()

        val postProcess = DiffSimplifier.postProcess(code)
        assertEquals(
            postProcess,
            """modify file server/src/main/kotlin/com/thoughtworks/archguard/code/module/infrastructure/dubbo/DubboConfigRepositoryImpl.kt
change import from com.thoughtworks.archguard.code.module.domain.dubbo.ServiceConfig to org.archguard.protocol.dubbo.ServiceConfig"""
        )
    }

    @Test
    fun handleForRenameAndChangeImport() {
        val code = """
            Index: server/src/main/kotlin/com/thoughtworks/archguard/code/module/domain/model/LeafManger.kt
            rename from server/src/main/kotlin/com/thoughtworks/archguard/code/module/domain/model/LeafManger.kt
            rename to server/metric-service/src/main/kotlin/org/archguard/arch/LeafManger.kt
            @@ -2,7 +2,7 @@
             
             import com.thoughtworks.archguard.code.module.domain.dubbo.DubboConfigRepository
             import org.archguard.protocol.dubbo.ReferenceConfig
            -import com.thoughtworks.archguard.code.module.domain.dubbo.ServiceConfig
            +import org.archguard.protocol.dubbo.ServiceConfig
             import org.archguard.protocol.dubbo.SubModuleDubbo
             import org.assertj.core.api.Assertions.assertThat
             import org.junit.jupiter.api.Test
        """.trimIndent()

        val postProcess = DiffSimplifier.postProcess(code)
        assertEquals(
            postProcess,
            """rename file from server/src/main/kotlin/com/thoughtworks/archguard/code/module/domain/model/LeafManger.kt to server/metric-service/src/main/kotlin/org/archguard/arch/LeafManger.kt
change import from com.thoughtworks.archguard.code.module.domain.dubbo.ServiceConfig to org.archguard.protocol.dubbo.ServiceConfig"""
        )
    }

    @Test
    fun testHandleForFileChange() {
        val code = """--- a/server/src/test/kotlin/com/thoughtworks/archguard/code/clazz/domain/CodeTreeTest.kt
+++ b/server/src/test/kotlin/com/thoughtworks/archguard/code/clazz/domain/CodeTreeTest.kt	(date 1704769088000)
@@ -1,4 +1,4 @@"""

        val postProcess = DiffSimplifier.postProcess(code)
        assertEquals(
            postProcess,
            """modify file server/src/test/kotlin/com/thoughtworks/archguard/code/clazz/domain/CodeTreeTest.kt"""
        )
    }

    @Test
    fun testHandleForRealWorld() {
        val code = """Index: src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java
===================================================================
diff --git a/src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java b/src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java
--- a/src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java	(revision 9b4b04de55fe5be5372eae67987b27f7d329e1f3)
+++ b/src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java	(revision 768efa678ecb9c044aea7c5c4873ed218357773b)
@@ -6,7 +6,5 @@
 public class BlogCategoryController {
     // devti://story/github/1
 
-    //    银行账户管理
-
     // Close a bank account
 }"""

        val postProcess = DiffSimplifier.postProcess(code)
        assertEquals(
            postProcess,
            """modify file src/main/java/cc/unitmesh/untitled/demo/controller/BlogCategoryController.java
 public class BlogCategoryController {
     // devti://story/github/1
-    //    银行账户管理
-
     // Close a bank account
 }"""
        )
    }
}
