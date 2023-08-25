package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.client.CoUnitApi
import cc.unitmesh.devti.counit.dto.PayloadType
import cc.unitmesh.devti.counit.dto.QueryResponse
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

    fun queryTool(query: String, hypotheticalDocument: String): Pair<QueryResponse?, QueryResponse?> {
        val normalQuery: QueryResponse? = service.query(query, PayloadType.OpenApi).execute().body()
        val hydeDoc: QueryResponse? = service.query(hypotheticalDocument, PayloadType.OpenApi).execute().body()

        return Pair(normalQuery, hydeDoc)
    }
}