package cc.unitmesh.devti.language.debugger

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.*
import cc.unitmesh.devti.language.debugger.snapshot.UserCustomVariableSnapshot
import cc.unitmesh.devti.language.debugger.snapshot.VariableSnapshotRecorder
import org.jetbrains.concurrency.Promise


class ShireExecutionStack(private val process: ShireDebugProcess, project: Project) :
    XExecutionStack("Variables") {
    private val stackFrames: MutableList<ShireStackFrame> = mutableListOf()

    init {
        stackFrames.add(ShireStackFrame(process, project, null))
        val variableSnapshots = VariableSnapshotRecorder.getInstance(project).all()
        variableSnapshots.forEach {
            stackFrames.add(ShireStackFrame(process, project, it))
        }

        stackFrames.reverse()
    }

    override fun getTopFrame(): XStackFrame? = stackFrames.firstOrNull()

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        container.addStackFrames(stackFrames, true)
    }
}

class ShireStackFrame(
    val process: ShireDebugProcess,
    val project: Project,
    private val snapshot: UserCustomVariableSnapshot? = null,
) : XStackFrame(), Disposable {
    private var snapshotValue: ShireDebugValue? = null
    override fun customizePresentation(component: ColoredTextContainer) {
        if (snapshot == null) {
            component.append("Init", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.setIcon(AllIcons.Debugger.Frame)
            return
        }

        val variableOperation = snapshot.operations.firstOrNull()
        if (variableOperation == null) {
            component.append(
                snapshot.variableName + " -> " + "Init" + "(" + ")",
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
            )
            component.setIcon(AllIcons.Debugger.Frame)
        } else {
            val functionName = variableOperation.functionName
            val value = variableOperation.value.toString()
            snapshotValue = ShireDebugValue(snapshot.variableName, "String", value, snapshot)
            component.append(
                snapshot.variableName + " -> " + functionName + "(" + value + ")",
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
            )
            component.setIcon(AllIcons.Debugger.Frame)
        }
    }

    override fun computeChildren(node: XCompositeNode) {
        val root = XValueChildrenList()
        snapshotValue?.let {
            root.add(it)
        }

        node.addChildren(root, false)

        val filteredChildren = XValueChildrenList()
        process.shireRunnerContext?.compiledVariables?.forEach {
            filteredChildren.add(ShireDebugValue(it.key, "String", it.value.toString()))
        }

        node.addChildren(filteredChildren, true)
    }

    override fun getEvaluator(): XDebuggerEvaluator? {
        return ShireDebugEvaluator()
    }

    override fun dispose() {

    }
}

class ShireDebugEvaluator : XDebuggerEvaluator() {
    override fun evaluate(expr: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val expression = expr.trim()
            if (expression.isEmpty()) {
                callback.evaluated(getNone())
                return@executeOnPooledThread
            }


            val value = ShireDebugValue(expression, "String", expression)
            callback.evaluated(value)
        }
    }

    private fun getNone(): XValue = ShireDebugValue("", "None", "")
}

class ShireDebugValue(
    private val myName: String,
    val type: String = "String",
    val value: String,
    val snapshot: UserCustomVariableSnapshot? = null,
) : XNamedValue(myName) {
    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        if (snapshot != null) {
            node.setPresentation(AllIcons.Debugger.Value, myName, value, true)
            return
        }

        node.setPresentation(AllIcons.Debugger.Value, myName, value, false)
    }

    override fun calculateEvaluationExpression(): Promise<XExpression> {
        return super.calculateEvaluationExpression()
    }
}
