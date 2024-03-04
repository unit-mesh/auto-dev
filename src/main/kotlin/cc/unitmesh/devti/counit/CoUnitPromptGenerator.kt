package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.client.CoUnitApi
import cc.unitmesh.devti.counit.dto.ExplainQuery
import cc.unitmesh.devti.counit.dto.PayloadType
import cc.unitmesh.devti.counit.dto.QueryResponse
import cc.unitmesh.devti.counit.dto.QueryResult
import cc.unitmesh.devti.counit.configurable.coUnitSettings
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

    fun semanticQuery(query: ExplainQuery): QueryResult {
        val englishQuery: QueryResponse? = service.query(query.query, PayloadType.OpenApi).execute().body()
        val hydeDoc: QueryResponse? = service.query(query.hypotheticalDocument, PayloadType.OpenApi).execute().body()
        val naturalLangQuery: QueryResponse? = service.query(query.natureLangQuery, PayloadType.OpenApi).execute().body()

        return QueryResult(
            englishQuery?.data ?: emptyList(),
            naturalLangQuery?.data ?: emptyList(),
            hydeDoc?.data ?: emptyList()
        )
    }
}