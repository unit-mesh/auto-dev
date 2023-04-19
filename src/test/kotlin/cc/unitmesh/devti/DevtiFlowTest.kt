package cc.unitmesh.devti

import org.junit.Test

class DevtiFlowTest {
    @Test
    fun testShould_extract_controller_name() {
        val controllerName = DevtiFlow.matchControllerName("返回最合适的 Controller 名字：BlogController")
        assert(controllerName == "BlogController")

        val controllerName2 = DevtiFlow.matchControllerName("BlogController")
        assert(controllerName2 == "BlogController")

        // 适合的 Controller 名字：VideoController
        val controllerName3 = DevtiFlow.matchControllerName("适合的 Controller 名字：VideoController")
        assert(controllerName3 == "VideoController")
    }
}
