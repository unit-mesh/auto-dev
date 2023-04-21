package cc.unitmesh.devti.connector.openai

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.kanban.SimpleProjectInfo
import com.intellij.openapi.util.NlsSafe
import java.io.InputStream

class PromptGenerator() {
    // 1. read resources/prompts/create_story_detail.txt
    // 2. replace {project} with project name
    fun storyDetail(project: SimpleProjectInfo, story: String): String {
        val promptText: InputStream =
            this::class.java.classLoader.getResourceAsStream("prompts/create_story_detail.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{project}", project.name + ":" + project.description)
            .replace("{story}", story)
    }

    fun createEndpoint(storyDetail: String, files: List<DtClass>): String {
        val promptText: InputStream = this::class.java.classLoader.getResourceAsStream("prompts/lookup_or_create_endpoint.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllers}", files.map { it.name }.joinToString(","))
            .replace("{storyDetail}", storyDetail)
    }

    fun updateControllerMethod(targetClazz: DtClass, storyDetail: String): String {
        val promptText: InputStream = this::class.java.classLoader.getResourceAsStream("prompts/update_controller_method.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllerName}", targetClazz.name)
            .replace("{controllers}", targetClazz.format())
            .replace("{storyDetail}", storyDetail)
    }

    fun codeComplete(methodCode: String, className: @NlsSafe String?): String {
        val promptText: InputStream = this::class.java.classLoader.getResourceAsStream("prompts/code_complete.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", methodCode)
            .replace("{className}", className ?: "")
    }

    fun autoComment(methodCode: String): String {
        val promptText: InputStream = this::class.java.classLoader.getResourceAsStream("prompts/code_comments.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", methodCode)
    }
}
