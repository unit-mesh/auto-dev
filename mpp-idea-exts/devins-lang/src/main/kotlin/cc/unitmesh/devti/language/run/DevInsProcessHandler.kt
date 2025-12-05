package cc.unitmesh.devti.language.run

import com.intellij.build.process.BuildProcessHandler
import java.io.OutputStream

class DevInsProcessHandler(private val myExecutionName: String) : BuildProcessHandler() {
    override fun detachIsDefault(): Boolean = false
    override fun destroyProcessImpl() = Unit
    override fun detachProcessImpl() = notifyProcessTerminated(0)
    fun exitWithError() = notifyProcessTerminated(-1)

    override fun getProcessInput(): OutputStream? = null
    override fun getExecutionName(): String = myExecutionName
}