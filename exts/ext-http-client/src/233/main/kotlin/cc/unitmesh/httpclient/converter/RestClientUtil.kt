package cc.unitmesh.httpclient.converter

import com.intellij.httpClient.execution.RestClientRequest
import com.intellij.httpClient.execution.auth.HttpClientAuthData
import com.intellij.httpClient.execution.auth.HttpRequestAuthCredentials
import com.intellij.httpClient.execution.auth.HttpRequestAuthScope
import com.intellij.httpClient.execution.auth.HttpRequestCommonAuthSchemes
import com.intellij.httpClient.http.request.psi.impl.HttpRequestPsiImplUtil
import com.intellij.openapi.util.text.StringUtil
import java.net.URL

fun RestClientRequest.buildFullUrl(): URL {
    val url = URL(getFullUri(this.url, this))
    if (url.userInfo == null) {
        return url
    }
    val userInfo = url.userInfo ?: throw IllegalStateException("getUserInfo(...) returned null")
    val usernameAndPassword = userInfo.split(":")
    val httpRequestAuthScope = HttpRequestAuthScope(HttpRequestCommonAuthSchemes.BASIC)
    val username = usernameAndPassword[0]
    val password = usernameAndPassword.getOrNull(1) ?: ""
    this.authData =
        HttpClientAuthData(httpRequestAuthScope, HttpRequestAuthCredentials.UsernamePassword(username, password))

    val fullUrl = buildString {
        url.protocol?.let {
            append(it).append(HttpRequestPsiImplUtil.SCHEME_SEPARATOR)
        }
        url.host?.let {
            append(it)
        }
        val port = url.port.takeIf { it > 0 }
        port?.let {
            append(":").append(it)
        }
        url.path?.let {
            append("/").append(it)
        }
    }

    return URL(fullUrl)
}

fun getFullUri(uri: String, request: RestClientRequest): String {
    var uriStr = uri
    if (request.parametersEnabled) {
        val query = request.createQueryString()
        if (StringUtil.isNotEmpty(query)) {
            uriStr = uriStr + (if (uriStr.contains("?")) "&" else "?") + query
        }
    }

    return uriStr
}
