package cc.unitmesh.devti

import cc.unitmesh.devti.flow.AutoDevFlow
import org.junit.Test

class AutoDevFlowTest {
    @Test
    fun testShould_extract_controller_name() {
        val controllerName = AutoDevFlow.matchControllerName("返回最合适的 Controller 名字：BlogController")
        assert(controllerName == "BlogController")

        val controllerName2 = AutoDevFlow.matchControllerName("BlogController")
        assert(controllerName2 == "BlogController")

        // 适合的 Controller 名字：VideoController
        val controllerName3 = AutoDevFlow.matchControllerName("适合的 Controller 名字：VideoController")
        assert(controllerName3 == "VideoController")
    }
}
