package cc.unitmesh.devti.connector.openai

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import com.intellij.openapi.util.NlsSafe
import java.io.InputStream

class PromptGenerator {
    fun storyDetail(project: SimpleProjectInfo, story: String): String {
        val promptText: InputStream = getResource("create_story_detail")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{project}", project.name + ":" + project.description)
            .replace("{story}", story)
    }

    fun createEndpoint(storyDetail: String, files: List<DtClass>): String {
        val promptText: InputStream = getResource("lookup_or_create_endpoint")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllers}", files.map { it.name }.joinToString(","))
            .replace("{storyDetail}", storyDetail)
    }

    private fun getResource(fileName: String): InputStream? =
        this::class.java.classLoader.getResourceAsStream("prompts/openai/$fileName.txt")

    fun updateControllerMethod(targetClazz: DtClass, storyDetail: String): String {
        val promptText: InputStream = getResource("update_controller_method")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllerName}", targetClazz.name)
            .replace("{controllers}", targetClazz.format())
            .replace("{storyDetail}", storyDetail)
    }

    fun codeComplete(methodCode: String, className: @NlsSafe String?): String {
        val promptText: InputStream = getResource("copilot/code_complete")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", methodCode)
            .replace("{className}", className ?: "")
    }

    fun autoComment(methodCode: String): String {
        val promptText: InputStream = getResource("copilot/code_comments")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", methodCode)
    }

    fun codeReview(text: String): String {
        val promptText: InputStream = getResource("copilot/code_review")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", text)
    }

    fun findBug(text: String): String {
        val promptText: InputStream = getResource("copilot/find_bug")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", text)
    }
}
