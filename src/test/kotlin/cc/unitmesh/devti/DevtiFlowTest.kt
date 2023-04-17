package cc.unitmesh.devti

import org.junit.Test

class DevtiFlowTest {
//
//    @Test
//    @Ignore
//    fun testShould_fetch_github_story() {
//        val dotenv = dotenv()
//        val githubToken = dotenv["GITHUB_TOKEN"]
//        val openAIKey = dotenv["OPENAI_KEY"]
//
//        val gitHubIssue = GitHubIssue("unit-mesh/untitled", githubToken!!)
//        val openAIAction = OpenAIAction(openAIKey!!, "gpt-3.5-turbo")
//        val devtiFlow = DevtiFlow(gitHubIssue, openAIAction)
//        devtiFlow.processAll("1")
//    }

    // input: """返回最合适的 Controller 名字：BlogController""", output: BlogController
    // input: """BlogController""", output: BlogController
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
