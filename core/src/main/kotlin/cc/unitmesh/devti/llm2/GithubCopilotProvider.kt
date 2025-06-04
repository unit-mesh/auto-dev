package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llm2.model.CopilotModelsResponse
import cc.unitmesh.devti.llms.custom.CustomRequest
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.llms.custom.updateCustomFormat
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File


private data class ApiToken(
    val apiKey: String,
    val expiresAt: Instant, // 使用某种 DateTime 类型
) {
    // 计算令牌的剩余有效时间（秒）
    fun remainingSeconds(): Long {
        val now = Clock.System.now()
        return expiresAt.epochSeconds - now.epochSeconds
    }
}

private object GithubOAuthProvider {
    private val logger = Logger.getInstance(GithubOAuthProvider::class.java)

    private var oauthToken: String? = null
    private var apiToken: ApiToken? = null
    private var supportedModels: CopilotModelsResponse? = null

    private fun extractOauthToken(): String? {
        val configDir = getConfigDir()
        val appsFile = configDir.resolve("apps.json")

        if (!appsFile.exists()) {
            return null
        }

        return appsFile.readText().let {
            val arr: List<String>? = JsonPath.parse(it)?.read("$..oauth_token")
            arr?.lastOrNull()
        }
    }

    fun requestApiToken(client: OkHttpClient): ApiToken? {
        if (oauthToken == null) {
            oauthToken = extractOauthToken()
        }
        if (oauthToken == null) {
            logger.warn("oauthToken not exists")
            return null
        }

        val currentApiToken = apiToken
        if (currentApiToken != null && currentApiToken.remainingSeconds() >= 5 * 60) {
            return currentApiToken
        }

        val request = Request.Builder()
            .url("https://api.github.com/copilot_internal/v2/token")
            .addHeader("Authorization", "token $oauthToken")
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: throw IllegalStateException("响应体为空")
            val tokenResponse = JsonPath.parse(responseBody) ?: throw IllegalStateException("解析响应失败")
            val apiKey: String = tokenResponse.read("$.token") ?: throw IllegalStateException("解析 token 失败")
            val expiresAt: Int =
                tokenResponse.read("$.expires_at") ?: throw IllegalStateException("解析 expiresAt 失败")
            return ApiToken(
                apiKey = apiKey,
                expiresAt = Instant.fromEpochSeconds(expiresAt.toLong())
            ).also {
                apiToken = it
            }
        } else {
            val errorBody = response.body?.string() ?: throw IllegalStateException("响应体为空")
            throw IllegalStateException("获取 API 令牌失败: $errorBody")
        }
    }

    private fun getConfigDir(): File {
        // mac only TODO: windows

        // 获取用户的 home 目录
        val homeDir = System.getProperty("user.home")
        return File("${homeDir}/.config/github-copilot/")
    }
}

class GithubCopilotProvider(
    requestCustomize: String,
    responseResolver: String,
    project: Project? = null,
) : LLMProvider2(project, requestCustomize = requestCustomize, responseResolver = responseResolver) {

    override fun textComplete(
        session: ChatSession<Message>,
        stream: Boolean,
    ): Flow<SessionMessageItem<Message>> = callbackFlow {

        val apiToken = GithubOAuthProvider.requestApiToken(client) ?: throw IllegalStateException("获取 API 令牌失败")
        val customRequest = CustomRequest(session.chatHistory.map {
            val cm = it.chatMessage
            Message(cm.role, cm.content)
        })
        val requestBodyText = customRequest.updateCustomFormat(requestCustomize)
        val content = requestBodyText.toByteArray()
        val requestBody = content.toRequestBody("application/json".toMediaTypeOrNull(), 0, content.size)
        val request = Request.Builder()
            .url("https://api.githubcopilot.com/chat/completions")
            .addHeader("Authorization", "Bearer ${apiToken.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Editor-Version", "Zed/Unknow")
            .addHeader("Copilot-Integration-Id", "vscode-chat")
            .post(requestBody)
            .build()

        if (stream) {
            sseStream(
                client,
                request,
                onFailure = {
                    close(it)
                },
                onClosed = {
                    close()
                },
                onEvent = {
                    trySend(it)
                }
            )
        } else {
            kotlin.runCatching {
                val result = directResult(client, request)
                trySend(result)
                close()
            }.onFailure {
                close(it)
            }
        }
        awaitClose()
    }


    private val client: OkHttpClient = OkHttpClient.Builder()
        .build()
}

