package cc.unitmesh.devti.prompt.openai

import cc.unitmesh.devti.kanban.SimpleProjectInfo

class GptPromptText(val project: SimpleProjectInfo) {
    fun fillStoryDetail(story: String): String {
        return ""
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
