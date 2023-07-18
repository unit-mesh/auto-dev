package cc.unitmesh.ide.idea.provider

import org.junit.Test

class JavaAutoDevFlowTest {
    @Test
    fun testShould_extract_controller_name() {
        val controllerName = JavaAutoDevFlow.matchControllerName("返回最合适的 Controller 名字：BlogController")
        assert(controllerName == "BlogController")

        val controllerName2 = JavaAutoDevFlow.matchControllerName("BlogController")
        assert(controllerName2 == "BlogController")

        // 适合的 Controller 名字：VideoController
        val controllerName3 = JavaAutoDevFlow.matchControllerName("适合的 Controller 名字：VideoController")
        assert(controllerName3 == "VideoController")
    }
}
