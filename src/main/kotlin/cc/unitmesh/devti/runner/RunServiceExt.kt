// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.runner

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment

class CheckExecutionListener(
    private val executorId: String,
    private val runContext: RunContext,
) : ExecutionListener {
    override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            runContext.executionListener?.processStartScheduled(executorId, env)
        }
    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            runContext.latch.countDown()
            runContext.executionListener?.processNotStarted(executorId, env)
        }
    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
        checkAndExecute(executorId, env) {
            runContext.executionListener?.processStarting(executorId, env)
        }
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        checkAndExecute(executorId, env) {
            runContext.executionListener?.processStarted(executorId, env, handler)
        }
    }

    override fun processTerminating(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        checkAndExecute(executorId, env) {
            runContext.executionListener?.processTerminating(executorId, env, handler)
        }
    }

    override fun processTerminated(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int
    ) {
        checkAndExecute(executorId, env) {
            runContext.executionListener?.processTerminated(executorId, env, handler, exitCode)
        }
    }

    private fun checkAndExecute(executorId: String, env: ExecutionEnvironment, action: () -> Unit) {
        if (this.executorId == executorId && env in runContext.environments) {
            action()
        }
    }
}
