package cc.unitmesh.idea.provider

import cc.unitmesh.idea.flow.JvmAutoDevFlow
import org.junit.Test

class JvmAutoDevFlowTest {
    @Test
    fun testShould_extract_controller_name() {
        val controllerName = JvmAutoDevFlow.matchControllerName("返回最合适的 Controller 名字：BlogController")
        assert(controllerName == "BlogController")

        val controllerName2 = JvmAutoDevFlow.matchControllerName("BlogController")
        assert(controllerName2 == "BlogController")

        // 适合的 Controller 名字：VideoController
        val controllerName3 = JvmAutoDevFlow.matchControllerName("适合的 Controller 名字：VideoController")
        assert(controllerName3 == "VideoController")
    }
}
