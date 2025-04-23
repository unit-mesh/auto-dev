package cc.unitmesh.devti.language.run

import com.intellij.build.process.BuildProcessHandler
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import java.io.*
import java.nio.channels.Channels
import java.nio.channels.Pipe

class ShireProcessHandler(private val myExecutionName: String) : BuildProcessHandler() {
    private var myProcessInputWriter: OutputStream? = null
    private var myProcessInputReader: InputStream? = null
    private val myAnsiEscapeDecoder = AnsiEscapeDecoder()

    init {
        try {
            val pipe = Pipe.open()
            myProcessInputReader = BufferedInputStream(Channels.newInputStream(pipe.source()))
            myProcessInputWriter = BufferedOutputStream(Channels.newOutputStream(pipe.sink()))
        } catch (_: IOException) {

        }

    }

    override fun detachIsDefault(): Boolean = true
    override fun destroyProcessImpl() = Unit
    override fun detachProcessImpl() {
        try {
            notifyProcessDetached()
        } catch (e: Exception) {
            // ignore
            logger<ShireProcessHandler>().warn(e)
        }
        finally {
            notifyProcessTerminated(0)
        }
    }

    fun exitWithError() = notifyProcessTerminated(-1)

    override fun notifyTextAvailable(text: String, outputType: Key<*>) {
        myAnsiEscapeDecoder.escapeText(
            text, outputType
        ) { decodedText: String?, attributes: Key<*>? ->
            super.notifyTextAvailable(
                decodedText!!, attributes!!
            )
        }
    }

    override fun getProcessInput(): OutputStream? = myProcessInputWriter
    override fun getExecutionName(): String = myExecutionName
}
