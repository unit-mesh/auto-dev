package cc.unitmesh.devti.language.lints

import cc.unitmesh.devti.language.DevInBundle
import cc.unitmesh.devti.language.psi.DevInTypes
import cc.unitmesh.devti.language.psi.DevInUsed
import cc.unitmesh.devti.language.psi.DevInVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType

class DevInsDuplicateAgentInspection : LocalInspectionTool() {
    override fun getDisplayName() = "Duplicate agent calling"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DevInsDuplicateAgentVisitor(holder)
    }

    private class DevInsDuplicateAgentVisitor(val holder: ProblemsHolder) : DevInVisitor() {
        private var agentIds: MutableSet<String> = mutableSetOf()

        override fun visitUsed(o: DevInUsed) {
            o.firstChild.let { next ->
                if (next.nextSibling.elementType == DevInTypes.AGENT_ID) {
                    if (agentIds.contains(next.text)) {
                        holder.registerProblem(
                            o,
                            DevInBundle.message("inspection.duplicate.agent")
                        )
                    } else {
                        agentIds.add(next.text)
                    }
                }
            }
        }
    }
}
