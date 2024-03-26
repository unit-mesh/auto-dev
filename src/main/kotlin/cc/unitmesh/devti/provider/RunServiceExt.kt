// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.provider

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import java.util.concurrent.CountDownLatch

class CheckExecutionListener(
    private val executorId: String,
    private val context: Context,
) : ExecutionListener {
    override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processStartScheduled(executorId, env)
        }
    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            context.latch.countDown()
            context.executionListener?.processNotStarted(executorId, env)
        }
    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processStarting(executorId, env)
        }
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processStarted(executorId, env, handler)
        }
    }

    override fun processTerminating(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processTerminating(executorId, env, handler)
        }
    }

    override fun processTerminated(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int
    ) {
        checkAndExecute(executorId, env) {
            context.executionListener?.processTerminated(executorId, env, handler, exitCode)
        }
    }

    private fun checkAndExecute(executorId: String, env: ExecutionEnvironment, action: () -> Unit) {
        if (this.executorId == executorId && env in context.environments) {
            action()
        }
    }
}

class Context(
    val processListener: ProcessListener?,
    val executionListener: ExecutionListener?,
    val latch: CountDownLatch
) : Disposable {
    val environments: MutableList<ExecutionEnvironment> = mutableListOf()
    override fun dispose() {}
}