package cc.unitmesh.devti.runner

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import java.util.concurrent.CountDownLatch

class RunContext(
    val processListener: ProcessListener?,
    val executionListener: ExecutionListener?,
    val latch: CountDownLatch
) : Disposable {
    val environments: MutableList<ExecutionEnvironment> = mutableListOf()
    override fun dispose() {}
}