package cc.unitmesh.devti.language.ast.snapshot

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class VariableSnapshotRecorder {
    private val snapshots = mutableListOf<UserCustomVariableSnapshot>()
    private val listeners = mutableListOf<VariableSnapshotListener>()

    fun addSnapshot(variableName: String, value: Any, operation: String? = null, operationArg: Any? = null) {
        val operationList = mutableListOf<VariableOperation>()
        if (operation != null) {
            operationList.add(VariableOperation(operation, System.currentTimeMillis(), operationArg))
        }

        val result = when (value) {
            is Array<*> -> {
                value.joinToString(", ")
            }

            is List<*> -> {
                value.joinToString(", ")
            }

            else -> {
                value.toString()
            }
        }

        snapshots.add(UserCustomVariableSnapshot(variableName, result, operations = operationList))
        listeners.forEach { it.onSnapshot(variableName, result, operationList) }
    }

    fun clear() {
        snapshots.clear()
    }

    fun all(): List<UserCustomVariableSnapshot> {
        return snapshots
    }

    fun addListener(listener: VariableSnapshotListener) {
        listeners.add(listener)
    }

    companion object {
        fun getInstance(project: Project): VariableSnapshotRecorder {
            return project.getService(VariableSnapshotRecorder::class.java)
        }
    }
}

interface VariableSnapshotListener {
    fun onSnapshot(variableName: String, value: String, operations: List<VariableOperation>)
}