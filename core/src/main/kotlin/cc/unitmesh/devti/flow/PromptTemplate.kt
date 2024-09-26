package cc.unitmesh.devti.flow

import cc.unitmesh.devti.context.model.DtClass
import cc.unitmesh.devti.flow.model.SimpleProjectInfo
import cc.unitmesh.devti.prompting.CodePromptText
import cc.unitmesh.devti.custom.action.CustomPromptConfig
import java.io.InputStream

class PromptTemplate {
    fun storyDetail(project: SimpleProjectInfo, story: String): String {
        val promptText: InputStream = getTemplate("create_story_detail")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{project}", project.name + ":" + project.description)
            .replace("{story}", story)
    }

    fun createEndpoint(storyDetail: String, controllers: List<DtClass>): String {
        val promptText: InputStream = getTemplate("lookup_or_create_endpoint")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllers}", controllers.joinToString(",") { it.name })
            .replace("{storyDetail}", storyDetail)
    }

    private fun getTemplate(fileName: String): InputStream? =
        this::class.java.classLoader.getResourceAsStream("prompts/default/$fileName.vm")

    fun createDtoAndEntity(storyDetail: String, files: List<DtClass>): String {
        val promptText: InputStream = getTemplate("create_dto_and_entity")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        val basePackageName: String = files.firstOrNull {
            it.name.endsWith("Controller")
        }?.packageName ?: ""

        return promptTextString
            .replace("{entityList}", files.joinToString(",") { it.name })
            .replace("{storyDetail}", storyDetail)
            .replace("{basePackageName}", basePackageName)
    }

    fun createOrUpdateControllerMethod(
        targetClazz: DtClass,
        storyDetail: String,
        models: List<DtClass>,
        services: List<DtClass>,
        isNewController: Boolean
    ): String {
        val promptText: InputStream = if (isNewController) {
            getTemplate("create_controller")!!
        } else {
            getTemplate("update_controller_method")!!
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

    fun createServiceAndRepository(controller: String): String {
        val promptText: InputStream = getTemplate("create_service_and_repository")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllerCode}", controller)
    }

    fun updateServiceMethod(finalCode: CodePromptText): String {
        val promptText: InputStream = getTemplate("update_service_method")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{prefixCode}", finalCode.prefixCode)
            .replace("{suffixCode}", finalCode.suffixCode)
    }
}
