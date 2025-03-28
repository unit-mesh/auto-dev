package cc.unitmesh.rust.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.rust.cargo.project.model.CargoProjectsService
import org.rust.lang.RsLanguage

class RustVersionContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.sourceFile?.language is RsLanguage
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val cargoProjectsService = project.service<CargoProjectsService>()
        val rustcInfo = cargoProjectsService.allProjects.firstOrNull()?.rustcInfo ?: return emptyList()

        val text = "Rust Version: ${rustcInfo.version?.semver}, Run in host: ${rustcInfo.version?.host}"
        return listOf(ChatContextItem(RustVersionContextProvider::class, text))
    }
}
