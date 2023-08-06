package cc.unitmesh.devti.context.builder

import cc.unitmesh.devti.context.SupplyInterfaceContext
import com.intellij.openapi.project.Project

interface SupplyInterfaceContextBuilder {
    fun getSupplyInterfaceContext(project: Project): SupplyInterfaceContext?
}
