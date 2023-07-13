package cc.unitmesh.devti.connector.openai

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import cc.unitmesh.devti.prompting.PromptConfig
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
            .replace("{controllers}", files.joinToString(",") { it.name })
            .replace("{storyDetail}", storyDetail)
    }

    private fun getResource(fileName: String): InputStream? =
        this::class.java.classLoader.getResourceAsStream("prompts/openai/$fileName.txt")

    fun createDtoAndEntity(storyDetail: String, files: List<DtClass>): String {
        val promptText: InputStream = getResource("create_dto_and_entity")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{entityList}", files.joinToString(",") { it.name })
            .replace("{storyDetail}", storyDetail)
    }

    fun updateControllerMethod(targetClazz: DtClass, storyDetail: String): String {
        val promptText: InputStream = getResource("update_controller_method")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        val spec = PromptConfig.load().spec["controller"]

        return promptTextString
            .replace("{controllerName}", targetClazz.name)
            .replace("{controllers}", targetClazz.format())
            .replace("{storyDetail}", storyDetail)
            .replace("{spec}", spec ?: "")
    }

    fun autoComment(methodCode: String): String {
        val promptText: InputStream = getResource("copilot/code_comments")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", methodCode)
    }

    fun findBug(text: String): String {
        val promptText: InputStream = getResource("copilot/find_bug")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", text)
    }
}
