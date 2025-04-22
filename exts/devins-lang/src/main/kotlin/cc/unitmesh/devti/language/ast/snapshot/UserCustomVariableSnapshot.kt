package cc.unitmesh.devti.language.ast.snapshot

import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolver
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.datetime.Instant

data class SnapshotMetadata(
    val createdAt: Instant,          // 创建时间
    val version: String,             // 版本号或其他标识
    val file: VirtualFile,           // 文件的虚拟路径
)

/**
 * Variable Snapshot will store all change flow of a variable. For example:
 * ```shire
 * ---
 * variables:
 *   "controllers": /.*.java/ { cat | grep("class\s+([a-zA-Z]*Controller)")  }
 * ---
 * ```
 *
 * The variable snapshot should store:
 *
 * - the value after cat function
 * - the value after grep function
 */
data class VariableOperation(
    val functionName: String,
    val timestamp: Long,
    val value: Any?,
)

class UserCustomVariableSnapshot(
    val variableName: String,
    val value: Any? = null,
    val className: String? = VariableResolver::class.java.name,
    val operations: List<VariableOperation> = mutableListOf(),
    private val context: ExecutionContext = ExecutionContext(),
) : UserDataHolderBase() {
    private val valueHistory = mutableListOf<Any>()
    private var currentValue: Any? = null

    fun recordValue(value: Any, functionIndex: Int = -1) {
        currentValue = value
        valueHistory.add(value)
    }

    fun getCurrentValue(): Any? = currentValue

    fun getHistory(): List<Any> = valueHistory.toList()
}

data class ExecutionContext(
    val variables: MutableMap<String, Any> = mutableMapOf(),
    val environment: MutableMap<String, String> = mutableMapOf(),
    val metadata: MutableMap<String, Any> = mutableMapOf(),
)

