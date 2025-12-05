package cc.unitmesh.devti.language.debugger

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XSuspendContext

class ShireSuspendContext(val process: ShireDebugProcess, project: Project) : XSuspendContext() {
    private val shireExecutionStacks: Array<XExecutionStack> = arrayOf(
        ShireExecutionStack(process, project)
    )

    override fun getActiveExecutionStack(): XExecutionStack? = shireExecutionStacks.firstOrNull()
    override fun getExecutionStacks(): Array<XExecutionStack> = shireExecutionStacks
}