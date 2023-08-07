// MIT License
//
//Copyright (c) [year] [fullname]
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.
package cc.unitmesh.devti.llms.azure

import com.theokanning.openai.OpenAiError
import com.theokanning.openai.OpenAiHttpException
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.SSE
import com.theokanning.openai.service.SSEFormatException
import io.reactivex.FlowableEmitter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.apache.commons.httpclient.HttpException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Callback to parse Server Sent Events (SSE) from raw InputStream and
 * emit the events with io.reactivex.FlowableEmitter to allow streaming of
 * SSE.
 */
class ResponseBodyCallback(private val emitter: FlowableEmitter<SSE>, private val emitDone: Boolean) : Callback {
    override fun onResponse(call: Call, response: Response) {
        var reader: BufferedReader? = null
        try {
            if (!response.isSuccessful) {
                val e = HttpException(response.body.toString())
                val errorBody = response.body
                if (errorBody == null) {
                    throw e
                } else {
                    val error = mapper.readValue(
                        errorBody.string(),
                        OpenAiError::class.java
                    )
                    throw OpenAiHttpException(error, e, e.reasonCode)
                }
            }
            val inputStream = response.body!!.byteStream()
            reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            var line = ""
            var sse: SSE? = null
            while (!emitter.isCancelled && reader.readLine().also { line = it } != null) {
                sse = if (line.startsWith("data:")) {
                    val data = line.substring(5).trim { it <= ' ' }
                    SSE(data)
                } else if (line == "" && sse != null) {
                    if (sse.isDone) {
                        if (emitDone) {
                            emitter.onNext(sse)
                        }
                        break
                    }
                    emitter.onNext(sse)
                    null
                } else {
                    throw SSEFormatException("Invalid sse format! $line")
                }
            }
            emitter.onComplete()
        } catch (t: Throwable) {
            onFailure(call, t as IOException)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    // do nothing
                }
            }
        }
    }

    override fun onFailure(call: Call, e: IOException) {
        emitter.onError(e)
    }

    companion object {
        private val mapper = OpenAiService.defaultObjectMapper()
    }
}
