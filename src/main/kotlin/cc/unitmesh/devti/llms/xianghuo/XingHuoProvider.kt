@file:OptIn(ExperimentalCoroutinesApi::class)

package cc.unitmesh.devti.llms.xianghuo

import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.XingHuoApiVersion
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


@Service(Service.Level.PROJECT)
class XingHuoProvider(val project: Project) : LLMProvider {
    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private val secrectKey: String
        get() = autoDevSettingsState.xingHuoApiSecrect

    private val apiVersion: XingHuoApiVersion
        get() = autoDevSettingsState.xingHuoApiVersion
    private val XingHuoApiVersion.asGeneralDomain
        get() = when (this) {
            XingHuoApiVersion.V1 -> ""
            XingHuoApiVersion.V2 -> "v2"
            else -> "v3"
        }

    private val appid: String
        get() = autoDevSettingsState.xingHuoAppId

    private val apikey: String
        get() = autoDevSettingsState.xingHuoApiKey

    private val hmacsha256Algorithms = "hmacsha256"
    private val uid = UUID.randomUUID().toString().substring(0, 32)

    private val hmacsha256: Mac
        get() {
            val hmac = Mac.getInstance(hmacsha256Algorithms)
            val keySpec = SecretKeySpec(secrectKey.toByteArray(), hmacsha256Algorithms)
            hmac.init(keySpec)
            return hmac
        }

    override fun prompt(promptText: String): String {
        // prompt 接口看似是无用的废弃接口，因为所有 LLM 请求都只能异步返回，不可能直接返回同步结果
        TODO()
    }

    override fun clearMessage() {
        //
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
        if (!keepHistory) {
            clearMessage()
        }

        return callbackFlow {
            val client = OkHttpClient()
            client.newWebSocket(request, MyListener(this, onSocketOpen = {
                val msg = getSendBody(promptText)
                send(msg)
            }))
            awaitClose()
        }
    }


    class MyListener(
        private val producerScope: ProducerScope<String>,
        private val onSocketOpen: WebSocket.() -> Unit,
    ) : WebSocketListener() {

        private var sockedOpen = false
        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.onSocketOpen()
            sockedOpen = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching {
                val element = Json.parseToJsonElement(text)
                val choices = element.jsonObject["payload"]!!.jsonObject["choices"]!!
                val statusCode: Int = choices.jsonObject["status"]?.jsonPrimitive?.int!!
                val message = choices.jsonObject["text"]!!.jsonArray[0]
                val text: String = message.jsonObject["content"]!!.jsonPrimitive.content
                producerScope.trySend(text)
                if (statusCode == 2) { // TODO fix hardcode
                    producerScope.close()
                }
            }.getOrElse { err ->
                producerScope.trySend("onMessage ${err.message}")
                producerScope.close(err)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            producerScope.close()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // WebSocket connection failed
            println("failure ${t.message} ${response?.body} ${response?.message} ${response?.code}")
            producerScope.trySend("onFailure ${response?.body} ${response?.message} ${response?.code}")
            producerScope.close()
        }
    }

    private val request: Request
        get() {
            // https://www.xfyun.cn/doc/spark/general_url_authentication.html#_1-2-%E9%89%B4%E6%9D%83%E5%8F%82%E6%95%B0
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            format.timeZone = TimeZone.getTimeZone("GMT")
            val date: String = format.format(Date())
            val header = """
            |host: spark-api.xf-yun.com
            |date: $date
            |GET /v${apiVersion.value}.1/chat HTTP/1.1
        """.trimMargin()
            val signature = hmacsha256.doFinal(header.toByteArray()).encodeBase64()
            val authorization =
                """api_key="$apikey", algorithm="hmac-sha256", headers="host date request-line", signature="$signature""""

            val params = mapOf(
                "authorization" to authorization.toByteArray().encodeBase64(),
                "date" to date,
                "host" to "spark-api.xf-yun.com"
            )
            val urlBuilder = "https://spark-api.xf-yun.com/v${apiVersion.value}.1/chat".toHttpUrl().newBuilder()
            params.forEach {
                urlBuilder.addQueryParameter(it.key, it.value)
            }
            val url = urlBuilder.build().toString().replace("https://", "wss://")
            println(url)
            return Request.Builder().url(url).build()
        }

    private fun getSendBody(message: String): String {
        return """{
            "header": {
                "app_id": "$appid",
                "uid": "$uid"
            },
            "parameter": {
                "chat": {
                    "domain": "general${apiVersion.asGeneralDomain}",
                    "temperature": 0.5,
                    "max_tokens": 1024
                }
            },
            "payload": {
                "message": {
                    "text": [
                        {"role": "user", "content": "${message.replace("\n", "\\n")}"}
                    ]
                }
            }
        }""".trimIndent()
    }
}

private fun ByteArray.encodeBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}
