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
    override fun getGroupDisplayName() = DevInBundle.message("inspection.group.name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DevInsDuplicateAgentVisitor(holder)
    }

    private class DevInsDuplicateAgentVisitor(val holder: ProblemsHolder) : DevInVisitor() {
        private var agentIds: MutableSet<DevInUsed> = mutableSetOf()

        override fun visitUsed(o: DevInUsed) {
            if (o.firstChild.nextSibling.elementType == DevInTypes.AGENT_ID) {
                agentIds.add(o)

                if (agentIds.contains(o)) {
                    agentIds.forEachIndexed { index, it ->
                        if (index > 0) {
                            holder.registerProblem(it, DevInBundle.message("inspection.duplicate.agent"))
                        }
                    }
                }
            }
        }
    }
}
