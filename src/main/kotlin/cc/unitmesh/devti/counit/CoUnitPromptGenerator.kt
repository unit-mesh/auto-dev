package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.client.CoUnitApi
import cc.unitmesh.devti.settings.configurable.coUnitSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Service(Service.Level.PROJECT)
class CoUnitPromptGenerator(val project: Project) {
    private var retrofit = Retrofit.Builder()
        .baseUrl(project.coUnitSettings.serverAddress)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    var service: CoUnitApi = retrofit.create(CoUnitApi::class.java)

    fun findIntention(input: String): String? {
        val body = service.explainQuery(input).execute().body()
        return body?.prompt
    }

    fun findTool(input: String): String? {
        val body = service.toolPrompter(input).execute().body()
        return body?.prompt
    }
}