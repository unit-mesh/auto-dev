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
import kotlinx.serialization.json.Json
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

    /**
     * 获取GitHub Copilot支持的模型列表
     *
     * @param client HTTP客户端
     * @param forceRefresh 是否强制刷新缓存的模型列表
     * @return 支持的模型列表，如果获取失败则返回null
     */
    fun getSupportedModels(client: OkHttpClient, forceRefresh: Boolean = false): CopilotModelsResponse? {
        val currentApiToken = apiToken ?: requestApiToken(client) ?: return null

        // 如果已经有缓存的模型列表且不需要强制刷新，则直接返回缓存的结果
        if (!forceRefresh && supportedModels != null) {
            return supportedModels
        }

        val request = Request.Builder()
            .url("https://api.githubcopilot.com/models")
            .addHeader("Authorization", "Bearer ${currentApiToken.apiKey}")
            .addHeader("Editor-Version", "Neovim/0.6.1")
            .addHeader("Content-Type", "application/json")
            .addHeader("Copilot-Integration-Id", "vscode-chat")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: throw IllegalStateException("响应体为空")
                // 使用kotlinx.serialization解析JSON响应
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<CopilotModelsResponse>(responseBody).also {
                    supportedModels = it
                }
            } else {
                logger.warn("获取支持的模型列表失败: ${response.code}")
                null
            }
        } catch (e: Exception) {
            logger.warn("获取支持的模型列表时发生异常", e)
            null
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

//    private suspend fun monitorConfigFiles() {
//        val configDir = getConfigDir() // ~/.config/github-copilot 或类似路径
//        val configFiles = setOf(
//            configDir.resolve("apps.json")
//        )
//
//        // 监听配置目录变更
//        val changes = watchConfigDir(configDir, configFiles)
//
//        // 处理变更事件
//        changes.collect { _ ->
//            val token = extractOauthToken()
//            oauthToken = token
//            // TODO: 这里可以添加逻辑来处理令牌的更新，例如通知其他组件
////                notifyListeners()
//        }
//    }
    // 监听 [dir] 变化，返回一个发出文件内容变更的Flow
//    private fun watchConfigDir(dir: File, files: Set<File>): Flow<Map<File, String>> = flow {
//        val messageBus = project?.messageBus ?: return@flow
//
//        // 确保目录存在
//        if (!dir.exists()) {
//            dir.mkdirs()
//        }
//
//        val connection = messageBus.connect()
//        try {
//            // 创建一个MutableStateFlow用于存储当前文件内容
//            val contentsFlow = MutableStateFlow(readInitialContents(files))
//
//            // 监听文件系统变化
//            connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
//                override fun after(events: List<VFileEvent>) {
//                    val changedFiles = events
//                        .filter { event ->
//                            val file = event.file
//                            file != null && files.any { it.path == file.path }
//                        }
//                        .mapNotNull { it.file }
//
//                    if (changedFiles.isNotEmpty()) {
//                        val updatedContents = contentsFlow.value.toMutableMap()
//                        changedFiles.forEach { vFile ->
//                            val correspondingFile = files.find { it.path == vFile.path }
//                            if (correspondingFile != null) {
//                                try {
//                                    updatedContents[correspondingFile] =
//                                        vFile.contentsToByteArray().toString(Charsets.UTF_8)
//                                } catch (e: Exception) {
//                                    Logger.getInstance(GithubCopilotProvider::class.java)
//                                        .warn("读取文件内容失败: ${vFile.path}", e)
//                                }
//                            }
//                        }
//                        contentsFlow.value = updatedContents
//                    }
//                }
//            })
//
//            // 收集内容变更并发出
//            contentsFlow.collect { emit(it) }
//        } finally {
//            connection.disconnect()
//        }
//    }
//
//    // 读取初始文件内容
//    private fun readInitialContents(files: Set<File>): Map<File, String> {
//        return files.associateWith { file ->
//            try {
//                if (file.exists()) {
//                    file.readText()
//                } else {
//                    ""
//                }
//            } catch (e: Exception) {
//                Logger.getInstance(GithubCopilotProvider::class.java)
//                    .warn("读取文件内容失败: ${file.path}", e)
//                ""
//            }
//        }
//    }
}

