package cc.unitmesh.devti.embedding

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlin.collections.indices
import kotlin.collections.slice
import kotlin.collections.toLongArray
import kotlin.ranges.until
import kotlin.to
import kotlinx.coroutines.*

class LocalEmbedding(
    private val tokenizer: HuggingFaceTokenizer,
    private val session: OrtSession,
    private val env: OrtEnvironment,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val embeddingContext = Dispatchers.Default.limitedParallelism(1)

    @RequiresBackgroundThread
    suspend fun embed(input: String): FloatArray {
        return withContext(embeddingContext) {
            embedInternal(input)
        }
    }

    fun embedInternal(input: String, dimensions: Int = 512): FloatArray {
        val tokenized = tokenizer.encode(input, true, true)

        var inputIds = tokenized.ids
        var attentionMask = tokenized.attentionMask
        var typeIds = tokenized.typeIds

        if (tokenized.ids.size >= dimensions) {
            inputIds = inputIds.slice(0 until dimensions).toLongArray()
            attentionMask = attentionMask.slice(0 until dimensions).toLongArray()
            typeIds = typeIds.slice(0 until dimensions).toLongArray()
        }

        val tensorInput = OrtUtil.reshape(inputIds, longArrayOf(1, inputIds.size.toLong()))
        val tensorAttentionMask = OrtUtil.reshape(attentionMask, longArrayOf(1, attentionMask.size.toLong()))
        val tensorTypeIds = OrtUtil.reshape(typeIds, longArrayOf(1, typeIds.size.toLong()))

        val result = session.run(
            mapOf(
                "input_ids" to OnnxTensor.createTensor(env, tensorInput),
                "attention_mask" to OnnxTensor.createTensor(env, tensorAttentionMask),
                "token_type_ids" to OnnxTensor.createTensor(env, tensorTypeIds),
            ),
        )

        val outputTensor: OnnxTensor = result.get(0) as OnnxTensor

        val floatArray = outputTensor.floatBuffer.array()
        // floatArray is an inputIds.size * 384 array, we need to mean it to 384 * 1
        // 1, shape, shape.length
        val shapeSize = outputTensor.info.shape[2].toInt()
        val meanArray = FloatArray(shapeSize)
        for (i in 0 until shapeSize) {
            var sum = 0f
            for (j in inputIds.indices) {
                sum += floatArray[j * shapeSize + i]
            }

            meanArray[i] = sum / inputIds.size
        }

        return meanArray
    }

    companion object {
        /**
         * Create a new instance of [LocalEmbedding] with default model.
         * We use official model: [all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)
         * We can use [optimum](https://github.com/huggingface/optimum) to transform the model to onnx.
         */
        fun create(): LocalEmbedding? {
            val currentThread = Thread.currentThread()
            val originalClassLoader = currentThread.contextClassLoader
            val pluginClassLoader = Companion::class.java.classLoader

            return try {
                currentThread.setContextClassLoader(pluginClassLoader);

                val tokenizerStream = pluginClassLoader.getResourceAsStream("model/tokenizer.json")
                val tokenizer = HuggingFaceTokenizer.newInstance(tokenizerStream, null)

                val ortEnv = OrtEnvironment.getEnvironment()
                val sessionOptions = OrtSession.SessionOptions()

                val onnxStream = pluginClassLoader.getResourceAsStream("model/model.onnx")!!
                // load onnxPath as byte[]
                val onnxPathAsByteArray = onnxStream.readAllBytes()
                val session = ortEnv.createSession(onnxPathAsByteArray, sessionOptions)

                return LocalEmbedding(tokenizer, session, ortEnv)
            } catch (e: Exception) {
                e.printStackTrace()
                currentThread.setContextClassLoader(originalClassLoader)
                null
            }
        }
    }
}
