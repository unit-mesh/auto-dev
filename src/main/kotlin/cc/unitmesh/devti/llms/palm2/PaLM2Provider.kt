package cc.unitmesh.devti.llms.palm2

import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.llms.custom.CustomRequest
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class PaLM2Request(val prompt: String, val input: String)

@Service(Service.Level.PROJECT)
class PaLM2Provider(val project: Project) : LLMProvider {
    private val key: String
        get() {
            return AutoDevSettingsState.getInstance().openAiKey
        }
    override fun prompt(input: String): String {
//        val requestContent = Json.encodeToString(CustomRequest(input, input))
//        val body = requestContent.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
//        val builder = Request.Builder()
//            .url("https://generativelanguage.googleapis.com/v1beta2/models/text-bison-001:generateText?key=$key")
//            .post(body)
//        OkHttpClient().newCall(builder.build()).execute().use { response ->
//            if (!response.isSuccessful) throw Exception("Unexpected code $response")
//            return response.body!!.string()
//        }
        TODO()
    }
}
