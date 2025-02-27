package cc.unitmesh.devti.llms.custom

import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class JSONBodyResponseCallback(private val responseFormat: String,private val callback: suspend (String)->Unit): Callback {
    override fun onFailure(call: Call, e: IOException) {
        runBlocking {
            callback("error. ${e.message}")
        }
    }

    override fun onResponse(call: Call, response: Response) {
        val responseBody: String? = response.body?.string()
        if (responseFormat.isEmpty()) {
            runBlocking {
                callback(responseBody ?: "")
            }

            return
        }

        val responseContent: String = JsonPath.parse(responseBody)?.read(responseFormat) ?: ""

        runBlocking() {
            callback(responseContent)
        }
    }
}
