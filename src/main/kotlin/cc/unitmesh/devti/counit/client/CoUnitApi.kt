package cc.unitmesh.devti.counit.client

import cc.unitmesh.devti.counit.dto.PayloadType
import cc.unitmesh.devti.counit.dto.PromptResult
import cc.unitmesh.devti.counit.dto.QueryResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CoUnitApi {
    @GET("/api/agent/prompt/explain")
    fun explainQuery(@Query("q") q: String): Call<PromptResult>

    @POST("/api/agent/prompt/functions/matching")
    fun toolPrompter(@Query("q") q: String): Call<PromptResult>

    @GET("/api/query")
    fun query(@Query("q") q: String, @Query("type") payloadType: PayloadType): Call<QueryResponse>
}
