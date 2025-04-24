package cc.unitmesh.httpclient.converter

import com.intellij.httpClient.execution.RestClientRequest
import com.intellij.httpClient.http.request.HttpRequestHeaderFields
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object CUrlConverter {
    fun convert(request: RestClientRequest): Request {
        val builder = Request.Builder()

        builder.url(request.buildFullUrl())
        request.headers.forEach {
            try {
                builder.header(it.key, it.value)
            } catch (e: IllegalArgumentException) {
                // ignore
            }
        }

        val body = request.textToSend

        val mediaType = request
            .getHeadersValue(HttpRequestHeaderFields.CONTENT_TYPE)
            ?.firstOrNull()
            ?.toMediaTypeOrNull()

        when (request.httpMethod) {
            "GET" -> builder.get()
            "POST" -> builder.post(body.toRequestBody(mediaType))
            "PUT" -> builder.put(body.toRequestBody(mediaType))
            "DELETE" -> builder.delete(body.toRequestBody(mediaType))
            else -> builder.method(request.httpMethod, body.toRequestBody(mediaType))
        }

        return builder.build()
    }
}
