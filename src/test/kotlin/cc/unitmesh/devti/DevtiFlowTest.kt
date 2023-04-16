package cc.unitmesh.devti

import cc.unitmesh.devti.analysis.JavaCrudProcessor
import cc.unitmesh.devti.kanban.impl.GitHubIssue
import cc.unitmesh.devti.prompt.openai.OpenAIAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import io.github.cdimascio.dotenv.dotenv
import org.junit.Ignore

class DevtiFlowTest: LightPlatformTestCase() {

    @Test
    @Ignore
    fun testShould_fetch_github_story() {
        val dotenv = dotenv()
        val githubToken = dotenv["GITHUB_TOKEN"]
        val openAIKey = dotenv["OPENAI_KEY"]

        val gitHubIssue = GitHubIssue("unit-mesh/untitled", githubToken!!)
        val openAIAction = OpenAIAction(openAIKey!!, "gpt-3.5-turbo")
        val devtiFlow = DevtiFlow(gitHubIssue, openAIAction)
        devtiFlow.start("1")
    }
}
