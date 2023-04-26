package cc.unitmesh.devti.connector.custom

import cc.unitmesh.devti.connector.CodeCopilot
import cc.unitmesh.devti.settings.DevtiSettingsState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class CustomConnector : CodeCopilot {
    val devtiSettingsState = DevtiSettingsState.getInstance()

    val url = devtiSettingsState?.customEngineServer ?: ""
    val key = devtiSettingsState?.customEngineToken ?: ""
    var promptConfig: PromptConfig? = null

    init {
        val prompts = devtiSettingsState?.customEnginePrompts
        try {
            if (prompts != null) {
                promptConfig = Json.decodeFromString(prompts)
            }
        } catch (e: Exception) {
            println("Error parsing prompts: $e")
        }

        if (promptConfig == null) {
            promptConfig = PromptConfig(
                PromptItem("Auto complete", "{code}"),
                PromptItem("Auto comment", "{code}"),
                PromptItem("Code review", "{code}"),
                PromptItem("Find bug", "{code}")
            )
        }
    }

    private fun prompt(instruction: String, input: String): String {
//        val retrofit = Retrofit.Builder()
//            .baseUrl(url)
//            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//            .build()

        return ""
    }

    override fun codeCompleteFor(text: String, className: String): String {
        val complete = promptConfig!!.autoComplete
        return prompt(complete.instruction, complete.input.replace("{code}", text))
    }

    override fun autoComment(text: String): String {
        val comment = promptConfig!!.autoComment
        return prompt(comment.instruction, comment.input.replace("{code}", text))
    }

    override fun codeReviewFor(text: String): String {
        val review = promptConfig!!.codeReview
        return prompt(review.instruction, review.input.replace("{code}", text))
    }

    override fun findBug(text: String): String {
        val bug = promptConfig!!.findBug
        return prompt(bug.instruction, bug.input.replace("{code}", text))
    }

}