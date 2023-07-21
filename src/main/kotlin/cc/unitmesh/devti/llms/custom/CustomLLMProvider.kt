package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.llms.CodeCopilotProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.prompting.model.PromptConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request


@Service(Service.Level.PROJECT)
class CustomLLMProvider(val project: Project) : CodeCopilotProvider {
    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private val url = autoDevSettingsState.customEngineServer
    private val key = autoDevSettingsState.customEngineToken
    private var promptConfig: PromptConfig? = null
    private var client = OkHttpClient()

    init {
        val prompts = autoDevSettingsState.customEnginePrompts
        promptConfig = PromptConfig.tryParse(prompts)
    }

    private val logger = Logger.getInstance(CustomLLMProvider::class.java)


    override fun prompt(promptText: String): String {
        return this.prompt(promptText, "")
    }

    fun prompt(instruction: String, input: String): String {
        val body = okhttp3.RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            """
                {
                    "instruction": "$instruction",
                    "input": "$input",
                }
            """.trimIndent()
        )

        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
        }

        val request = builder
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            logger.error("$response")
            return ""
        }

        return response.body?.string() ?: ""
    }

    override fun autoComment(text: String): String {
        val comment = promptConfig!!.autoComment
        return prompt(comment.instruction, comment.input.replace("{code}", text))
    }

    override fun findBug(text: String): String {
        val bug = promptConfig!!.refactor
        return prompt(bug.instruction, bug.input.replace("{code}", text))
    }

}