package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.configurable.customAgentSetting
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.time.Duration

@Service(Service.Level.PROJECT)
class CustomAgentHandler(val project: Project) {
    private var client = OkHttpClient()

    fun execute(input: String, selectedAgent: Any): String? {
        val serverAddress = project.customAgentSetting.serverAddress ?: return null
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), input)
        val builder = Request.Builder()

        client = client.newBuilder().build()
        val call = client.newCall(builder.url(serverAddress).post(body).build())

        call.execute().use { response ->
            if (!response.isSuccessful) {
                return null
            }
            return response.body?.string()
        }
    }
}