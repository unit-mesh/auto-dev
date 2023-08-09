package cc.unitmesh.devti.llms.openai

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import cc.unitmesh.devti.prompting.CodePromptText
import cc.unitmesh.devti.custom.CustomPromptConfig
import com.intellij.openapi.util.NlsSafe
import java.io.InputStream

class PromptTemplate {
    fun storyDetail(project: SimpleProjectInfo, story: String): String {
        val promptText: InputStream = getResource("create_story_detail")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{project}", project.name + ":" + project.description)
            .replace("{story}", story)
    }

    fun createEndpoint(storyDetail: String, controllers: List<DtClass>): String {
        val promptText: InputStream = getResource("lookup_or_create_endpoint")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllers}", controllers.joinToString(",") { it.name })
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

    fun createOrUpdateControllerMethod(
        targetClazz: DtClass,
        storyDetail: String,
        models: List<DtClass>,
        services: List<DtClass>,
        isNewController: Boolean
    ): String {
        val promptText: InputStream = if (isNewController) {
            getResource("create_controller")!!
        } else {
            getResource("update_controller_method")!!
        }

        val promptTextString = promptText.bufferedReader().use { it.readText() }
        val spec = CustomPromptConfig.load().spec["controller"]

        return promptTextString
            .replace("{controllerName}", targetClazz.name)
            .replace("{controllers}", targetClazz.format())
            .replace("{storyDetail}", storyDetail)
            .replace("{models}", models.joinToString("\n", transform = DtClass::formatDto))
            .replace("{services}", services.joinToString("\n", transform = DtClass::format))
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

    fun createServiceAndRepository(controller: @NlsSafe String): String {
        val promptText: InputStream = getResource("create_service_and_repository")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllerCode}", controller)
    }

    fun updateServiceMethod(finalCode: CodePromptText): String {
        val promptText: InputStream = getResource("update_service_method")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{prefixCode}", finalCode.prefixCode)
            .replace("{suffixCode}", finalCode.suffixCode)
    }
}
