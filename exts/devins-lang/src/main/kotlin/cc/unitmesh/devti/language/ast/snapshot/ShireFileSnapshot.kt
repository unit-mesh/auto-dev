package cc.unitmesh.devti.language.ast.snapshot

import com.intellij.openapi.vfs.VirtualFile
import kotlinx.datetime.Clock

/**
 * the snapshot of TimeTravel Debugger
 * ```shire
 * ---
 * name: "Context Variable"
 * description: "Here is a description of the action."
 * interaction:  RunPanel
 * variables:
 *   "contextVariable": /ContextVariable\.kt/ { cat }
 *   "psiContextVariable": /PsiContextVariable\.kt/ { cat }
 * onStreamingEnd: { parseCode | saveFile("docs/shire/shire-builtin-variable.md") }
 * ---
 *
 * 根据如下的信息，编写对应的 ContextVariable 相关信息的 markdown 文档。
 * ```
 */
class ShireFileSnapshot(
    val file: VirtualFile,
    val rnd: Int, // seed for random number generator
    var variables: Map<String, UserCustomVariableSnapshot>,
    var allCode: String = "",
    /**
     * execute to current code line
     */
    var executedCode: String = "",
    val metadata: SnapshotMetadata = SnapshotMetadata(Clock.System.now(), "0.0.1", file),
) {

}