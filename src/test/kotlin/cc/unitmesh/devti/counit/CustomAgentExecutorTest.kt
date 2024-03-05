package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.model.CustomAgentConfig
import com.intellij.testFramework.LightPlatformTestCase


class CustomAgentExecutorTest : LightPlatformTestCase() {
    fun testExecute() {
        val customAgentExecutor = CustomAgentExecutor(project)
//        val response = customAgentExecutor.execute(
//            "test",
//            CustomAgentConfig("test", "test", "http://127.0.0.1:8765/api/agent/market")
//        )

//        assertEquals("test", response)
    }
}