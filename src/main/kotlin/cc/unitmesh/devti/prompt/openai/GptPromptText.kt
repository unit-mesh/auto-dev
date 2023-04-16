package cc.unitmesh.devti.prompt.openai

import cc.unitmesh.devti.kanban.SimpleProjectInfo
import java.io.InputStream

class GptPromptText() {
    // 1. read resources/prompts/create_story_detail.txt
    // 2. replace {project} with project name
    fun fillStoryDetail(project: SimpleProjectInfo, story: String): String {
        val promptText: InputStream = this::class.java.classLoader.getResourceAsStream("prompts/create_story_detail.txt")!!
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
}
