package cc.unitmesh.devti.prompt.openai

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.kanban.SimpleProjectInfo
import com.intellij.openapi.util.NlsSafe
import java.io.InputStream

class GptPromptText() {
    // 1. read resources/prompts/create_story_detail.txt
    // 2. replace {project} with project name
    fun fillStoryDetail(project: SimpleProjectInfo, story: String): String {
        val promptText: InputStream =
            this::class.java.classLoader.getResourceAsStream("prompts/create_story_detail.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{project}", project.name + ":" + project.description)
            .replace("{story}", story)
    }

    fun generateControllerCode(story: String): String {
        return "This is a controller code about a ${story}"
    }

    fun generateServiceCode(story: String): String {
        return "This is a service code about a ${story}"
    }

    fun generateModelCode(story: String): String {
        return "This is a model code about a ${story}"
    }

    fun generateRepositoryCode(story: String): String {
        return "This is a repository code about a ${story}"
    }

    fun fillEndpoint(storyDetail: String, files: List<DtClass>): String {
        val promptText: InputStream = this::class.java.classLoader.getResourceAsStream("prompts/create_endpoint.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllers}", files.map { it.name }.joinToString(","))
            .replace("{storyDetail}", storyDetail)
    }

    fun fillUpdateMethod(targetClazz: DtClass, storyDetail: String): String {
        val promptText: InputStream = this::class.java.classLoader.getResourceAsStream("prompts/update_controller_method.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{controllerName}", targetClazz.name)
            .replace("{controllers}", targetClazz.format())
            .replace("{storyDetail}", storyDetail)
    }

    // code complete
    fun fillCodeComplete(methodCode: String, className: @NlsSafe String?): String {
        val promptText: InputStream = this::class.java.classLoader.getResourceAsStream("prompts/code_complete.txt")!!
        val promptTextString = promptText.bufferedReader().use { it.readText() }
        return promptTextString
            .replace("{code}", methodCode)
            .replace("{className}", className ?: "")
    }
}
